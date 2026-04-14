package fr.sqq.choixcreneaux.application.query;
import fr.sqq.mediator.Query;
import java.util.UUID;
public record GetMeQuery(String keycloakSubject, String email) implements Query<GetMeQuery.Result> {
    public record Result(UUID cooperatorId, String email, String firstName, String lastName, UUID registeredSlotId) {}
}
