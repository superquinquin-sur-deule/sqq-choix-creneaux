package fr.sqq.choixcreneaux.domain.model;

public record SlotWithFillInfo(SlotTemplate slot, int registrationCount, SlotStatus status) {}
