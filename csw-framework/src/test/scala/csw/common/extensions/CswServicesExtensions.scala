package csw.common.extensions
import csw.framework.models.CswServices
import csw.messages.framework.ComponentInfo

object CswServicesExtensions {
  implicit class RichCswServices(val cswServices: CswServices) extends AnyVal {
    def copy(newComponentInfo: ComponentInfo): CswServices =
      new CswServices(
        cswServices.locationService,
        cswServices.eventService,
        cswServices.alarmService,
        cswServices.loggerFactory,
        cswServices.configClientService,
        cswServices.currentStatePublisher,
        cswServices.commandResponseManager,
        newComponentInfo
      )
  }
}
