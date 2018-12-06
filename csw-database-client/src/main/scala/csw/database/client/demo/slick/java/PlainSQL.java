package csw.database.client.demo.slick.java;

import akka.actor.ActorSystem;
import csw.database.api.javadsl.IDatabaseService;
import csw.database.client.demo.jooq.scala.Film;
import csw.database.client.scaladsl.DatabaseServiceFactory;
import scala.concurrent.ExecutionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class PlainSQL {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ActorSystem system = ActorSystem.apply("test");
        ExecutionContext ec = system.dispatcher();
        DatabaseServiceFactory factory = new DatabaseServiceFactory();
        IDatabaseService databaseService = factory.jMake("localhost", 5432, "postgres", "salonivithalani", ec);

        String createQuery = "CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL)";
        databaseService.update(createQuery).get();

        List<String> inserts = new ArrayList<>();
        inserts.add("INSERT INTO films(name) VALUES ('movie_1')");
        inserts.add("INSERT INTO films(name) VALUES ('movie_4')");
        inserts.add("INSERT INTO films(name) VALUES ('movie_2')");
        databaseService.updateAll(inserts).get();

        String selectQuery = "SELECT * FROM films where name = ?";
        databaseService.query(selectQuery, (pp) -> pp.setString("movie_1"), (dbRow -> new Film(dbRow.nextInt(), dbRow.nextString())));
    }
}
