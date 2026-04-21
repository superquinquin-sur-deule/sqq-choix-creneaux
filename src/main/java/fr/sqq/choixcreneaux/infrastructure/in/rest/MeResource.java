package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.query.GetMyRegistrationQuery;
import fr.sqq.mediator.Mediator;
import io.quarkus.oidc.IdToken;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.Set;
import java.util.UUID;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    SecurityIdentity identity;

    @Inject
    Mediator mediator;

    @GET
    public MeResponse getMe() {
        return new MeResponse(
                idToken.getClaim("preferred_username"),
                idToken.getClaim("email"),
                idToken.getClaim("given_name"),
                idToken.getClaim("family_name"),
                identity.getRoles()
        );
    }

    @GET
    @Path("/registration")
    public RegistrationResponse getRegistration() {
        String barcodeBase = idToken.getClaim("preferred_username");
        var result = mediator.send(new GetMyRegistrationQuery(barcodeBase));
        return new RegistrationResponse(result.registeredSlotId());
    }

    public record MeResponse(String barcodeBase, String email, String firstName, String lastName, Set<String> roles) {}
    public record RegistrationResponse(UUID registeredSlotId) {}
}
