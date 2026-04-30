package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationFinder;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record SearchCooperatorsQuery(String q, int page, int size) implements Query<PendingCooperatorsPage> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<SearchCooperatorsQuery, PendingCooperatorsPage> {
        private final CooperatorRepository cooperatorRepo;
        private final SlotRegistrationFinder registrationFinder;
        private final SlotRepository slotRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo,
                       SlotRegistrationFinder registrationFinder,
                       SlotRepository slotRepo) {
            this.cooperatorRepo = cooperatorRepo;
            this.registrationFinder = registrationFinder;
            this.slotRepo = slotRepo;
        }

        @Override
        public PendingCooperatorsPage handle(SearchCooperatorsQuery query) {
            int page = Math.max(1, query.page());
            int size = Math.clamp(query.size(), 1, 100);
            int offset = (page - 1) * size;
            long total = cooperatorRepo.countSearch(query.q());
            var items = cooperatorRepo.search(query.q(), offset, size);
            Map<UUID, CooperatorSlotSummary> slotByCooperatorId = new HashMap<>();
            for (Cooperator c : items) {
                registrationFinder.findByCooperatorId(c.id()).ifPresent(reg ->
                        slotRepo.findById(reg.slotTemplateId()).ifPresent(slot ->
                                slotByCooperatorId.put(c.id(), toSummary(slot))));
            }
            return new PendingCooperatorsPage(items, total, Map.of(), slotByCooperatorId);
        }

        private CooperatorSlotSummary toSummary(Slot slot) {
            return new CooperatorSlotSummary(
                    slot.week().name(), slot.dayOfWeek(), slot.startTime(), slot.endTime());
        }
    }
}
