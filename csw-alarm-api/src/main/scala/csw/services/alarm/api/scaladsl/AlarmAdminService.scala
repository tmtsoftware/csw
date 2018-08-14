package csw.services.alarm.api.scaladsl
import csw.services.alarm.api.internal.{HealthService, MetadataService, SeverityService, StatusService}

trait AlarmAdminService extends SeverityService with MetadataService with HealthService with StatusService
