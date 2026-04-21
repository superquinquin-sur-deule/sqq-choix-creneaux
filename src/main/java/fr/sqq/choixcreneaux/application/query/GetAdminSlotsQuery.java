package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

public record GetAdminSlotsQuery() implements Query<List<AdminSlotView>> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetAdminSlotsQuery, List<AdminSlotView>> {

        private final SlotRepository slotRepo;
        private final CooperatorRepository cooperatorRepo;

        @Inject
        public Handler(SlotRepository slotRepo, CooperatorRepository cooperatorRepo) {
            this.slotRepo = slotRepo;
            this.cooperatorRepo = cooperatorRepo;
        }

        @Override
        public List<AdminSlotView> handle(GetAdminSlotsQuery query) {
            var slots = slotRepo.findAll();
            boolean anyUnderMin = slots.stream().anyMatch(s -> s.status() == SlotStatus.NEEDS_PEOPLE);

            Set<UUID> cooperatorIds = slots.stream()
                    .flatMap(s -> s.registrations().stream())
                    .map(SlotRegistration::cooperatorId)
                    .collect(Collectors.toSet());
            Map<UUID, Cooperator> cooperatorsById = cooperatorRepo.findAllById(cooperatorIds).stream()
                    .collect(Collectors.toMap(Cooperator::id, c -> c));

            return slots.stream().map(slot -> {
                SlotStatus status = (anyUnderMin && slot.status() == SlotStatus.OPEN) ? SlotStatus.LOCKED : slot.status();
                var registrants = slot.registrations().stream()
                        .map(r -> toSummary(cooperatorsById.get(r.cooperatorId())))
                        .filter(Objects::nonNull)
                        .toList();
                return new AdminSlotView(slot, status, registrants);
            }).toList();
        }

        private RegistrantSummary toSummary(Cooperator cooperator) {
            if (cooperator == null) return null;
            return new RegistrantSummary(cooperator.id(), cooperator.firstName(), lastNameInitial(cooperator.lastName()));
        }

        private String lastNameInitial(String lastName) {
            if (lastName == null || lastName.isBlank()) return "";
            return lastName.substring(0, 1).toUpperCase() + ".";
        }
    }
}
