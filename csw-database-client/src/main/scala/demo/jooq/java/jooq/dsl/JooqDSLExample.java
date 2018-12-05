//package demo.jooq.java.jooq.dsl;
//
//import org.jooq.*;
//import org.jooq.impl.DSL;
//
//import static demo.jooq.java.generate.Tables.AUTHOR;
//import static demo.jooq.java.generate.Tables.BOOK;
//
//public class JooqDSLExample {
//    public static void main(String[] args) {
//        DSLContext dsl = DSL.using("jdbc:postgresql://localhost:5432/bharats?user=bharats&password=feroh");
//
//        SelectConditionStep<Record3<String, String, String>> query =
//                dsl.select(BOOK.TITLE, AUTHOR.FIRST_NAME, AUTHOR.LAST_NAME)
//                        .from(BOOK)
//                        .join(AUTHOR)
//                        .on(BOOK.AUTHOR_ID.eq(AUTHOR.ID))
//                        .where(BOOK.PUBLISHED_IN.eq(1948));
//
//        String sql = query.getSQL();
//
//        System.out.println("-----------------------------");
//        System.out.println(sql);
//        System.out.println("-----------------------------");
//        Result<Record3<String, String, String>> result = query.fetch();
//
//        for (Record r : result) {
//            String bookTitle = r.getValue(BOOK.TITLE);
//            String firstName = r.getValue(AUTHOR.FIRST_NAME);
//            String lastName = r.getValue(AUTHOR.LAST_NAME);
//
//            System.out.println("Book title: " + bookTitle);
//            System.out.println("first name: " + firstName);
//            System.out.println("last name: " + lastName);
//        }
//
//    }
//}
