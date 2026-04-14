package fr.sqq.choixcreneaux.domain.model;

import java.time.Instant;
import java.util.UUID;

public record SlotRegistration(UUID id, UUID slotTemplateId, UUID cooperatorId, Instant registeredAt) {}
