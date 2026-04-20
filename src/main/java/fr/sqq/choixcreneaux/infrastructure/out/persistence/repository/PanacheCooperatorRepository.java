package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.CooperatorEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;

@ApplicationScoped
public class PanacheCooperatorRepository implements CooperatorRepository {
    @Inject EntityMapper mapper;
    @Inject EntityManager em;

    @Override
    public Optional<Cooperator> findByKeycloakSubject(String subject) {
        return CooperatorEntity.<CooperatorEntity>find("keycloakSubject", subject)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public Optional<Cooperator> findByEmail(String email) {
        return CooperatorEntity.<CooperatorEntity>find("email", email)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void linkKeycloakSubject(UUID cooperatorId, String keycloakSubject) {
        CooperatorEntity.<CooperatorEntity>findByIdOptional(cooperatorId).ifPresent(entity -> {
            entity.keycloakSubject = keycloakSubject;
            entity.persist();
        });
    }

    @Override
    public Optional<Cooperator> findById(UUID id) {
        return CooperatorEntity.<CooperatorEntity>findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    public List<Cooperator> findAllById(Collection<UUID> ids) {
        if (ids.isEmpty()) return List.of();
        return CooperatorEntity.<CooperatorEntity>list("id in ?1", ids).stream()
                .map(mapper::toDomain).toList();
    }

    private static final String WITHOUT_REGISTRATION_SQL =
            "SELECT c.* FROM cooperator c LEFT JOIN slot_registration sr ON c.id = sr.cooperator_id WHERE sr.id IS NULL ORDER BY c.last_name, c.first_name";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> findWithoutRegistration() {
        return em.createNativeQuery(WITHOUT_REGISTRATION_SQL, CooperatorEntity.class)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> findWithoutRegistration(int offset, int limit) {
        return em.createNativeQuery(WITHOUT_REGISTRATION_SQL, CooperatorEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countWithoutRegistration() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM cooperator c LEFT JOIN slot_registration sr ON c.id = sr.cooperator_id WHERE sr.id IS NULL")
                .getSingleResult()).longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> search(String q, int offset, int limit) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return em.createNativeQuery(
                "SELECT * FROM cooperator WHERE LOWER(first_name) LIKE ?1 OR LOWER(last_name) LIKE ?1 OR LOWER(email) LIKE ?1 ORDER BY last_name, first_name",
                CooperatorEntity.class)
                .setParameter(1, pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countSearch(String q) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*) FROM cooperator WHERE LOWER(first_name) LIKE ?1 OR LOWER(last_name) LIKE ?1 OR LOWER(email) LIKE ?1")
                .setParameter(1, pattern)
                .getSingleResult()).longValue();
    }

    @Override
    public long countTotal() { return CooperatorEntity.count(); }

    @Override
    public long countWithRegistration() {
        return ((Number) em.createNativeQuery("SELECT COUNT(DISTINCT cooperator_id) FROM slot_registration")
                .getSingleResult()).longValue();
    }

    @Override
    @Transactional
    public void saveAll(List<Cooperator> cooperators) {
        for (var c : cooperators) {
            var entity = new CooperatorEntity();
            entity.id = c.id() != null ? c.id() : UUID.randomUUID();
            entity.email = c.email();
            entity.firstName = c.firstName();
            entity.lastName = c.lastName();
            entity.odooPartnerId = c.odooPartnerId();
            entity.keycloakSubject = c.keycloakSubject();
            entity.persist();
        }
    }
}
