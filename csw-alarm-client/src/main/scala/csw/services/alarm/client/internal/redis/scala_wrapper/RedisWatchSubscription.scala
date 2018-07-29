package csw.services.alarm.client.internal.redis.scala_wrapper

import akka.Done
import akka.stream.KillSwitch
import scala.async.Async.{async, await}

import scala.concurrent.{ExecutionContext, Future}

class RedisWatchSubscription[TPatternKey] private[Alarm] (
    keys: List[TPatternKey],
    pSubscribeF: Future[Unit],
    killSwitch: KillSwitch,
    terminationSignal: Future[Done],
    redisReactiveScalaApi: RedisReactiveScalaApi[TPatternKey, _]
)(implicit executionContext: ExecutionContext) {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Unit] = async {
    await(redisReactiveScalaApi.punsubscribe(keys))
    await(redisReactiveScalaApi.quit)
    killSwitch.shutdown()
    await(terminationSignal) // await on terminationSignal when unsubscribe is called by user
  }

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Unit] = async {
    case _ if terminationSignal.isCompleted ⇒ terminationSignal.map(_ ⇒ ())
  }
}
