package csw.database.javadsl
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

/**
 * A java helper to schedule and execute blocking operations on a dedicated thread pool. This mechanism will prevent any
 * blocking operation to be scheduled on a thread pool designated for async operations.
 */
object AsyncHelper {

  /**
   * The method wraps ForkJoinPool#managedBlock() and executes the provided supplier in a dedicated thread pool for blocking
   * operations
   *
   * @param supplier to be scheduled on a thread pool for blocking operations
   * @tparam T the type of value that the supplier provides
   * @return a CompletableFuture that waits to complete till the supplier is ready to be executedx in the thread pool and produces
   *         the result of type T
   */
  def managedBlock[T](supplier: Supplier[T]): CompletableFuture[T] =
    CompletableFuture.supplyAsync { () ⇒
      concurrent.blocking {
        supplier.get()
      }
    }
}
