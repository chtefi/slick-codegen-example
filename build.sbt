name := "slick-codegen-example"
scalaVersion := "2.12.4"

resolvers += Resolver.bintrayRepo("akka", "maven")

val slickVersion = "3.2.1"
libraryDependencies ++= Seq(
  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-codegen" % slickVersion,
  "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "0.14",
  "org.slf4j" % "log4j-over-slf4j" % "1.7.25",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.6",
  "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
  "org.postgresql" % "postgresql" % "42.1.4"
)

lazy val slick = TaskKey[Seq[File]]("gen-tables")
lazy val slickCodeGenTask = Def.task {
  val outputDir = sourceManaged.value.getPath
  val url = "jdbc:postgresql:postgres"
  val jdbcDriver = "org.postgresql.Driver"
  val slickDriver = "slick.jdbc.PostgresProfile"
  val pkg = "example.model"

  (runner in Compile).value.run("slick.codegen.SourceCodeGenerator",
    (dependencyClasspath in Compile).value.files,
        Array(slickDriver, jdbcDriver, url, outputDir, pkg, "postgres", "admin"),
    streams.value.log)
  val fname = outputDir + Path.sep + pkg.replace('.', Path.sep) + Path.sep + "Tables.scala"
  Seq(file(fname))
}

slick := { slickCodeGenTask.value }
sourceGenerators in Compile += slickCodeGenTask.taskValue
