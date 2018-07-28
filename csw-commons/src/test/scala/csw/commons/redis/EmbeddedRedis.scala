package csw.commons.redis
import java.io.IOException
import java.net.ServerSocket

import redis.embedded.{RedisSentinel, RedisServer}

import scala.annotation.tailrec
import scala.util.{Failure, Success, Try}

trait EmbeddedRedis {

  @tailrec
  final def getFreePort: Int = {
    Try(new ServerSocket(0)) match {
      case Success(socket) =>
        val port = socket.getLocalPort
        socket.close()
        port
      case Failure(_: IOException) => getFreePort
      case Failure(e)              => throw e
    }
  }

  type ServerPort   = Int
  type SentinelPort = Int

  def withRedis[T](port: Int = getFreePort)(f: ServerPort => T): T = {
    val redisServer = new RedisServer(port)
    redisServer.start()
    val result = f(port)
    redisServer.stop()
    result
  }

  def withSentinel[T](
      sentinelPort: Int = getFreePort,
      serverPort: Int = getFreePort,
      masterId: String
  )(f: (SentinelPort, ServerPort) ⇒ T): (T, RedisSentinel, RedisServer) = {
    val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId)
    val result             = f(sentinelPort, serverPort)
    (result, sentinel, server)
  }

  def startRedis(port: Int = getFreePort): RedisServer = {
    val redisServer = new RedisServer(port)
    redisServer.start()
    addJvmShutdownHook(stopRedis(redisServer))
    redisServer
  }

  def startSentinel(
      sentinelPort: Int = getFreePort,
      serverPort: Int = getFreePort,
      masterId: String
  ): (RedisSentinel, RedisServer) = {
    val redisServer = new RedisServer(serverPort)

    val redisSentinel: RedisSentinel = RedisSentinel
      .builder()
      .port(sentinelPort)
      .masterName(masterId)
      .masterPort(serverPort)
      .quorumSize(1)
      .build()

    redisServer.start()
    redisSentinel.start()

    addJvmShutdownHook(stopSentinel(redisSentinel, redisServer))
    (redisSentinel, redisServer)
  }

  def stopRedis(redisServer: RedisServer): Unit = redisServer.stop()

  def stopSentinel(redisSentinel: RedisSentinel, redisServer: RedisServer): Unit = {
    redisServer.stop()
    redisSentinel.stop()
  }

  private def addJvmShutdownHook[T](hook: ⇒ T): Unit =
    Runtime.getRuntime.addShutdownHook(new Thread { override def run(): Unit = hook })
}
