package csw.alarm.client.internal
import csw.logging.core.scaladsl.LoggerFactory

/**
 * All the logs generated from location service will have a fixed componentName.
 * The componentName helps in production to filter out logs from a particular component and this case, it helps to filter out logs
 * generated from alarm service.
 */
private[alarm] object AlarmServiceLogger extends LoggerFactory("alarm-service-lib")
