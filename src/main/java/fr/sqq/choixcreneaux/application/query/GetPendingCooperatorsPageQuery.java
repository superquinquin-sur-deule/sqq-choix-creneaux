package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.EmailLogRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationFinder;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.EmailType;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record GetPendingCooperatorsPageQuery(int page, int size, String q, boolean withoutSlotOnly, boolean withSlotOnly, boolean neverRemindedOnly, boolean exemptedOnly, CooperatorSort sort) implements Query<PendingCooperatorsPage> {

    public GetPendingCooperatorsPageQuery {
        if (sort == null) sort = CooperatorSort.DEFAULT;
    }

    public GetPendingCooperatorsPageQuery(int page, int size, String q, boolean withoutSlotOnly, boolean withSlotOnly, boolean neverRemindedOnly, CooperatorSort sort) {
        this(page, size, q, withoutSlotOnly, withSlotOnly, neverRemindedOnly, false, sort);
    }

    public GetPendingCooperatorsPageQuery(int page, int size) {
        this(page, size, null, true, false, false, false, CooperatorSort.DEFAULT);
    }

    public GetPendingCooperatorsPageQuery(int page, int size, String q) {
        this(page, size, q, true, false, false, false, CooperatorSort.DEFAULT);
    }

    public GetPendingCooperatorsPageQuery(int page, int size, String q, boolean withoutSlotOnly) {
        this(page, size, q, withoutSlotOnly, false, false, false, CooperatorSort.DEFAULT);
    }

    public GetPendingCooperatorsPageQuery(int page, int size, String q, boolean withoutSlotOnly, boolean neverRemindedOnly) {
        this(page, size, q, withoutSlotOnly, false, neverRemindedOnly, false, CooperatorSort.DEFAULT);
    }

    public GetPendingCooperatorsPageQuery(int page, int size, String q, boolean withoutSlotOnly, boolean neverRemindedOnly, CooperatorSort sort) {
        this(page, size, q, withoutSlotOnly, false, neverRemindedOnly, false, sort);
    }

    private boolean hasQuery() {
        return q != null && !q.isBlank();
    }

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetPendingCooperatorsPageQuery, PendingCooperatorsPage> {
        private final CooperatorRepository cooperatorRepo;
        private final EmailLogRepository emailLogRepo;
        private final SlotRegistrationFinder registrationFinder;
        private final SlotRepository slotRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo, EmailLogRepository emailLogRepo,
                       SlotRegistrationFinder registrationFinder, SlotRepository slotRepo) {
            this.cooperatorRepo = cooperatorRepo;
            this.emailLogRepo = emailLogRepo;
            this.registrationFinder = registrationFinder;
            this.slotRepo = slotRepo;
        }

        @Override
        public PendingCooperatorsPage handle(GetPendingCooperatorsPageQuery query) {
            int page = Math.max(1, query.page());
            int size = Math.clamp(query.size(), 1, 100);
            int offset = (page - 1) * size;
            CooperatorSort sort = query.sort() != null ? query.sort() : CooperatorSort.DEFAULT;
            long total;
            java.util.List<Cooperator> items;
            if (query.exemptedOnly()) {
                String q = query.hasQuery() ? query.q() : "";
                total = cooperatorRepo.countSearchExempted(q);
                items = cooperatorRepo.searchExempted(q, offset, size, sort);
            } else if (query.withoutSlotOnly()) {
                String q = query.hasQuery() ? query.q() : "";
                if (query.neverRemindedOnly()) {
                    total = cooperatorRepo.countSearchWithoutRegistrationNeverReminded(q);
                    items = cooperatorRepo.searchWithoutRegistrationNeverReminded(q, offset, size, sort);
                } else if (query.hasQuery()) {
                    total = cooperatorRepo.countSearchWithoutRegistration(q);
                    items = cooperatorRepo.searchWithoutRegistration(q, offset, size, sort);
                } else {
                    total = cooperatorRepo.countWithoutRegistration();
                    items = cooperatorRepo.findWithoutRegistration(offset, size, sort);
                }
            } else if (query.withSlotOnly()) {
                String q = query.hasQuery() ? query.q() : "";
                total = cooperatorRepo.countSearchWithRegistration(q);
                items = cooperatorRepo.searchWithRegistration(q, offset, size, sort);
            } else {
                String q = query.hasQuery() ? query.q() : "";
                total = cooperatorRepo.countSearch(q);
                items = cooperatorRepo.search(q, offset, size, sort);
            }
            var ids = items.stream().map(Cooperator::id).toList();
            var lastReminder = emailLogRepo.findLastSentByCooperators(ids, EmailType.REMINDER);
            Map<UUID, CooperatorSlotSummary> slotByCooperatorId = new HashMap<>();
            for (UUID id : ids) {
                registrationFinder.findByCooperatorId(id).ifPresent(reg ->
                        slotRepo.findById(reg.slotTemplateId()).ifPresent(slot ->
                                slotByCooperatorId.put(id, toSummary(slot))));
            }
            return new PendingCooperatorsPage(items, total, lastReminder, slotByCooperatorId);
        }

        private CooperatorSlotSummary toSummary(Slot slot) {
            return new CooperatorSlotSummary(
                    slot.week().name(), slot.dayOfWeek(), slot.startTime(), slot.endTime());
        }
    }
}
