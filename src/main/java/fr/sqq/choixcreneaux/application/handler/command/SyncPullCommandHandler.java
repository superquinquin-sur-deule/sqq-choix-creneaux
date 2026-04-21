package fr.sqq.choixcreneaux.application.handler.command;

import fr.sqq.choixcreneaux.application.command.SyncPullCommand;
import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.mediator.CommandHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

@ApplicationScoped
public class SyncPullCommandHandler implements CommandHandler<SyncPullCommand, SyncPullCommand.Result> {
    private final OdooSyncPort odoo;
    private final SlotRepository slotRepo;
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public SyncPullCommandHandler(OdooSyncPort odoo, SlotRepository slotRepo, CooperatorRepository cooperatorRepo) {
        this.odoo = odoo;
        this.slotRepo = slotRepo;
        this.cooperatorRepo = cooperatorRepo;
    }

    @Override
    @Transactional
    public SyncPullCommand.Result handle(SyncPullCommand command) {
        var templates = odoo.pullSlotTemplates();
        List<Slot> slots = templates.stream().map(this::toAggregate).toList();
        slotRepo.saveAll(slots);
        var cooperators = odoo.pullCooperators();
        cooperatorRepo.saveAll(cooperators);
        return new SyncPullCommand.Result(templates.size(), cooperators.size());
    }

    private Slot toAggregate(SlotTemplate t) {
        return Slot.create(t.id(), t.week(), t.dayOfWeek(), t.startTime(), t.endTime(),
                t.minCapacity(), t.maxCapacity(), t.odooTemplateId());
    }
}
