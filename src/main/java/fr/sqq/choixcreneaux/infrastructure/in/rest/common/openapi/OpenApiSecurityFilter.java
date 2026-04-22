package fr.sqq.choixcreneaux.infrastructure.in.rest.common.openapi;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.OAuthFlows;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

public class OpenApiSecurityFilter implements OASFilter {

    private static final String SCHEME_NAME = "SecurityScheme";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        String authServerUrl = ConfigProvider.getConfig()
                .getOptionalValue("quarkus.oidc.auth-server-url", String.class)
                .orElse("");

        OAuthFlow authCode = OASFactory.createOAuthFlow()
                .authorizationUrl(authServerUrl + "/protocol/openid-connect/auth")
                .tokenUrl(authServerUrl + "/protocol/openid-connect/token")
                .refreshUrl(authServerUrl + "/protocol/openid-connect/token")
                .scopes(java.util.Map.of("openid", "OpenID Connect"));

        OAuthFlows flows = OASFactory.createOAuthFlows().authorizationCode(authCode);

        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Keycloak OIDC (Authorization Code + PKCE)")
                .flows(flows);

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme(SCHEME_NAME, scheme);
        openAPI.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme(SCHEME_NAME));
    }
}
