package csw.common.extensions
import csw.framework.models.CswContext
import csw.command.client.internal.models.framework.ComponentInfo

object CswContextExtensions {
  implicit class RichCswContext(val cswCtx: CswContext) extends AnyVal {
    def copy(newComponentInfo: ComponentInfo): CswContext =
      new CswContext(
        cswCtx.locationService,
        cswCtx.eventService,
        cswCtx.alarmService,
        cswCtx.loggerFactory,
        cswCtx.configClientService,
        cswCtx.currentStatePublisher,
        cswCtx.commandResponseManager,
        newComponentInfo
      )
  }
}
