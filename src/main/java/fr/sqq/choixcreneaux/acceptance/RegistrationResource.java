package fr.sqq.choixcreneaux.acceptance;

import fr.sqq.choixcreneaux.application.command.ChooseSlotCommand;
import fr.sqq.mediator.Mediator;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/slots")
@Produces(MediaType.APPLICATION_JSON)
public class RegistrationResource {

    @Inject
    Mediator mediator;

    @Inject
    SecurityIdentity identity;

    @POST
    @Path("/{slotId}/register")
    public Response register(@PathParam("slotId") UUID slotId) {
        String barcodeBase = identity.getPrincipal().getName();
        mediator.send(new ChooseSlotCommand(slotId, barcodeBase));
        return Response.noContent().build();
    }
}
