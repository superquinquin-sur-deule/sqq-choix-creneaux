package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;

public record SlotWithFillInfo(SlotTemplate slot, int registrationCount, SlotStatus status) {}
