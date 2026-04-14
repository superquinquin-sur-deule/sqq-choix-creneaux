package fr.sqq.choixcreneaux.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record Campaign(
    UUID id, CampaignStatus status, Instant startDate, Instant endDate,
    LocalDate storeOpening, LocalDate weekAReference
) {
    public Week weekOf(LocalDate date) {
        return Week.fromDate(date, weekAReference);
    }

    public LocalDate firstMondayAfterOpening(Week targetWeek) {
        Week openingWeek = weekOf(storeOpening);
        int offset = Math.floorMod(targetWeek.ordinal() - openingWeek.ordinal(), 4);
        return storeOpening.plusWeeks(offset);
    }

    public boolean isOpen() {
        return status == CampaignStatus.OPEN;
    }
}
