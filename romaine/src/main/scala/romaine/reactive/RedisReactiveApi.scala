package romaine.reactive

import akka.{Done, NotUsed}
import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.concurrent.Future

trait RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Done]
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed]
  def unsubscribe(keys: List[K]): Future[Done]
  def quit(): Future[String]
}
