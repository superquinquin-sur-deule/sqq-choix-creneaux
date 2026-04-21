package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;

import java.util.List;

public record AdminSlotView(Slot slot, SlotStatus status, List<RegistrantSummary> registrants) {
    public int registrationCount() { return slot.registrationCount(); }
}
