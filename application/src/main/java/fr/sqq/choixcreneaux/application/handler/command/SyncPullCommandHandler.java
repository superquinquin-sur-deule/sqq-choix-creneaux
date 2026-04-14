package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.SyncPullCommand;
import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.mediator.CommandHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class SyncPullCommandHandler implements CommandHandler<SyncPullCommand, SyncPullCommand.Result> {
    private final OdooSyncPort odoo;
    private final SlotTemplateRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public SyncPullCommandHandler(OdooSyncPort odoo, SlotTemplateRepository slotRepo, CooperatorRepository cooperatorRepo) {
        this.odoo = odoo;
        this.slotRepo = slotRepo;
        this.cooperatorRepo = cooperatorRepo;
    }

    @Override
    @Transactional
    public SyncPullCommand.Result handle(SyncPullCommand command) {
        var templates = odoo.pullSlotTemplates();
        slotRepo.saveAll(templates);
        var cooperators = odoo.pullCooperators();
        cooperatorRepo.saveAll(cooperators);
        return new SyncPullCommand.Result(templates.size(), cooperators.size());
    }
}
