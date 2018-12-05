package demo.jooq.java;

import org.jooq.codegen.GenerationTool;
import org.jooq.meta.jaxb.*;

public class CodeGen {
    public static void main(String[] args) {

        Configuration configuration = new Configuration()
                .withJdbc(new Jdbc()
                        .withDriver("org.postgresql.Driver")
                        .withUrl("jdbc:postgresql:postgres")
                        .withUser("userName")
                        .withPassword("password"))
                .withGenerator(new Generator()
                        .withDatabase(new Database()
                                .withIncludeRoutines(false)
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                .withIncludes(".*")
                                .withExcludes("")
                                .withInputSchema("public"))
                        .withTarget(new Target()
                                .withPackageName("demo.jooq.java.generate")
                                .withDirectory("csw-database-client/src/main/scala")));

        try {
            GenerationTool.generate(configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
