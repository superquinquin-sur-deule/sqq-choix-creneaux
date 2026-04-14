package fr.sqq.choixcreneaux.domain.exception;

public class SlotLockedException extends DomainException {
    public SlotLockedException() {
        super("Ce créneau est verrouillé. Choisissez un créneau qui a encore besoin de coopérateurs.");
    }
}
