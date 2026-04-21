package fr.sqq.choixcreneaux.acceptance;

import fr.sqq.choixcreneaux.application.command.SyncCooperatorsCommand;
import fr.sqq.choixcreneaux.application.command.SyncPullCommand;
import fr.sqq.choixcreneaux.application.command.SyncPushCommand;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

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

    @POST
    @Path("/push")
    public PushResponse push() {
        return new PushResponse(mediator.send(new SyncPushCommand()));
    }

    @POST
    @Path("/cooperators")
    public SyncCooperatorsCommand.Result syncCooperators() {
        return mediator.send(new SyncCooperatorsCommand());
    }

    public record PushResponse(int pushedCount) {}
}
