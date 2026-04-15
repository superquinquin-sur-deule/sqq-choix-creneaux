package fr.sqq.choixcreneaux.application.query;
import fr.sqq.choixcreneaux.domain.model.SlotWithFillInfo;
import fr.sqq.mediator.Query;
import java.util.List;
public record GetDashboardQuery() implements Query<GetDashboardQuery.Result> {
    public record Result(long totalCooperators, long registeredCooperators, long pendingCooperators,
                         int slotsUnderMinimum, boolean allMinimumsReached, List<SlotWithFillInfo> slotsNeedingPeople) {}
}
