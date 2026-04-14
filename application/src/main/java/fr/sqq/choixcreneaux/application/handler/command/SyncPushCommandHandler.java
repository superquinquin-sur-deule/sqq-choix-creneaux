package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.SyncPushCommand;
import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.mediator.CommandHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SyncPushCommandHandler implements CommandHandler<SyncPushCommand, Integer> {
    private static final Logger LOG = Logger.getLogger(SyncPushCommandHandler.class);
    private final SlotRegistrationRepository registrationRepo;
    private final SlotTemplateRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;
    private final OdooSyncPort odoo;

    @Inject
    public SyncPushCommandHandler(SlotRegistrationRepository registrationRepo, SlotTemplateRepository slotRepo,
                                   CooperatorRepository cooperatorRepo, OdooSyncPort odoo) {
        this.registrationRepo = registrationRepo;
        this.slotRepo = slotRepo;
        this.cooperatorRepo = cooperatorRepo;
        this.odoo = odoo;
    }

    @Override
    public Integer handle(SyncPushCommand command) {
        var registrations = registrationRepo.findAll();
        int pushed = 0;
        for (var reg : registrations) {
            var slot = slotRepo.findById(reg.slotTemplateId()).orElse(null);
            var coop = cooperatorRepo.findById(reg.cooperatorId()).orElse(null);
            if (slot == null || coop == null || slot.odooTemplateId() == null || coop.odooPartnerId() == null) {
                LOG.warnf("Skipping registration %s: missing Odoo IDs", reg.id());
                continue;
            }
            try {
                odoo.pushRegistration(coop.odooPartnerId(), slot.odooTemplateId());
                pushed++;
            } catch (Exception e) {
                LOG.errorf("Failed to push registration %s: %s", reg.id(), e.getMessage());
            }
        }
        return pushed;
    }
}
