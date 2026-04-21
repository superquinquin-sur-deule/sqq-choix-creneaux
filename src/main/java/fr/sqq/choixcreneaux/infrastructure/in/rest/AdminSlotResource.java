package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.command.AdminAssignSlotCommand;
import fr.sqq.choixcreneaux.application.command.AdminAssignSlotResult;
import fr.sqq.choixcreneaux.application.query.GetAdminSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.AdminSlotView;
import fr.sqq.choixcreneaux.domain.model.RegistrantSummary;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;

@Path("/api/admin/slots")
@Produces(MediaType.APPLICATION_JSON)
//@RolesAllowed("admin")
public class AdminSlotResource {

    @Inject
    Mediator mediator;

    @GET
    public List<AdminSlotResponse> getSlots() {
        return mediator.send(new GetAdminSlotsQuery()).stream()
                .map(AdminSlotResponse::from)
                .toList();
    }

    @POST
    @Path("/{slotId}/assign")
    @Consumes(MediaType.APPLICATION_JSON)
    public AssignResponse assign(@PathParam("slotId") UUID slotId, AssignRequest body) {
        AdminAssignSlotResult result = mediator.send(new AdminAssignSlotCommand(slotId, body.cooperatorId()));
        return new AssignResponse(result.moved());
    }

    public record AssignRequest(UUID cooperatorId) {}
    public record AssignResponse(boolean moved) {}

    public record AdminSlotResponse(
            UUID id,
            String week,
            DayOfWeek dayOfWeek,
            String startTime,
            String endTime,
            int minCapacity,
            int maxCapacity,
            int registrationCount,
            String status,
            List<RegistrantResponse> registrants
    ) {
        static AdminSlotResponse from(AdminSlotView v) {
            return new AdminSlotResponse(
                    v.slot().id(),
                    v.slot().week().name(),
                    v.slot().dayOfWeek(),
                    v.slot().startTime().toString(),
                    v.slot().endTime().toString(),
                    v.slot().minCapacity(),
                    v.slot().maxCapacity(),
                    v.registrationCount(),
                    v.status().name(),
                    v.registrants().stream().map(RegistrantResponse::from).toList()
            );
        }
    }

    public record RegistrantResponse(String firstName, String lastNameInitial) {
        static RegistrantResponse from(RegistrantSummary s) {
            return new RegistrantResponse(s.firstName(), s.lastNameInitial());
        }
    }
}
