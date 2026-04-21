package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;

public record SlotWithFillInfo(Slot slot, SlotStatus status) {
    public int registrationCount() { return slot.registrationCount(); }
}
