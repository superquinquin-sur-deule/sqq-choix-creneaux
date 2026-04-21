package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

public record GetMyRegistrationQuery(String barcodeBase) implements Query<GetMyRegistrationQuery.Result> {
    public record Result(UUID registeredSlotId) {}

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetMyRegistrationQuery, Result> {
        private final CooperatorRepository cooperatorRepo;
        private final SlotRegistrationRepository registrationRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo, SlotRegistrationRepository registrationRepo) {
            this.cooperatorRepo = cooperatorRepo;
            this.registrationRepo = registrationRepo;
        }

        @Override
        public Result handle(GetMyRegistrationQuery query) {
            var coop = cooperatorRepo.findByBarcodeBase(query.barcodeBase());
            if (coop.isEmpty()) {
                return new Result(null);
            }
            var registration = registrationRepo.findByCooperatorId(coop.get().id());
            return new Result(registration.map(r -> r.slotTemplateId()).orElse(null));
        }
    }
}
