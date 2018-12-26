package csw.database;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.database.client.DatabaseServiceFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.params.commands.CommandResponse;
import csw.params.commands.ControlCommand;
import org.jooq.DSLContext;

import java.util.concurrent.CompletableFuture;

//DEOPSCSW-615: DB service accessible to CSW component developers
public class JAssemblyComponentHandlers extends JComponentHandlers {

    ActorContext<TopLevelActorMessage> ctx;
    JCswContext cswCtx;
    DatabaseServiceFactory dbFactory;
    DSLContext dsl;

    public JAssemblyComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.ctx = ctx;
        this.cswCtx = cswCtx;

    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        //#dbFactory-access
        dbFactory = new DatabaseServiceFactory(ctx.getSystem());

        dbFactory
                .jMakeDsl(cswCtx.locationService(), "postgres")
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-access

        //#dbFactory-write-access
        dbFactory
                .jMakeDsl(cswCtx.locationService(), "postgres", "dbWriteUsername", "dbWritePassword")
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-write-access

        //#dbFactory-test-access
        dbFactory
                .jMakeDsl()
                .thenAccept(dsl -> this.dsl = dsl);
        //#dbFactory-test-access
        return null;
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        return null;
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return null;
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {
        return null;
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

    @Override
    public void onGoOffline() {

    }

    @Override
    public void onGoOnline() {

    }
}