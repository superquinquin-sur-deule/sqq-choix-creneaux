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
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

public record SendReminderCommand(List<UUID> cooperatorIds, boolean all, boolean onlyNeverReminded) implements Command<Integer> {

    public SendReminderCommand(List<UUID> cooperatorIds, boolean all) {
        this(cooperatorIds, all, false);
    }

    @ApplicationScoped
    @Transactional
    public static class Handler implements CommandHandler<SendReminderCommand, Integer> {
        private final CooperatorRepository cooperatorRepo;
        private final EmailSender emailSender;
        private final EmailLogRepository emailLogRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo, EmailSender emailSender,
                       EmailLogRepository emailLogRepo) {
            this.cooperatorRepo = cooperatorRepo;
            this.emailSender = emailSender;
            this.emailLogRepo = emailLogRepo;
        }

        @Override
        public Integer handle(SendReminderCommand command) {
            Log.infof("SendReminderCommand: all=%s onlyNeverReminded=%s cooperatorIds=%s",
                    command.all(), command.onlyNeverReminded(),
                    command.cooperatorIds() == null ? 0 : command.cooperatorIds().size());
            List<Cooperator> targets;
            if (command.all()) {
                targets = cooperatorRepo.findWithoutRegistration();
                if (command.onlyNeverReminded()) {
                    var ids = targets.stream().map(Cooperator::id).toList();
                    var alreadyReminded = emailLogRepo.findLastSentByCooperators(ids, EmailType.REMINDER);
                    targets = targets.stream()
                            .filter(c -> !alreadyReminded.containsKey(c.id()))
                            .toList();
                }
            } else {
                targets = command.cooperatorIds().stream()
                        .map(id -> cooperatorRepo.findById(id).orElse(null))
                        .filter(java.util.Objects::nonNull).toList();
            }
            int sent = 0;
            for (var coop : targets) {
                try {
                    emailSender.sendReminder(coop);
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
