package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.query.GetPendingCooperatorsQuery;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;

@ApplicationScoped
public class GetPendingCooperatorsQueryHandler implements QueryHandler<GetPendingCooperatorsQuery, List<Cooperator>> {
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public GetPendingCooperatorsQueryHandler(CooperatorRepository cooperatorRepo) {
        this.cooperatorRepo = cooperatorRepo;
    }

    @Override
    public List<Cooperator> handle(GetPendingCooperatorsQuery query) {
        return cooperatorRepo.findWithoutRegistration();
    }
}
