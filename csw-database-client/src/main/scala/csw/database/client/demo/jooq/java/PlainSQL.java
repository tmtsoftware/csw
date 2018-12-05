package csw.database.client.demo.jooq.java;

import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class PlainSQL {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DSLContext dsl = DSL.using("jdbc:postgresql://localhost:5432/postgres?user=salonivithalani");

        // ***************************************************************** //

        Integer result = dsl.query("CREATE DATABASE box_office")
                .executeAsync()
                .toCompletableFuture()
                .get();

        System.out.println("CREATE DATABASE box_office - " + result);

        // ***************************************************************** //

        Integer result1 = dsl.query("DROP DATABASE box_office")
                .executeAsync()
                .toCompletableFuture()
                .get();

        System.out.println("DROP DATABASE box_office - " + result1);

        // ***************************************************************** //

        Integer result2 = dsl.query("CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL)")
                .executeAsync()
                .toCompletableFuture()
                .get();

        System.out.println("CREATE TABLE IF NOT EXISTS films (id SERIAL PRIMARY KEY, name VARCHAR (10) NOT NULL) " + result2);

        // ***************************************************************** //

        Query q1 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_1");
        Query q2 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_4");
        Query q3 = dsl.query("INSERT INTO films(name) VALUES (?)", "movie_2");

        CompletableFuture<int[]> future = CompletableFuture.supplyAsync(() -> dsl.queries(q1, q2, q3).executeBatch());
        int[] result3 = future.get();

        System.out.println("Multiple inserts - " + result3[0] + " - " + result3[1] + " - " + result3[2]);

        // ***************************************************************** //

        Result<Record> result4 = dsl.resultQuery("SELECT * FROM films where name = ?", "movie_1")
                .fetchAsync()
                .toCompletableFuture()
                .get();

        List<Film> films = result4.map(record -> record.into(Film.class));

        System.out.println("SELECT * FROM films where name = ? - ");
        films.forEach(System.out::println);

        // ***************************************************************** //

        dsl.close();
    }
}

class Film {
    private Integer id;
    String name;

    Film(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public String toString() {
        return "id: " +id+ " name: " + name;
    }
}

