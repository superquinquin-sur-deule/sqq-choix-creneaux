package fr.sqq.choixcreneaux.domain.exception;

public class AlreadyRegisteredException extends DomainException {
    public AlreadyRegisteredException() {
        super("Vous avez déjà choisi un créneau.");
    }
}
