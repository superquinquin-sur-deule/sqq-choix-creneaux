package fr.sqq.choixcreneaux.application.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.mediator.Query;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

public record GetPendingCooperatorsPageQuery(int page, int size) implements Query<PendingCooperatorsPage> {

    @ApplicationScoped
    public static class Handler implements QueryHandler<GetPendingCooperatorsPageQuery, PendingCooperatorsPage> {
        private final CooperatorRepository cooperatorRepo;

        @Inject
        public Handler(CooperatorRepository cooperatorRepo) {
            this.cooperatorRepo = cooperatorRepo;
        }

        @Override
        public PendingCooperatorsPage handle(GetPendingCooperatorsPageQuery query) {
            int page = Math.max(1, query.page());
            int size = Math.clamp(query.size(), 1, 100);
            int offset = (page - 1) * size;
            long total = cooperatorRepo.countWithoutRegistration();
            var items = cooperatorRepo.findWithoutRegistration(offset, size);
            return new PendingCooperatorsPage(items, total);
        }
    }
}
