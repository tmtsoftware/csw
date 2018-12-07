package csw.database.client.javadsl
import java.util.concurrent.CompletableFuture

import org.jooq.Queries

object JooqHelper {
  def executeBatch(queries: Queries): CompletableFuture[Array[Int]] = AsyncHelper.managedBlock(() ⇒ queries.executeBatch())
}
