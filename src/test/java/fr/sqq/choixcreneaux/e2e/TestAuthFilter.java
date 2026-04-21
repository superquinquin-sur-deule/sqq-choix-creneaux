package fr.sqq.choixcreneaux.e2e;

import io.quarkus.security.identity.CurrentIdentityAssociation;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

// Test-only filter: when a request carries an X-Test-Auth header, install a
// SecurityIdentity with that header as the principal name (= barcodeBase in
// this app). Used by the Cucumber/Playwright acceptance tests to authenticate
// browser requests without a real OIDC flow. Requests without the header are
// untouched, so @QuarkusTest/@TestSecurity tests keep Quarkus' default behavior.
@Provider
@ApplicationScoped
public class TestAuthFilter implements ContainerRequestFilter {

    private static final String HEADER = "X-Test-Auth";

    @Inject
    CurrentIdentityAssociation identityAssociation;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String barcode = requestContext.getHeaderString(HEADER);
        if (barcode == null || barcode.isBlank()) return;

        QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                .setPrincipal(new QuarkusPrincipal(barcode))
                .addRole("Foodcoop Admin")
                .addRole("Member Manager");
        identityAssociation.setIdentity(builder.build());
    }
}
