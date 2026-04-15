package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.application.query.GetAdminSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.*;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class GetAdminSlotsQueryHandler implements QueryHandler<GetAdminSlotsQuery, List<AdminSlotView>> {

    private final SlotTemplateRepository slotRepo;
    private final SlotRegistrationRepository registrationRepo;
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public GetAdminSlotsQueryHandler(SlotTemplateRepository slotRepo,
                                     SlotRegistrationRepository registrationRepo,
                                     CooperatorRepository cooperatorRepo) {
        this.slotRepo = slotRepo;
        this.registrationRepo = registrationRepo;
        this.cooperatorRepo = cooperatorRepo;
    }

    @Override
    public List<AdminSlotView> handle(GetAdminSlotsQuery query) {
        var slots = slotRepo.findAll();
        var registrations = registrationRepo.findAll();

        Map<UUID, List<SlotRegistration>> regsBySlot = registrations.stream()
                .collect(Collectors.groupingBy(SlotRegistration::slotTemplateId));

        Set<UUID> cooperatorIds = registrations.stream()
                .map(SlotRegistration::cooperatorId).collect(Collectors.toSet());
        Map<UUID, Cooperator> cooperatorsById = cooperatorRepo.findAllById(cooperatorIds).stream()
                .collect(Collectors.toMap(Cooperator::id, c -> c));

        boolean anyUnderMinimum = slots.stream().anyMatch(s -> {
            int count = regsBySlot.getOrDefault(s.id(), List.of()).size();
            return count < s.minCapacity();
        });

        return slots.stream().map(slot -> {
            var slotRegs = regsBySlot.getOrDefault(slot.id(), List.of());
            int count = slotRegs.size();
            var status = SlotStatusCalculator.compute(slot.minCapacity(), slot.maxCapacity(), count, anyUnderMinimum);
            var registrants = slotRegs.stream()
                    .map(r -> toSummary(r, cooperatorsById.get(r.cooperatorId())))
                    .filter(Objects::nonNull)
                    .toList();
            return new AdminSlotView(slot, count, status, registrants);
        }).toList();
    }

    private RegistrantSummary toSummary(SlotRegistration registration, Cooperator cooperator) {
        if (cooperator == null) return null;
        return new RegistrantSummary(cooperator.id(), cooperator.firstName(), lastNameInitial(cooperator.lastName()));
    }

    private String lastNameInitial(String lastName) {
        if (lastName == null || lastName.isBlank()) return "";
        return lastName.substring(0, 1).toUpperCase() + ".";
    }
}
