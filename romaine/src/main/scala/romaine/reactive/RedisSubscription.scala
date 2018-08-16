package romaine.reactive

import scala.concurrent.Future

trait RedisSubscription {
  def unsubscribe(): Future[Unit]
  def ready(): Future[Unit]
}
