package fr.sqq.choixcreneaux.infrastructure.out.email;

import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.DayOfWeek;
import java.util.Map;

@ApplicationScoped
public class BrevoEmailSender implements EmailSender {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private static final Map<DayOfWeek, String> DAY_NAMES = Map.of(
            DayOfWeek.MONDAY, "Lundi",
            DayOfWeek.TUESDAY, "Mardi",
            DayOfWeek.WEDNESDAY, "Mercredi",
            DayOfWeek.THURSDAY, "Jeudi",
            DayOfWeek.FRIDAY, "Vendredi",
            DayOfWeek.SATURDAY, "Samedi",
            DayOfWeek.SUNDAY, "Dimanche"
    );

    @ConfigProperty(name = "brevo.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "brevo.api-key", defaultValue = "")
    String apiKey;

    @ConfigProperty(name = "brevo.confirmation-template-id", defaultValue = "0")
    int confirmationTemplateId;

    @ConfigProperty(name = "brevo.reminder-template-id", defaultValue = "56")
    int reminderTemplateId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public void sendConfirmation(Cooperator cooperator, SlotTemplate slot, String weekLabel) {
        String dayName = DAY_NAMES.getOrDefault(slot.dayOfWeek(), slot.dayOfWeek().name());
        if (!enabled) {
            Log.infof("""
                    ╔══════════════════════════════════════════════════════════╗
                    ║  📧 EMAIL CONFIRMATION (brevo disabled)                 ║
                    ╠══════════════════════════════════════════════════════════╣
                    ║  To:      %s (%s %s)
                    ║  Créneau: Semaine %s — %s %s-%s
                    ╚══════════════════════════════════════════════════════════╝""",
                    cooperator.email(), cooperator.firstName(), cooperator.lastName(),
                    weekLabel, dayName, slot.startTime(), slot.endTime());
            return;
        }
        String params = """
                {"firstName":"%s","lastName":"%s","weekLabel":"%s","dayName":"%s","startTime":"%s","endTime":"%s"}
                """.formatted(
                escape(cooperator.firstName()),
                escape(cooperator.lastName()),
                escape(weekLabel),
                dayName,
                slot.startTime().toString(),
                slot.endTime().toString()
        ).trim();

        String body = buildBody(confirmationTemplateId, cooperator.email(), cooperator.firstName(), cooperator.lastName(), params);
        send(body, cooperator.email());
    }

    @Override
    public void sendReminder(Cooperator cooperator) {
        if (!enabled) {
            Log.infof("""
                    ╔══════════════════════════════════════════════════════════╗
                    ║  📧 EMAIL RELANCE (brevo disabled)                      ║
                    ╠══════════════════════════════════════════════════════════╣
                    ║  To: %s (%s %s)
                    ╚══════════════════════════════════════════════════════════╝""",
                    cooperator.email(), cooperator.firstName(), cooperator.lastName());
            return;
        }
        String body = """
                {"templateId":%d,"to":[{"email":"%s","name":"%s %s"}]}
                """.formatted(
                reminderTemplateId,
                escape(cooperator.email()),
                escape(cooperator.firstName()),
                escape(cooperator.lastName())
        ).trim();
        send(body, cooperator.email());
    }

    private String buildBody(int templateId, String email, String firstName, String lastName, String params) {
        return """
                {"templateId":%d,"to":[{"email":"%s","name":"%s %s"}],"params":%s}
                """.formatted(templateId, escape(email), escape(firstName), escape(lastName), params).trim();
    }

    private void send(String body, String recipientEmail) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BREVO_API_URL))
                    .header("api-key", apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Log.info("Email sent via Brevo to " + recipientEmail);
            } else {
                Log.warnf("Brevo returned status %d for %s: %s", response.statusCode(), recipientEmail, response.body());
            }
        } catch (Exception e) {
            Log.errorf(e, "Failed to send Brevo email to %s", recipientEmail);
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
