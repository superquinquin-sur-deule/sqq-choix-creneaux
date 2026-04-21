package fr.sqq.choixcreneaux.e2e;

import io.quarkus.oidc.IdToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.mockito.Mockito;

// OIDC is disabled in the test profile, so the normal @IdToken JsonWebToken
// producer from quarkus-oidc is absent. Supply a stub returning the expected
// claims so MeResource can serve /api/me and /api/me/registration during
// acceptance tests that exercise real endpoints.
@ApplicationScoped
public class TestJwtProducer {

    @Produces
    @IdToken
    public JsonWebToken idToken() {
        JsonWebToken jwt = Mockito.mock(JsonWebToken.class);
        Mockito.when(jwt.<String>getClaim("preferred_username")).thenReturn("12345");
        Mockito.when(jwt.<String>getClaim("email")).thenReturn("coop@example.com");
        Mockito.when(jwt.<String>getClaim("given_name")).thenReturn("Jean");
        Mockito.when(jwt.<String>getClaim("family_name")).thenReturn("Dupont");
        return jwt;
    }
}
