package csw.config.server.svn

import csw.config.api.scaladsl.ConfigService
import csw.config.server.ActorRuntime
import csw.config.server.files.AnnexFileService

private[config] class SvnConfigServiceFactory(actorRuntime: ActorRuntime, annexFileService: AnnexFileService) {

  import actorRuntime._

  def make(): ConfigService = make(settings.`svn-user-name`)

  def make(userName: String): ConfigService = {
    val svnRepo = new SvnRepo(userName, settings, actorRuntime.blockingIoDispatcher)
    new SvnConfigService(settings, actorRuntime, svnRepo, annexFileService)
  }
}
