package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.application.query.GetMyRegistrationQuery;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetMyRegistrationQueryHandler implements QueryHandler<GetMyRegistrationQuery, GetMyRegistrationQuery.Result> {
    private final CooperatorRepository cooperatorRepo;
    private final SlotRegistrationRepository registrationRepo;

    @Inject
    public GetMyRegistrationQueryHandler(CooperatorRepository cooperatorRepo, SlotRegistrationRepository registrationRepo) {
        this.cooperatorRepo = cooperatorRepo;
        this.registrationRepo = registrationRepo;
    }

    @Override
    public GetMyRegistrationQuery.Result handle(GetMyRegistrationQuery query) {
        var coop = cooperatorRepo.findByBarcodeBase(query.barcodeBase());
        if (coop.isEmpty()) {
            return new GetMyRegistrationQuery.Result(null);
        }
        var registration = registrationRepo.findByCooperatorId(coop.get().id());
        return new GetMyRegistrationQuery.Result(registration.map(r -> r.slotTemplateId()).orElse(null));
    }
}
