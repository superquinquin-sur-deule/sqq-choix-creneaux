package fr.sqq.choixcreneaux.infrastructure.out.email;

import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.DayOfWeek;
import java.util.Map;

@ApplicationScoped
@UnlessBuildProfile("prod")
public class LoggingEmailSender implements EmailSender {

    private static final Logger LOG = Logger.getLogger(LoggingEmailSender.class);

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Lundi", DayOfWeek.TUESDAY, "Mardi",
            DayOfWeek.WEDNESDAY, "Mercredi", DayOfWeek.THURSDAY, "Jeudi",
            DayOfWeek.FRIDAY, "Vendredi", DayOfWeek.SATURDAY, "Samedi"
    );

    @Override
    public void sendConfirmation(Cooperator cooperator, SlotTemplate slot, String weekLabel) {
        LOG.infof("""
                ╔══════════════════════════════════════════════════════════╗
                ║  📧 EMAIL CONFIRMATION                                  ║
                ╠══════════════════════════════════════════════════════════╣
                ║  To:      %s (%s %s)
                ║  Créneau: Semaine %s — %s %s-%s
                ╚══════════════════════════════════════════════════════════╝""",
                cooperator.email(), cooperator.firstName(), cooperator.lastName(),
                weekLabel,
                DAY_NAMES.getOrDefault(slot.dayOfWeek(), slot.dayOfWeek().name()),
                slot.startTime(), slot.endTime());
    }

    @Override
    public void sendReminder(Cooperator cooperator, String appUrl) {
        LOG.infof("""
                ╔══════════════════════════════════════════════════════════╗
                ║  📧 EMAIL RELANCE                                       ║
                ╠══════════════════════════════════════════════════════════╣
                ║  To:   %s (%s %s)
                ║  Lien: %s
                ╚══════════════════════════════════════════════════════════╝""",
                cooperator.email(), cooperator.firstName(), cooperator.lastName(),
                appUrl);
    }
}
