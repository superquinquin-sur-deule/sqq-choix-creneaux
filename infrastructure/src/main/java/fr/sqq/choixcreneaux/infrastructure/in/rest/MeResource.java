package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.query.GetMeQuery;
import fr.sqq.mediator.Mediator;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.UUID;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    @Inject
    Mediator mediator;

    @Inject
    SecurityIdentity identity;

    @Inject
    @IdToken
    JsonWebToken idToken;

    @GET
    public MeResponse getMe() {
        String subject = identity.getPrincipal().getName();
        String email = idToken.getClaim("email");
        if (email == null) {
            email = identity.getAttribute("email");
        }
        var result = mediator.send(new GetMeQuery(subject, email));
        return new MeResponse(result.cooperatorId(), result.email(), result.firstName(), result.lastName(), result.registeredSlotId());
    }

    public record MeResponse(UUID cooperatorId, String email, String firstName, String lastName, UUID registeredSlotId) {}
}
