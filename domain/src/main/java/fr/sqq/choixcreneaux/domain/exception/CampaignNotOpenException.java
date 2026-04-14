package fr.sqq.choixcreneaux.domain.exception;

public class CampaignNotOpenException extends DomainException {
    public CampaignNotOpenException() {
        super("La campagne de choix n'est pas ouverte.");
    }
}
