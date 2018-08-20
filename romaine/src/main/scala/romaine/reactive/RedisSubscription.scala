package romaine.reactive

import akka.Done

import scala.concurrent.Future

trait RedisSubscription {
  def unsubscribe(): Future[Done]
  def ready(): Future[Done]
}
