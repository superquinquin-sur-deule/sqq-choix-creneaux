package fr.sqq.choixcreneaux.application.query;
import fr.sqq.choixcreneaux.domain.model.SlotWithFillInfo;
import fr.sqq.mediator.Query;
import java.util.List;
public record GetSlotsQuery() implements Query<List<SlotWithFillInfo>> {}
