package fr.sqq.choixcreneaux.domain.model;

public final class SlotStatusCalculator {

    private SlotStatusCalculator() {}

    public static SlotStatus compute(int min, int max, int count, boolean anyUnderMinimum) {
        if (count >= max) return SlotStatus.FULL;
        if (count < min) return SlotStatus.NEEDS_PEOPLE;
        if (anyUnderMinimum) return SlotStatus.LOCKED;
        return SlotStatus.OPEN;
    }
}
