package example

import java.sql.Date
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.slick.javadsl.SlickSession
import akka.stream.alpakka.slick.scaladsl.Slick
import akka.stream.scaladsl.Sink
import example.model.Tables
import example.model.Tables._
import org.slf4j.LoggerFactory
import slick.jdbc
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.{GetResult, PostgresProfile}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

object Main extends App {
  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()
  implicit val ec = system.dispatcher

  val logger = LoggerFactory.getLogger("Main")

  testSlick()
  testAlpakka()

  private def testSlick(): Unit = {
    val db = Database.forConfig("mydb")

    Await.result(Future.sequence(Seq(
      create(db),
      cleanup(db),
      insertSample(db),
      listAddresses(db),
      streamQuery(db)
    )), 2 seconds)

    db.close()
  }

  private def create(db: jdbc.PostgresProfile.backend.Database): Future[Unit] = {
    db.run(Tables.schema.create) recover { case ex => logger.warn("Table creation: " + ex.getMessage) }
  }

  private def cleanup(db: PostgresProfile.backend.Database): Future[Unit] = {
    Future.sequence(Seq(db.run(Person.delete), db.run(Address.delete))).map(_ => ())
          .andThen { case _ => logger.info("Cleaned up database")}
  }

  private def streamQuery(db: PostgresProfile.backend.Database): Future[Unit] = {
    val query = for {
      addresses <- Address
      person <- Person if addresses.id === person.addressid
    } yield (addresses, person.lastname)

    db.stream(query.result)
      .mapResult { case (address, _) => address.toString }
      .foreach(logger.info)
  }

  private def listAddresses(db: PostgresProfile.backend.Database): Future[Unit] = {
    db.run(Address.result)
      .map(
        _.map(_.toString)
         .foreach(logger.info)
      )
  }

  private def insertSample(db: PostgresProfile.backend.Database): Future[Unit] = {
    val address = AddressRow(
      id = UUID.randomUUID(),
      street1 = "123",
      street2 = Some("456"),
      zip = "75000",
      city = "Paris",
      country = "France"
    )
    db.run(for {
        _ <- Address += address
        _ <- Person += PersonRow(
          id = UUID.randomUUID(),
          title = "Mme",
          firstname = "John",
          lastname = "Doe",
          birthdate = Some(new Date(1900, 1, 1)),
          addressid = Some(address.id)
        )} yield ())
  }

  private def testAlpakka(): Unit = {
    implicit val db = SlickSession.forConfig("mydb-alpakka")
    import db.profile.api._
    system.registerOnTermination(() => db.close())

    case class LightPersonRow(id: String, name: String)

    val viaEntity = Slick.source(Person.result)
      .log("person")
      .runWith(Sink.ignore)

    val viaSql = Slick.source(sql"select id, firstname from person".as[LightPersonRow](GetResult(r => LightPersonRow(r.nextString, r.nextString))))
      .log("light-person")
      .runWith(Sink.ignore)

    viaEntity.zip(viaSql).onComplete { _ =>
      system.terminate()
    }
  }

}
