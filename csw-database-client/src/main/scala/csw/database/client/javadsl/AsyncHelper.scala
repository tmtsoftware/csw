package csw.database.client.javadsl
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

object AsyncHelper {
  def managedBlock[T](supplier: Supplier[T]): CompletableFuture[T] =
    CompletableFuture.supplyAsync { () ⇒
      concurrent.blocking {
        supplier.get()
      }
    }
}
