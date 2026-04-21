package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

public record GetSlotsQuery() implements Query<List<SlotWithFillInfo>> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetSlotsQuery, List<SlotWithFillInfo>> {
        private final SlotRepository slotRepo;

        @Inject
        public Handler(SlotRepository slotRepo) {
            this.slotRepo = slotRepo;
        }

        @Override
        public List<SlotWithFillInfo> handle(GetSlotsQuery query) {
            var slots = slotRepo.findAll();
            boolean anyUnderMin = slots.stream().anyMatch(s -> s.status() == SlotStatus.NEEDS_PEOPLE);
            return slots.stream().map(slot -> {
                SlotStatus status = (anyUnderMin && slot.status() == SlotStatus.OPEN) ? SlotStatus.LOCKED : slot.status();
                return new SlotWithFillInfo(slot, status);
            }).toList();
        }
    }
}
