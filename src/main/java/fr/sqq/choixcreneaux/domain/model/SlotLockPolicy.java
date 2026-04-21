package fr.sqq.choixcreneaux.domain.model;

import java.util.Collection;

public final class SlotLockPolicy {

    private final boolean anyUnderMinimum;

    private SlotLockPolicy(boolean anyUnderMinimum) {
        this.anyUnderMinimum = anyUnderMinimum;
    }

    public static SlotLockPolicy from(Collection<Slot> allSlots) {
        boolean anyUnderMin = allSlots.stream().anyMatch(Slot::isUnderMinimum);
        return new SlotLockPolicy(anyUnderMin);
    }

    public static SlotLockPolicy unlocked() {
        return new SlotLockPolicy(false);
    }

    public boolean isLockedFor(Slot slot) {
        return !slot.isUnderMinimum() && anyUnderMinimum;
    }
}
