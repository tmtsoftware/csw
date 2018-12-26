package csw.database
import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.database.client.DatabaseServiceFactory
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.{CommandResponse, ControlCommand}
import org.jooq.DSLContext

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContextExecutor, Future}

//DEOPSCSW-615: DB service accessible to CSW component developers
class AssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext

  private var dsl: DSLContext = _
  override def initialize(): Future[Unit] = async {
    //#dbFactory-access
    val dbFactory = new DatabaseServiceFactory(ctx.system)

    dsl = await(dbFactory.makeDsl(locationService, "postgres"))
    //#dbFactory-access

    //#dbFactory-write-access
    dsl = await(dbFactory.makeDsl(locationService, "postgres", "dbWriteUsername", "dbWritePassword"))
    //#dbFactory-write-access

    //#dbFactory-test-access
    dsl = await(dbFactory.makeDsl())
    //#dbFactory-test-access
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???
  override def validateCommand(
      controlCommand: ControlCommand
  ): CommandResponse.ValidateCommandResponse = ???
  override def onSubmit(
      controlCommand: ControlCommand
  ): CommandResponse.SubmitResponse                           = ???
  override def onOneway(controlCommand: ControlCommand): Unit = ???
  override def onShutdown(): Future[Unit]                     = ???
  override def onGoOffline(): Unit                            = ???
  override def onGoOnline(): Unit                             = ???
}
