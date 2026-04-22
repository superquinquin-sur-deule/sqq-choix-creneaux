package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.hibernate.StaleStateException;

import java.time.temporal.ChronoUnit;
import java.util.UUID;

public record AdminAssignSlotCommand(UUID slotTemplateId, UUID cooperatorId) implements Command<AdminAssignSlotCommand.Result> {

    public record Result(boolean moved) {}

    @ApplicationScoped
    public static class Handler implements CommandHandler<AdminAssignSlotCommand, Result> {

        private final SlotRepository slotRepo;
        private final CooperatorRepository cooperatorRepo;
        private final SlotRegistrationFinder registrationRepo;
        private final EmailSender emailSender;
        private final EmailLogRepository emailLogRepo;

        @Inject
        public Handler(SlotRepository slotRepo, CooperatorRepository cooperatorRepo,
                       SlotRegistrationFinder registrationRepo,
                       EmailSender emailSender, EmailLogRepository emailLogRepo) {
            this.slotRepo = slotRepo;
            this.cooperatorRepo = cooperatorRepo;
            this.registrationRepo = registrationRepo;
            this.emailSender = emailSender;
            this.emailLogRepo = emailLogRepo;
        }

        @Override
        @Retry(maxRetries = 3, delay = 20, jitter = 10, delayUnit = ChronoUnit.MILLIS,
                retryOn = {OptimisticLockException.class, StaleStateException.class})
        @Transactional
        public Result handle(AdminAssignSlotCommand command) {
            var cooperator = cooperatorRepo.findById(command.cooperatorId())
                    .orElseThrow(() -> new RuntimeException("Cooperator not found"));

            var existing = registrationRepo.findByCooperatorId(cooperator.id());
            boolean moved = false;
            if (existing.isPresent()) {
                if (existing.get().slotTemplateId().equals(command.slotTemplateId())) {
                    return new Result(false);
                }
                Log.infof("Admin move: cooperator %s from slot %s to slot %s",
                        cooperator.id(), existing.get().slotTemplateId(), command.slotTemplateId());
                Slot sourceSlot = slotRepo.findById(existing.get().slotTemplateId())
                        .orElseThrow(() -> new RuntimeException("Source slot not found"));
                sourceSlot.unregister(cooperator.id());
                slotRepo.save(sourceSlot);
                moved = true;
            }

            Slot targetSlot = slotRepo.findById(command.slotTemplateId())
                    .orElseThrow(() -> new RuntimeException("Slot not found"));
            targetSlot.adminAssign(cooperator);
            slotRepo.save(targetSlot);

            try {
                emailSender.sendConfirmation(cooperator, targetSlot.asTemplateView(), targetSlot.week().name());
                emailLogRepo.log(cooperator.id(), EmailType.CONFIRMATION);
            } catch (Exception e) {
                Log.warn("Failed to send confirmation email", e);
            }
            return new Result(moved);
        }
    }
}
