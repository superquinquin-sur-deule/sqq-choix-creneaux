package fr.sqq.choixcreneaux.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

public record SlotTemplate(
    UUID id, Week week, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime,
    int minCapacity, int maxCapacity, Long odooTemplateId
) {}
