package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public record SyncPushCommand() implements Command<Integer> {

    @ApplicationScoped
    public static class Handler implements CommandHandler<SyncPushCommand, Integer> {
        private final SlotRegistrationFinder registrationRepo;
        private final SlotTemplateFinder slotRepo;
        private final CooperatorRepository cooperatorRepo;
        private final OdooSyncPort odoo;

        @Inject
        public Handler(SlotRegistrationFinder registrationRepo, SlotTemplateFinder slotRepo,
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
                    Log.warnf("Skipping registration %s: missing Odoo IDs", reg.id());
                    continue;
                }
                try {
                    odoo.pushRegistration(coop.odooPartnerId(), slot.odooTemplateId());
                    pushed++;
                } catch (Exception e) {
                    Log.errorf("Failed to push registration %s: %s", reg.id(), e.getMessage());
                }
            }
            return pushed;
        }
    }
}
