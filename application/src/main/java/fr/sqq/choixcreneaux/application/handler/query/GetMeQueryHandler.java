package fr.sqq.choixcreneaux.application.handler.query;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.application.query.GetMeQuery;
import fr.sqq.mediator.QueryHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetMeQueryHandler implements QueryHandler<GetMeQuery, GetMeQuery.Result> {
    private final CooperatorRepository cooperatorRepo;
    private final SlotRegistrationRepository registrationRepo;

    @Inject
    public GetMeQueryHandler(CooperatorRepository cooperatorRepo, SlotRegistrationRepository registrationRepo) {
        this.cooperatorRepo = cooperatorRepo;
        this.registrationRepo = registrationRepo;
    }

    @Override
    public GetMeQuery.Result handle(GetMeQuery query) {
        // Try by keycloak subject first
        var cooperator = cooperatorRepo.findByKeycloakSubject(query.keycloakSubject());

        // Fallback: find by email and link the keycloak subject
        if (cooperator.isEmpty() && query.email() != null && !query.email().isBlank()) {
            cooperator = cooperatorRepo.findByEmail(query.email());
            cooperator.ifPresent(c -> cooperatorRepo.linkKeycloakSubject(c.id(), query.keycloakSubject()));
        }

        var coop = cooperator.orElseThrow(() ->
                new RuntimeException("Cooperator not found for subject: " + query.keycloakSubject() + " or email: " + query.email()));

        var registration = registrationRepo.findByCooperatorId(coop.id());
        return new GetMeQuery.Result(coop.id(), coop.email(), coop.firstName(),
                coop.lastName(), registration.map(r -> r.slotTemplateId()).orElse(null));
    }
}
