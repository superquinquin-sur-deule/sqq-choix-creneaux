package fr.sqq.choixcreneaux.application.query;
import fr.sqq.mediator.Query;
import java.util.UUID;
public record GetMyRegistrationQuery(String barcodeBase) implements Query<GetMyRegistrationQuery.Result> {
    public record Result(UUID registeredSlotId) {}
}
