package fr.sqq.choixcreneaux.application.query;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CooperatorSlotSummary(String week, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {}
