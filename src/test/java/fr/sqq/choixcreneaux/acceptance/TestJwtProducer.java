package fr.sqq.choixcreneaux.acceptance;

import io.quarkus.oidc.IdToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.mockito.Mockito;

// OIDC is disabled in the test profile (no Keycloak), so the normal
// @IdToken JsonWebToken producer from quarkus-oidc is absent. Supply a
// stub here so CDI resolution succeeds. The UI tests mock the endpoints
// that would read this token at the browser layer, so the stub is never
// actually invoked.
@ApplicationScoped
public class TestJwtProducer {

    @Produces
    @IdToken
    public JsonWebToken idToken() {
        return Mockito.mock(JsonWebToken.class);
    }
}
