package csw.database
import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.database.client.DatabaseServiceFactory
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandResponse, ControlCommand}
import org.jooq.{DSLContext, Query}

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

  override def onSubmit(controlCommand: ControlCommand): CommandResponse.SubmitResponse = {

    //#dsl-create
    val createQuery: Query = dsl.query("CREATE TABLE films (id SERIAL PRIMARY KEY, Name VARCHAR (10) NOT NULL)")

    import csw.database.client.scaladsl.JooqExtentions.RichQuery
    val createResultF: Future[Integer] = createQuery.executeAsyncScala()
    createResultF.foreach(result ⇒ println(s"Films table created with $result"))
    //#dsl-create

    //#dsl-batch
    val movie_2 = "movie_2"

    val queries = dsl.queries(
      dsl.query("INSERT INTO films(name) VALUES (?)", "movie_1"),
      dsl.query("INSERT INTO films(id, name) VALUES (?, ?)", "2", movie_2)
    )

    import csw.database.client.scaladsl.JooqExtentions.RichQueries
    val batchResultF: Future[List[Int]] = queries.executeBatchAsync()
    batchResultF.foreach(results ⇒ println(s"executed queries [$queries] with results [$results]"))
    //#dsl-batch

    //#dsl-fetch
    val selectQuery = dsl.resultQuery("SELECT name FROM films WHERE id = ?", "1")

    import csw.database.client.scaladsl.JooqExtentions.RichResultQuery
    val selectResultF: Future[List[String]] = selectQuery.fetchAsyncScala[String]
    selectResultF.foreach(names ⇒ s"Fetched names of films $names")
    //#dsl-fetch

    //#dsl-function
    val functionQuery = dsl
      .query(
        """
        |CREATE FUNCTION inc(val integer) RETURNS integer AS $$
        |BEGIN
        |RETURN val + 1;
        |END; $$
        |LANGUAGE PLPGSQL;
        """.stripMargin
      )

    val functionResultF: Future[Integer] = functionQuery.executeAsyncScala()
    functionResultF.foreach(result ⇒ println(s"Function inc created with $result"))
    //#dsl-function

    Completed(controlCommand.runId)
  }
  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit                              = ???
  override def validateCommand(controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse = ???
  override def onOneway(controlCommand: ControlCommand): Unit                                           = ???
  override def onShutdown(): Future[Unit]                                                               = ???
  override def onGoOffline(): Unit                                                                      = ???
  override def onGoOnline(): Unit                                                                       = ???
}
