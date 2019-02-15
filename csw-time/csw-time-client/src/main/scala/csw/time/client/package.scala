package csw.time

import java.time.ZonedDateTime

import csw.time.client.TMTTimeHelper
import csw.time.client.api.TimeServiceScheduler
import csw.time.api.models.TMTTime

/**
 * This module provides implementation for Time Service Scheduler which provides APIs for
 * scheduling periodic and non-periodic tasks in the future, which are optimised for scheduling at up to 1KHz frequency.
 *
 * For component developers, the scheduler API is injected in the ComponentHandlers.
 * Hence for them, there is no need to create a [[TimeServiceScheduler]] instance.
 *
 * If you are not using `csw-framework`, you can create an instance like shown below.
 *
 * {{{
 *     val scheduler: TimeServiceScheduler = TimeServiceSchedulerFactory.make()(actorSystem)
 * }}}
 *
 * In addition to the Scheduler API, this module also provides a [[TMTTimeHelper]] API which provides additional time zone related functionality
 * on top of [[TMTTime]]. It allows users to get a Java [[ZonedDateTime]] representation of a TMTTime.
 *
 * Following is a small sample showing how to access utilities from TMTTimeHelper.
 *
 * {{{
 *   TMTTimeHelper.atLocal(utcTime)
 *   TMTTimeHelper.atHawaii(utcTime)
 * }}}
 *
 */
package object client {}
