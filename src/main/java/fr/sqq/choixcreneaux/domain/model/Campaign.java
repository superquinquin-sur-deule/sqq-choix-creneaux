package fr.sqq.choixcreneaux.domain.model;

import java.time.LocalDate;

public class Campaign {
    private CampaignStatus status;
    private LocalDate storeOpening;
    private LocalDate weekAReference;

    public Campaign(
            CampaignStatus status, LocalDate storeOpening, LocalDate weekAReference
    ) {
        this.status = status;
        this.storeOpening = storeOpening;
        this.weekAReference = weekAReference;
    }

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

    public CampaignStatus status() {
        return status;
    }

    public LocalDate storeOpening() {
        return storeOpening;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public void setStoreOpening(LocalDate storeOpening) {
        this.storeOpening = storeOpening;
    }

    public void setWeekAReference(LocalDate weekAReference) {
        this.weekAReference = weekAReference;
    }

    public LocalDate weekAReference() {
        return weekAReference;
    }
}
