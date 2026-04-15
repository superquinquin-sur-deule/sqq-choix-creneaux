package fr.sqq.choixcreneaux.domain.model;

import java.util.List;

public record AdminSlotView(
        SlotTemplate slot,
        int registrationCount,
        SlotStatus status,
        List<RegistrantSummary> registrants
) {}
