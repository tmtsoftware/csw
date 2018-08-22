package romaine.reactive

import akka.NotUsed
import akka.stream.scaladsl.Source
import reactor.core.publisher.FluxSink.OverflowStrategy

import scala.concurrent.Future

trait RedisReactiveApi[K, V] {
  def subscribe(keys: List[K]): Future[Unit]
  def observe(overflowStrategy: OverflowStrategy): Source[RedisResult[K, V], NotUsed]
  def unsubscribe(keys: List[K]): Future[Unit]
  def quit(): Future[String]
}
