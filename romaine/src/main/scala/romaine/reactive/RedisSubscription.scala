/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package romaine.reactive

import org.apache.pekko.Done

import scala.concurrent.Future

trait RedisSubscription {

  /**
   * Used to unsubscribe and close the stream
   *
   * @return
   */
  def unsubscribe(): Future[Done]

  /**
   * Used to determine whether the stream is ready to be consumed or not
   *
   * @return a future of Done. A completed future indicates that stream is ready of consumption
   */
  def ready(): Future[Done]
}
