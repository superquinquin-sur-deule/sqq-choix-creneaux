package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.query.GetPendingCooperatorsPageQuery;
import fr.sqq.choixcreneaux.application.query.PendingCooperatorsPage;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetPendingCooperatorsPageQueryHandler implements QueryHandler<GetPendingCooperatorsPageQuery, PendingCooperatorsPage> {
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public GetPendingCooperatorsPageQueryHandler(CooperatorRepository cooperatorRepo) {
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
