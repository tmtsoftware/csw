package romaine.reactive

import akka.Done

import scala.concurrent.Future

trait RedisSubscription {

  /**
   * Used to unsubscribe and close the stream
   * @return
   */
  def unsubscribe(): Future[Done]

  /**
   * Used to determine whether the stream is ready to be consumed or not
   * @return a future of Done. A completed future indicates that stream is ready of consumption
   */
  def ready(): Future[Done]
}
