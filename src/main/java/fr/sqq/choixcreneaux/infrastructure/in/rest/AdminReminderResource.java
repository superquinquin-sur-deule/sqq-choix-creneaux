package fr.sqq.choixcreneaux.infrastructure.in.rest;

import fr.sqq.choixcreneaux.application.command.SendReminderCommand;
import fr.sqq.mediator.Mediator;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/admin/reminders")
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("admin")
public class AdminReminderResource {

    @Inject
    Mediator mediator;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public ReminderResponse sendReminders(ReminderRequest request) {
        int sentCount = mediator.send(new SendReminderCommand(request.cooperatorIds(), request.all()));
        return new ReminderResponse(sentCount);
    }

    public record ReminderRequest(List<UUID> cooperatorIds, boolean all) {}

    public record ReminderResponse(int sentCount) {}
}
