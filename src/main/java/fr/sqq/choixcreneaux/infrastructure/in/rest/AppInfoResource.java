package fr.sqq.choixcreneaux.infrastructure.in.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Path("/api/app-info")
@Produces(MediaType.APPLICATION_JSON)
public class AppInfoResource {

    @ConfigProperty(name = "app.production", defaultValue = "false")
    boolean production;

    @GET
    public AppInfoResponse getAppInfo() {
        return new AppInfoResponse(production);
    }

    public record AppInfoResponse(boolean production) {}
}
