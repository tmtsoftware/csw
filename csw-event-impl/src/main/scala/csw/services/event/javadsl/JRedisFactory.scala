package csw.services.event.javadsl

import java.util.concurrent.CompletableFuture

import akka.stream.Materializer
import csw.services.event.scaladsl.RedisSentinelFactory

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext

class JRedisFactory(redisFactory: RedisSentinelFactory)(implicit ec: ExecutionContext, mat: Materializer) {

  def publisher(host: String, port: Int, masterId: String): IEventPublisher =
    redisFactory.publisher(host, port, masterId).asJava

  def publisher(masterId: String): CompletableFuture[IEventPublisher] =
    redisFactory.publisher(masterId).map(_.asJava).toJava.toCompletableFuture

  def subscriber(host: String, port: Int, masterId: String): IEventSubscriber =
    redisFactory.subscriber(host, port, masterId).asJava

  def subscriber(masterId: String): CompletableFuture[IEventSubscriber] =
    redisFactory.subscriber(masterId).map(_.asJava).toJava.toCompletableFuture
}
