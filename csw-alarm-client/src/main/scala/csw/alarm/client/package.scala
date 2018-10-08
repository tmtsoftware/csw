package csw.alarm

/**
 * == Alarm Service ==
 *
 * This module implements an Alarm Service responsible for updating and managing alarms for a component. Alarm service comes
 * in two flavours. One provides admin level management like reset alarm, subscribe to alarm, etc. And other provides component
 * level api e.g. set the severity of an alarm.
 *
 * An alarm is uniquely identified within the component with [[csw.alarm.api.models.Key.AlarmKey]]. The alarm key is composed of
 * a [[csw.params.core.models.Subsystem]], component name, alarm name.
 *
 * === Example: Alarm Service ===
 *
 * {{{
 *
 *      val alarmServiceFactory                    = new AlarmServiceFactory()
 *      val alarmService: AlarmService             = alarmServiceFactory.makeClientApi(locationService)
 *      val alarmAdminService: AlarmAdminService   = alarmServiceFactory.makeAdminApi(locationService)
 *
 * }}}
 *
 * Using above code, you can create instance of [[csw.alarm.api.scaladsl.AlarmService]] or [[csw.alarm.api.scaladsl.AlarmAdminService]].
 *
 * === Example: Alarm Service ===
 *
 * Alarm Service provides asynchronous API to set the severity of an alarm
 *
 * {{{
 *
 *   val alarmKey = AlarmKey(NFIRAOS, "trombone", "tromboneAxisLowLimitAlarm")
 *
 *   val foo: Future[Done] = async {
 *      await(clientAPI.setSeverity(alarmKey, Okay))
 *   }
 *
 * }}}
 *
 *
 * Complete guide of usage of different API's provided by AlarmService is available at:
 * https://tmtsoftware.github.io/csw/services/alarm.html
 *
 */
package object client {}
