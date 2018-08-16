package romaine.reactive
import akka.Done
import akka.stream.KillSwitch

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

private class RedisSubscriptionImpl[K](
    keys: List[K],
    subscriptionF: Future[Unit],
    killSwitch: KillSwitch,
    terminationSignal: Future[Done],
    redisReactiveScalaApi: RedisReactiveScalaApi[K, _]
)(implicit executionContext: ExecutionContext)
    extends RedisSubscription {

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Unit] = async {
    await(redisReactiveScalaApi.unsubscribe(keys)) // unsubscribe is no-op
    await(redisReactiveScalaApi.quit)
    killSwitch.shutdown()
    await(terminationSignal) // await on terminationSignal when unsubscribe is called by user
  }

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Unit] = subscriptionF.map(_ ⇒ ()).recoverWith {
    case _ if terminationSignal.isCompleted ⇒ terminationSignal.map(_ => ())
  }
}
