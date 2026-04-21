package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.domain.model.SlotStatusCalculator;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

public record GetSlotsQuery() implements Query<List<SlotWithFillInfo>> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetSlotsQuery, List<SlotWithFillInfo>> {
        private final SlotTemplateRepository slotRepo;

        @Inject
        public Handler(SlotTemplateRepository slotRepo) {
            this.slotRepo = slotRepo;
        }

        @Override
        public List<SlotWithFillInfo> handle(GetSlotsQuery query) {
            var slots = slotRepo.findAll();
            var counts = slotRepo.countRegistrationsPerSlot();

            boolean anyUnderMinimum = slots.stream().anyMatch(s -> {
                int count = counts.getOrDefault(s.id(), 0);
                return count < s.minCapacity();
            });

            return slots.stream().map(slot -> {
                int count = counts.getOrDefault(slot.id(), 0);
                var status = SlotStatusCalculator.compute(slot.minCapacity(), slot.maxCapacity(), count, anyUnderMinimum);
                return new SlotWithFillInfo(slot, count, status);
            }).toList();
        }
    }
}
