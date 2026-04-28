package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.query.GetPendingCooperatorsPageQuery;
import fr.sqq.choixcreneaux.application.query.GetPendingCooperatorsQuery;
import fr.sqq.choixcreneaux.application.query.PendingCooperatorsPage;
import fr.sqq.choixcreneaux.application.query.SearchCooperatorsQuery;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Path("/api/admin/cooperators")
@RolesAllowed({"Member Manager", "Foodcoop Admin"})
public class AdminCooperatorResource {

    @Inject
    Mediator mediator;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public PageResponse getPendingCooperators(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        PendingCooperatorsPage result = mediator.send(new GetPendingCooperatorsPageQuery(page, size));
        List<CooperatorResponse> items = result.items().stream()
                .map(c -> new CooperatorResponse(c.id(), c.email(), c.firstName(), c.lastName(),
                        result.lastReminderByCooperatorId().get(c.id())))
                .toList();
        return new PageResponse(items, result.total(), page, size);
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public PageResponse search(
            @QueryParam("q") @DefaultValue("") String q,
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size) {
        PendingCooperatorsPage result = mediator.send(new SearchCooperatorsQuery(q, page, size));
        List<CooperatorResponse> items = result.items().stream()
                .map(c -> new CooperatorResponse(c.id(), c.email(), c.firstName(), c.lastName(),
                        result.lastReminderByCooperatorId().get(c.id())))
                .toList();
        return new PageResponse(items, result.total(), page, size);
    }

    @GET
    @Path("/export")
    @Produces("text/csv")
    public String exportCsv() {
        List<Cooperator> cooperators = mediator.send(new GetPendingCooperatorsQuery());
        var sb = new StringBuilder("id,email,first_name,last_name\n");
        for (Cooperator cooperator : cooperators) {
            sb.append(cooperator.id()).append(',')
              .append(cooperator.email()).append(',')
              .append(cooperator.firstName()).append(',')
              .append(cooperator.lastName()).append('\n');
        }
        return sb.toString();
    }

    public record CooperatorResponse(UUID id, String email, String firstName, String lastName, Instant lastReminderAt) {}

    public record PageResponse(List<CooperatorResponse> items, long total, int page, int size) {}
}
