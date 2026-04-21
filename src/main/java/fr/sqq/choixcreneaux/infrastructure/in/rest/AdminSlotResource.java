package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.command.AdminAssignSlotCommand;
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

//TODO: Extrait les dtos dans un un dossier dto
//TODO: Fusionne toutes les resources admin pour avoir une seule classe avec tous les endpoints
//TODO: Ajoute mapstruct pour faire le mapping des entre les dto d'api et ceux de la couche application
@Path("/api/admin/slots")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"Member Manager", "Foodcoop Admin"})
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
        AdminAssignSlotCommand.Result result = mediator.send(new AdminAssignSlotCommand(slotId, body.cooperatorId()));
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
