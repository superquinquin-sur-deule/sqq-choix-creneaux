package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.command.SendReminderCommand;
import fr.sqq.choixcreneaux.application.service.ReminderBulkJob;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/api/admin/reminders")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"Member Manager", "Foodcoop Admin"})
public class AdminReminderResource {

    @Inject
    Mediator mediator;

    @Inject
    ReminderBulkJob bulkJob;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response sendReminders(ReminderRequest request) {
        if (request.all()) {
            bulkJob.schedule(request.onlyNeverReminded());
            return Response.accepted(new ReminderResponse(0, true)).build();
        }
        int sentCount = mediator.send(new SendReminderCommand(request.cooperatorIds(), false));
        return Response.ok(new ReminderResponse(sentCount, false)).build();
    }

    public record ReminderRequest(List<UUID> cooperatorIds, boolean all, boolean onlyNeverReminded) {}

    public record ReminderResponse(int sentCount, boolean scheduled) {}
}
