/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.time.scheduler

import akka.actor.typed.Scheduler
import csw.time.scheduler.api.TimeServiceScheduler
import csw.time.scheduler.internal.{TimeServiceSchedulerImpl, TimeServiceSchedulerNative}

import scala.concurrent.ExecutionContext

/**
 * Factory to create [[csw.time.scheduler.api.TimeServiceScheduler]]
 */
class NativeTimeServiceSchedulerFactory {

  /**
   * API to create [[csw.time.scheduler.api.TimeServiceScheduler]]
   *
   * @param ec an executionContext
   * @return an instance of [[csw.time.scheduler.api.TimeServiceScheduler]] which can be used to schedule one-time/periodic tasks
   */
  def make()(implicit ec: ExecutionContext): TimeServiceScheduler = new TimeServiceSchedulerNative()

}
