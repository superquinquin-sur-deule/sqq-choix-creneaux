package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.query.PendingCooperatorsPage;
import fr.sqq.choixcreneaux.application.query.SearchCooperatorsQuery;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SearchCooperatorsQueryHandler implements QueryHandler<SearchCooperatorsQuery, PendingCooperatorsPage> {
    private final CooperatorRepository cooperatorRepo;

    @Inject
    public SearchCooperatorsQueryHandler(CooperatorRepository cooperatorRepo) {
        this.cooperatorRepo = cooperatorRepo;
    }

    @Override
    public PendingCooperatorsPage handle(SearchCooperatorsQuery query) {
        int page = Math.max(1, query.page());
        int size = Math.clamp(query.size(), 1, 100);
        int offset = (page - 1) * size;
        long total = cooperatorRepo.countSearch(query.q());
        var items = cooperatorRepo.search(query.q(), offset, size);
        return new PendingCooperatorsPage(items, total);
    }
}
