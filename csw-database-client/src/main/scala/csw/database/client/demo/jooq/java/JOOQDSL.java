package csw.database.client.demo.jooq.java;

import csw.database.client.demo.jooq.dsl_handle.DatabaseService;
import org.jooq.DSLContext;
import org.jooq.Record3;
import org.jooq.Result;
import org.jooq.ResultQuery;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static csw.database.client.demo.jooq.java.generate.Tables.AUTHOR;
import static csw.database.client.demo.jooq.java.generate.Tables.BOOK;

public class JOOQDSL {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        DSLContext dsl = DatabaseService.defaultDSL();

        ResultQuery<Record3<String, String, String>> query =
                dsl.select(BOOK.TITLE, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
                        .from(BOOK)
                        .join(AUTHOR)
                        .on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
                        .where(BOOK.PUBLISHED_IN.eq(1948));

        String sql = query.getSQL();

        System.out.println("-----------------------------");
        System.out.println(sql);
        System.out.println("-----------------------------");
        Result<Record3<String, String, String>> result = query.fetchAsync().toCompletableFuture().get();

        List<BookData> bookData = result.map(record -> record.into(BookData.class));
        bookData.forEach(System.out::println);
    }
}

class BookData {
    private String title;
    private String authorFirst;
    private String authorLast;

    public BookData(String title, String authorFirst, String authorLast) {
        this.title = title;
        this.authorFirst = authorFirst;
        this.authorLast = authorLast;
    }

    @Override
    public String toString() {
        return "BookData{" +
                "title='" + title + '\'' +
                ", authorFirst='" + authorFirst + '\'' +
                ", authorLast='" + authorLast + '\'' +
                '}';
    }
}