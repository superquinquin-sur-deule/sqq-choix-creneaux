package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

public record GetPendingCooperatorsQuery() implements Query<List<Cooperator>> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetPendingCooperatorsQuery, List<Cooperator>> {
        private final CooperatorRepository cooperatorRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo) {
            this.cooperatorRepo = cooperatorRepo;
        }

        @Override
        public List<Cooperator> handle(GetPendingCooperatorsQuery query) {
            return cooperatorRepo.findWithoutRegistration();
        }
    }
}
