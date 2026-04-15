package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
public interface EmailSender {
    void sendConfirmation(Cooperator cooperator, SlotTemplate slot, String weekLabel);
    void sendReminder(Cooperator cooperator, String appUrl);
}
