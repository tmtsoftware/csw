package romaine.reactive
import akka.Done
import akka.stream.KillSwitch

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

private class RedisSubscriptionImpl[K](
    keys: List[K],
    connectedF: Future[Unit],
    killSwitch: KillSwitch,
    terminationSignal: Future[Done],
    redisReactiveApi: Future[RedisReactiveApi[K, _]]
)(implicit executionContext: ExecutionContext)
    extends RedisSubscription {

  terminationSignal.onComplete(_ => unsubscribe()) //unsubscribe on stream termination

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Done] = async {
    await(connectedF)
    await(redisReactiveApi.flatMap(_.unsubscribe(keys))) // unsubscribe is no-op
    await(redisReactiveApi.flatMap(_.quit()))
    killSwitch.shutdown()
    await(terminationSignal) // await on terminationSignal when unsubscribe is called by user
  }

  /**
   * To check if the underlying subscription is ready to emit elements
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Done] = connectedF.map(_ ⇒ Done)
}
