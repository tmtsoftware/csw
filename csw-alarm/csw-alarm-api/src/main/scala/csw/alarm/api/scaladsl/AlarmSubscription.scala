/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.alarm.api.scaladsl

import org.apache.pekko.Done

import scala.concurrent.Future

/**
 * Represents a subscription of health or severity for a alarm/component/subsystem/system
 */
trait AlarmSubscription {

  /**
   * Un-subscribes alarm subscription of health or severity for a given alarm/component/subsystem/system
   *
   * @note unsubscribe is an idempotent call i.e. calling unsubscribe twice will have no effect once unsubscribed
   * @return a future which completes when the unsubscribe is completed
   */
  def unsubscribe(): Future[Done]

  /**
   * Checks if the subscription is ready to be consumed
   *
   * @return a future which completes when the subscription is ready
   */
  def ready(): Future[Done]
}
