package csw.services.alarm.client.internal.helpers
import akka.actor.ActorSystem
import csw.commons.redis.EmbeddedRedis
import csw.services.alarm.api.javadsl.IAlarmService
import csw.services.alarm.api.scaladsl.AlarmAdminService
import csw.services.alarm.client.internal.JAlarmServiceImpl
import io.lettuce.core.{RedisClient, RedisURI}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, FunSuite, Matchers}

import scala.concurrent.ExecutionContext

class AlarmServiceTestSetup(sentinelPort: Int, serverPort: Int)
    extends FunSuite
    with Matchers
    with EmbeddedRedis
    with BeforeAndAfterAll
    with BeforeAndAfterEach {
  private val alarmServer        = "AlarmServer"
  private val (sentinel, server) = startSentinel(sentinelPort, serverPort, masterId = alarmServer)

  private val redisURI                 = RedisURI.Builder.sentinel("localhost", sentinelPort, alarmServer).build()
  private val redisClient: RedisClient = RedisClient.create(redisURI)

  implicit val system: ActorSystem  = ActorSystem()
  implicit val ec: ExecutionContext = system.dispatcher

  val alarmServiceFactory             = new AlarmServiceTestFactory(redisURI, redisClient)
  val alarmService: AlarmAdminService = alarmServiceFactory.make()
  val jalarmService: IAlarmService    = new JAlarmServiceImpl(alarmService)

  override protected def afterAll(): Unit = stopSentinel(sentinel, server)
}
