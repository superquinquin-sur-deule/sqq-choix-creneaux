package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.query.GetDashboardQuery;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/admin/dashboard")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class AdminDashboardResource {

    @Inject
    Mediator mediator;

    @GET
    public DashboardResponse getDashboard() {
        var result = mediator.send(new GetDashboardQuery());
        List<SlotResource.SlotResponse> slotsNeedingPeople = result.slotsNeedingPeople().stream()
                .map(SlotResource.SlotResponse::from)
                .toList();
        return new DashboardResponse(
                result.totalCooperators(),
                result.registeredCooperators(),
                result.pendingCooperators(),
                result.slotsUnderMinimum(),
                result.allMinimumsReached(),
                slotsNeedingPeople
        );
    }

    public record DashboardResponse(
            long totalCooperators,
            long registeredCooperators,
            long pendingCooperators,
            int slotsUnderMinimum,
            boolean allMinimumsReached,
            List<SlotResource.SlotResponse> slotsNeedingPeople
    ) {}
}
