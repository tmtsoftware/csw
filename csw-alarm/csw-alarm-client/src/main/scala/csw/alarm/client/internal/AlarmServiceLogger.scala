package csw.alarm.client.internal

import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.CSW

/**
 * All the logs generated from location service will have a fixed prefix.
 * The prefix helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from alarm service.
 */
private[alarm] object AlarmServiceLogger extends LoggerFactory(Prefix(CSW, "alarm_service_lib"))
