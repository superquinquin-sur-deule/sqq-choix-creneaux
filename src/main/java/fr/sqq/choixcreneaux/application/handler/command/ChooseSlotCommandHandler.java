package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.ChooseSlotCommand;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.choixcreneaux.domain.exception.AlreadyRegisteredException;
import fr.sqq.choixcreneaux.domain.model.Campaign;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotLockPolicy;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.hibernate.StaleStateException;

import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class ChooseSlotCommandHandler implements CommandHandler<ChooseSlotCommand, Void> {

    private final SlotRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;
    private final SlotRegistrationRepository registrationRepo;
    private final Campaign campaign;
    private final EmailSender emailSender;
    private final EmailLogRepository emailLogRepo;

    @Inject
    public ChooseSlotCommandHandler(SlotRepository slotRepo, CooperatorRepository cooperatorRepo,
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

        var allSlots = slotRepo.findAll();
        var lockPolicy = SlotLockPolicy.from(allSlots);
        Slot targetSlot = allSlots.stream()
                .filter(s -> s.id().equals(command.slotTemplateId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Slot not found"));

        targetSlot.register(cooperator, lockPolicy, campaign);
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
