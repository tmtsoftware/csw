/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package csw.framework.javadsl.components;

import org.apache.pekko.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.api.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.CommandResponse.Completed;
import csw.params.commands.CommandResponse.SubmitResponse;
import csw.params.commands.ControlCommand;
import csw.params.core.models.Id;
import csw.time.core.models.UTCTime;

public class JInvalidComponentHandlers extends JComponentHandlers {

    // Demonstrating logger accessibility in Java Component handlers
    private final ILogger log;

    JInvalidComponentHandlers(JCswContext cswCtx, ActorContext<TopLevelActorMessage> ctx) {
        super(ctx, cswCtx);
        this.log = cswCtx.loggerFactory().getLogger(getClass());
    }

    @Override
    public void initialize() {
        log.debug("Initializing Sample component");
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {
    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(Id runId, ControlCommand controlCommand) {
        return new CommandResponse.Accepted(runId);
    }

    @Override
    public SubmitResponse onSubmit(Id runId, ControlCommand controlCommand) {
        return new Completed(runId);
    }

    @Override
    public void onOneway(Id runId, ControlCommand controlCommand) {
    }

    @Override
    public void onShutdown() {
    }

    @Override
    public void onGoOffline() {
    }

    @Override
    public void onGoOnline() {
    }

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
    }

    @Override
    public void onOperationsMode() {
    }
}
