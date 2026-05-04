package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.command.SyncCooperatorsCommand;
import fr.sqq.choixcreneaux.application.command.SyncPullCommand;
import fr.sqq.choixcreneaux.application.command.SyncPushCommand;
import fr.sqq.choixcreneaux.application.command.SyncPushOneCommand;
import fr.sqq.mediator.Mediator;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.util.UUID;

@Path("/api/admin/sync")
@RolesAllowed({"Foodcoop Admin"})
@Produces(MediaType.APPLICATION_JSON)
public class AdminSyncResource {

    @Inject
    Mediator mediator;

    @POST
    @Path("/pull")
    public SyncPullCommand.Result pull() {
        return mediator.send(new SyncPullCommand());
    }

    @GET
    @Path("/push-stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<SyncPushCommand.LogLine> pushStream() {
        return Multi.createFrom().emitter(em ->
                Infrastructure.getDefaultWorkerPool().execute(() -> {
                    try {
                        mediator.send(new SyncPushCommand(em::emit));
                        em.complete();
                    } catch (Throwable t) {
                        em.fail(t);
                    }
                }));
    }

    @POST
    @Path("/cooperators")
    public SyncCooperatorsCommand.Result syncCooperators() {
        return mediator.send(new SyncCooperatorsCommand());
    }

    @POST
    @Path("/push-one")
    @Consumes(MediaType.APPLICATION_JSON)
    public SyncPushOneCommand.Result pushOne(PushOneRequest request) {
        return mediator.send(new SyncPushOneCommand(request.cooperatorId()));
    }

    public record PushOneRequest(UUID cooperatorId) {}
}
