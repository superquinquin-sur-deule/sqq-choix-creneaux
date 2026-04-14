package fr.sqq.choixcreneaux.domain.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public enum Week {
    A, B, C, D;

    private static final Week[] VALUES = values();

    public static Week fromDate(LocalDate date, LocalDate weekAReference) {
        long daysBetween = ChronoUnit.DAYS.between(weekAReference, date);
        long weeksBetween = Math.floorDiv(daysBetween, 7);
        int index = Math.floorMod((int) weeksBetween, 4);
        return VALUES[index];
    }
}
