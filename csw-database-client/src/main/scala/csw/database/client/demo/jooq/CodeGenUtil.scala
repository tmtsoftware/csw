package csw.database.client.demo.jooq

import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb._

class CodeGenUtil {

  val configuration: Configuration =
    new Configuration()
      .withJdbc(
        new Jdbc()
          .withDriver("org.postgresql.Driver")
          .withUrl("jdbc:postgresql:postgres")
          .withUser("<username>")
      )
      .withGenerator(
        new Generator()
          .withDatabase(
            new Database()
              .withIncludeRoutines(false)
              .withName("org.jooq.meta.postgres.PostgresDatabase")
              .withIncludes(".*")
              .withExcludes("")
              .withInputSchema("public")
          )
          .withTarget(
            new Target()
              .withPackageName("csw.database.client.demo.jooq.java.generate")
              .withDirectory("csw-database-client/src/main/scala")
          )
      )
}

object Main {
  def main(args: Array[String]): Unit = {
    GenerationTool.generate(new CodeGenUtil().configuration)
  }
}
