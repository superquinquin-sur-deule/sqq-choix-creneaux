package fr.sqq.choixcreneaux.domain.exception;

public class SlotFullException extends DomainException {
    public SlotFullException() {
        super("Ce créneau est complet.");
    }
}
