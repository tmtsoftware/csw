package csw.logging.api.models

import java.util.UUID

/**
 * The trait for logging ids. It can be either a RequestId to a specific request or
 * NoId when there is no associated request.
 */
sealed trait AnyId

/**
 * The logging id of a specific request
 *
 * @param trackingId the global unique id of the request
 *                   optional: A new unique id will be created if this is not specified
 * @param spanId a sub-id used when a a service is called multiple times for the same global request
 *               optional: defaults to 0
 * @param level a field for controlling per request log levels
 *              optional: defaults to no per request control
 */
case class RequestId(
    trackingId: String = UUID.randomUUID().toString,
    spanId: String = "",
    level: Option[LoggingLevels.Level] = None
) extends AnyId

/**
 * The id value used in logging calls when there is no associated request
 */
case object noId extends AnyId
