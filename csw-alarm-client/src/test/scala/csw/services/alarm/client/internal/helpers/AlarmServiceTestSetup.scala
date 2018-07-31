package csw.services.alarm.client.internal.helpers
import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.client.internal.AlarmServiceImpl
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

abstract class AlarmServiceTestSetup
    extends FunSuite
    with Matchers
    with EmbeddedRedis
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  private val alarmServer        = "AlarmServer"
  private val (sentinel, server) = startSentinel(26379, 6379, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel("localhost", 26379, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create(redisURI)

  implicit val system: ActorSystem  = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val alarmServiceFactory            = new AlarmServiceTestFactory(redisURI, redisClient)
  val alarmService: AlarmServiceImpl = alarmServiceFactory.make()

  override protected def afterAll(): Unit = stopSentinel(sentinel, server)
}
