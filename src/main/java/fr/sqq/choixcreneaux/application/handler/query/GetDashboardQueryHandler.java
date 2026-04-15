package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.query.GetDashboardQuery;
import fr.sqq.choixcreneaux.application.query.GetSlotsQuery;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.SlotWithFillInfo;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class GetDashboardQueryHandler implements QueryHandler<GetDashboardQuery, GetDashboardQuery.Result> {
    private final CooperatorRepository cooperatorRepo;
    private final GetSlotsQueryHandler slotsHandler;

    @Inject
    public GetDashboardQueryHandler(CooperatorRepository cooperatorRepo, GetSlotsQueryHandler slotsHandler) {
        this.cooperatorRepo = cooperatorRepo;
        this.slotsHandler = slotsHandler;
    }

    @Override
    public GetDashboardQuery.Result handle(GetDashboardQuery query) {
        long total = cooperatorRepo.countTotal();
        long registered = cooperatorRepo.countWithRegistration();
        long pending = total - registered;

        var allSlots = slotsHandler.handle(new GetSlotsQuery());
        List<SlotWithFillInfo> slotsNeedingPeople = allSlots.stream()
                .filter(s -> s.status() == SlotStatus.NEEDS_PEOPLE)
                .sorted(Comparator.comparingDouble(s -> (double) s.registrationCount() / s.slot().minCapacity()))
                .toList();
        boolean allMinimumsReached = slotsNeedingPeople.isEmpty();

        return new GetDashboardQuery.Result(total, registered, pending,
                slotsNeedingPeople.size(), allMinimumsReached, slotsNeedingPeople);
    }
}
