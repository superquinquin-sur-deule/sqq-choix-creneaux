package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

public record SyncCooperatorsCommand() implements Command<SyncCooperatorsCommand.Result> {
    public record Result(int cooperatorsImported) {}

    @ApplicationScoped
    public static class Handler implements CommandHandler<SyncCooperatorsCommand, Result> {
        private final OdooSyncPort odoo;
        private final CooperatorRepository cooperatorRepo;

        @Inject
        public Handler(OdooSyncPort odoo, CooperatorRepository cooperatorRepo) {
            this.odoo = odoo;
            this.cooperatorRepo = cooperatorRepo;
        }

        @Override
        @Transactional
        public Result handle(SyncCooperatorsCommand command) {
            Log.info("SyncCooperatorsCommand: pulling cooperators from Odoo");
            var cooperators = odoo.pullCooperators();
            cooperatorRepo.saveAll(cooperators);
            return new Result(cooperators.size());
        }
    }
}
