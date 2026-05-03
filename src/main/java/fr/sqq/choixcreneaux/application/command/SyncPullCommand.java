package fr.sqq.choixcreneaux.application.command;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.OdooSyncPort;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.mediator.Command;
import fr.sqq.mediator.CommandHandler;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

public record SyncPullCommand() implements Command<SyncPullCommand.Result> {
    public record Result(int slotsImported, int cooperatorsImported) {}

    @ApplicationScoped
    public static class Handler implements CommandHandler<SyncPullCommand, Result> {
        private final OdooSyncPort odoo;
        private final SlotRepository slotRepo;
        private final CooperatorRepository cooperatorRepo;

        @Inject
        public Handler(OdooSyncPort odoo, SlotRepository slotRepo, CooperatorRepository cooperatorRepo) {
            this.odoo = odoo;
            this.slotRepo = slotRepo;
            this.cooperatorRepo = cooperatorRepo;
        }

        @Override
        @Transactional
        public Result handle(SyncPullCommand command) {
            Log.info("SyncPullCommand: pulling slot templates and cooperators from Odoo");
            var templates = odoo.pullSlotTemplates();
            List<Slot> slots = templates.stream().map(this::mergeTemplate).toList();
            slotRepo.saveAll(slots);
            var cooperators = odoo.pullCooperators();
            cooperatorRepo.saveAll(cooperators);
            return new Result(templates.size(), cooperators.size());
        }

        private Slot mergeTemplate(SlotTemplate t) {
            if (t.odooTemplateId() != null) {
                var existing = slotRepo.findByOdooTemplateId(t.odooTemplateId());
                if (existing.isPresent()) {
                    Slot slot = existing.get();
                    slot.updateFromTemplate(t.week(), t.dayOfWeek(), t.startTime(), t.endTime(),
                            t.minCapacity(), t.maxCapacity());
                    return slot;
                }
            }
            return Slot.create(t.id(), t.week(), t.dayOfWeek(), t.startTime(), t.endTime(),
                    t.minCapacity(), t.maxCapacity(), t.odooTemplateId());
        }
    }
}
