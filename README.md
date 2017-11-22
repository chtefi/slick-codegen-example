# Dependencies

- Slick 3.x
- sbt 1.0.3
- Slick code generator
- Alpakka Slick (Akka Streams)

# How to

- Create the db using `\src\main\resources\init.sql`
- `sbt genTables` (or directly `run`, it's dependent)
- `sbt run`
