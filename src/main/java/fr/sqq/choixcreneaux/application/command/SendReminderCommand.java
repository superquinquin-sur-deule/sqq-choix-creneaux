package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailSender;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.UUID;

public record SendReminderCommand(List<UUID> cooperatorIds, boolean all) implements Command<Integer> {

    @ApplicationScoped
    public static class Handler implements CommandHandler<SendReminderCommand, Integer> {
        private final CooperatorRepository cooperatorRepo;
        private final EmailSender emailSender;
        private final EmailLogRepository emailLogRepo;
        private final String appUrl;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo, EmailSender emailSender,
                       EmailLogRepository emailLogRepo,
                       @ConfigProperty(name = "app.url", defaultValue = "http://localhost:8080") String appUrl) {
            this.cooperatorRepo = cooperatorRepo;
            this.emailSender = emailSender;
            this.emailLogRepo = emailLogRepo;
            this.appUrl = appUrl;
        }

        @Override
        public Integer handle(SendReminderCommand command) {
            List<Cooperator> targets;
            if (command.all()) {
                targets = cooperatorRepo.findWithoutRegistration();
            } else {
                targets = command.cooperatorIds().stream()
                        .map(id -> cooperatorRepo.findById(id).orElse(null))
                        .filter(java.util.Objects::nonNull).toList();
            }
            int sent = 0;
            for (var coop : targets) {
                try {
                    emailSender.sendReminder(coop, appUrl);
                    emailLogRepo.log(coop.id(), EmailType.REMINDER);
                    sent++;
                } catch (Exception e) {
                    Log.warnf("Failed to send reminder to %s: %s", coop.email(), e.getMessage());
                }
            }
            return sent;
        }
    }
}
