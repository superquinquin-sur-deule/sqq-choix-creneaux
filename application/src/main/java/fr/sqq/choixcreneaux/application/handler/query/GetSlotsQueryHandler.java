package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.SlotWithFillInfo;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class GetSlotsQueryHandler implements QueryHandler<GetSlotsQuery, List<SlotWithFillInfo>> {
    private final SlotTemplateRepository slotRepo;

    @Inject
    public GetSlotsQueryHandler(SlotTemplateRepository slotRepo) {
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
            SlotStatus status = computeStatus(slot.minCapacity(), slot.maxCapacity(), count, anyUnderMinimum);
            return new SlotWithFillInfo(slot, count, status);
        }).toList();
    }

    private SlotStatus computeStatus(int min, int max, int count, boolean anyUnderMinimum) {
        if (count >= max) return SlotStatus.FULL;
        if (count < min) return SlotStatus.NEEDS_PEOPLE;
        if (anyUnderMinimum) return SlotStatus.LOCKED;
        return SlotStatus.OPEN;
    }
}
