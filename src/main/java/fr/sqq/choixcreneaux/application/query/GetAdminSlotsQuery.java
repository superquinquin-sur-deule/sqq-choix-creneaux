package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.domain.model.AdminSlotView;
import fr.sqq.mediator.Query;

import java.util.List;

public record GetAdminSlotsQuery() implements Query<List<AdminSlotView>> {}
