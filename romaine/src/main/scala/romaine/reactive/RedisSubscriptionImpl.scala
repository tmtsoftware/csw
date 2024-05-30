/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.reactive
import org.apache.pekko.Done
import org.apache.pekko.stream.KillSwitch
import romaine.reactive.subscribe.RedisReactiveApi

import cps.compat.FutureAsync.*
import scala.concurrent.{ExecutionContext, Future}

private class RedisSubscriptionImpl[K](
    keys: List[K],
    connectedF: Future[Done],
    killSwitch: KillSwitch,
    terminationSignal: Future[Done],
    redisReactiveApi: Future[RedisReactiveApi[K, ?]]
)(implicit executionContext: ExecutionContext)
    extends RedisSubscription {

  terminationSignal.onComplete(_ => unsubscribe()) // unsubscribe on stream termination

  /**
   * To unsubscribe a given subscription. This will also clean up subscription specific underlying resources
   *
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Done] =
    async {
      await(connectedF)
      await(redisReactiveApi.flatMap(_.unsubscribe(keys))) // unsubscribe is no-op
//      await(redisReactiveApi.flatMap(_.close()))
      killSwitch.shutdown()
      await(terminationSignal) // await on terminationSignal when unsubscribe is called by user
    }

  /**
   * To check if the underlying subscription is ready to emit elements
   *
   * @return a future which completes when the underlying subscription is ready to emit elements
   */
  def ready(): Future[Done] = connectedF
}
