package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import java.util.List;

public record AdminSlotView(
        SlotTemplate slot,
        int registrationCount,
        SlotStatus status,
        List<RegistrantSummary> registrants
) {}
