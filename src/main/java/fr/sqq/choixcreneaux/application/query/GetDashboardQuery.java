package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.SlotWithFillInfo;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;

public record GetDashboardQuery() implements Query<GetDashboardQuery.Result> {
    public record Result(long totalCooperators, long registeredCooperators, long pendingCooperators,
                         int slotsUnderMinimum, boolean allMinimumsReached, List<SlotWithFillInfo> slotsNeedingPeople) {}

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetDashboardQuery, Result> {
        private final CooperatorRepository cooperatorRepo;
        private final GetSlotsQuery.Handler slotsHandler;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo, GetSlotsQuery.Handler slotsHandler) {
            this.cooperatorRepo = cooperatorRepo;
            this.slotsHandler = slotsHandler;
        }

        @Override
        public Result handle(GetDashboardQuery query) {
            long total = cooperatorRepo.countTotal();
            long registered = cooperatorRepo.countWithRegistration();
            long pending = total - registered;

            var allSlots = slotsHandler.handle(new GetSlotsQuery());
            List<SlotWithFillInfo> slotsNeedingPeople = allSlots.stream()
                    .filter(s -> s.status() == SlotStatus.NEEDS_PEOPLE)
                    .sorted(Comparator.comparingDouble(s -> (double) s.registrationCount() / s.slot().minCapacity()))
                    .toList();
            boolean allMinimumsReached = slotsNeedingPeople.isEmpty();

            return new Result(total, registered, pending,
                    slotsNeedingPeople.size(), allMinimumsReached, slotsNeedingPeople);
        }
    }
}
