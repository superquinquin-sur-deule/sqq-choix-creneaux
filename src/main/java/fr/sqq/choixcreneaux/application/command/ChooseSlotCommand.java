package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.exception.AlreadyRegisteredException;
import fr.sqq.choixcreneaux.domain.exception.SlotLockedException;
import fr.sqq.choixcreneaux.domain.model.Campaign;
import fr.sqq.choixcreneaux.domain.model.EmailType;
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

public record ChooseSlotCommand(UUID slotTemplateId, String barcodeBase) implements Command<Void> {

    @ApplicationScoped
    public static class Handler implements CommandHandler<ChooseSlotCommand, Void> {

        private final SlotRepository slotRepo;
        private final CooperatorRepository cooperatorRepo;
        private final SlotRegistrationRepository registrationRepo;
        private final Campaign campaign;
        private final EmailSender emailSender;
        private final EmailLogRepository emailLogRepo;

        @Inject
        public Handler(SlotRepository slotRepo, CooperatorRepository cooperatorRepo,
                       SlotRegistrationRepository registrationRepo, Campaign campaign,
                       EmailSender emailSender, EmailLogRepository emailLogRepo) {
            this.slotRepo = slotRepo;
            this.cooperatorRepo = cooperatorRepo;
            this.registrationRepo = registrationRepo;
            this.campaign = campaign;
            this.emailSender = emailSender;
            this.emailLogRepo = emailLogRepo;
        }

        @Override
        @Retry(maxRetries = 3, delay = 20, jitter = 10, delayUnit = ChronoUnit.MILLIS,
                retryOn = {OptimisticLockException.class, StaleStateException.class})
        @Transactional
        public Void handle(ChooseSlotCommand command) {
            var cooperator = cooperatorRepo.findByBarcodeBase(command.barcodeBase())
                    .orElseThrow(() -> new RuntimeException(
                            "Cooperator with barcode base %s not found".formatted(command.barcodeBase())));

            if (registrationRepo.findByCooperatorId(cooperator.id()).isPresent()) {
                throw new AlreadyRegisteredException();
            }

            var targetSlot = slotRepo.findById(command.slotTemplateId())
                    .orElseThrow(() -> new RuntimeException("Slot not found"));

            if (slotRepo.anyUnderMinimum() && !targetSlot.isUnderMinimum()) {
                throw new SlotLockedException();
            }

            targetSlot.register(cooperator, campaign);
            slotRepo.save(targetSlot);

            try {
                emailSender.sendConfirmation(cooperator, targetSlot.asTemplateView(), targetSlot.week().name());
                emailLogRepo.log(cooperator.id(), EmailType.CONFIRMATION);
            } catch (Exception e) {
                Log.warn("Failed to send confirmation email", e);
            }
            return null;
        }
    }
}
