package csw.services.alarm.api.scaladsl
import csw.services.alarm.api.internal.{HealthService, MetadataService, SeverityService, StatusService}

/**
 * An AlarmAdminService interface to update and query alarms. All operations are non-blocking.
 */
trait AlarmAdminService extends SeverityService with MetadataService with HealthService with StatusService
