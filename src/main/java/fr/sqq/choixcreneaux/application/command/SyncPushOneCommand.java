package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.*;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

public record SyncPushOneCommand(UUID cooperatorId) implements Command<SyncPushOneCommand.Result> {

    public record Result(boolean pushed, String reason, String outcome) {}

    @ApplicationScoped
    public static class Handler implements CommandHandler<SyncPushOneCommand, Result> {
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
        public Result handle(SyncPushOneCommand command) {
            Log.infof("SyncPushOneCommand: pushing registration for cooperator %s", command.cooperatorId());
            var registration = registrationRepo.findByCooperatorId(command.cooperatorId()).orElse(null);
            if (registration == null) {
                return new Result(false, "no_registration", null);
            }
            var slot = slotRepo.findById(registration.slotTemplateId()).orElse(null);
            var coop = cooperatorRepo.findById(registration.cooperatorId()).orElse(null);
            if (slot == null || coop == null || slot.odooTemplateId() == null || coop.odooPartnerId() == null) {
                Log.warnf("Cannot push registration for cooperator %s: missing Odoo IDs", command.cooperatorId());
                return new Result(false, "missing_odoo_ids", null);
            }
            PushOutcome outcome = odoo.pushRegistration(coop.odooPartnerId(), slot.odooTemplateId());
            return new Result(true, null, outcome.name().toLowerCase());
        }
    }
}
