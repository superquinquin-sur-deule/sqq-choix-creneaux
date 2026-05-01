package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.query.CooperatorSort;
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

    private static final String LAST_REMINDER_JOIN =
            " LEFT JOIN (SELECT cooperator_id, MAX(sent_at) AS last_reminder FROM email_log" +
            " WHERE type = 'REMINDER' GROUP BY cooperator_id) lr ON lr.cooperator_id = c.id";

    private static String reminderJoin(CooperatorSort sort) {
        return sort.field() == CooperatorSort.Field.LAST_REMINDER ? LAST_REMINDER_JOIN : "";
    }

    private static String orderBy(CooperatorSort sort) {
        String dir = sort.direction() == CooperatorSort.Direction.DESC ? "DESC" : "ASC";
        return switch (sort.field()) {
            case NAME -> " ORDER BY c.last_name " + dir + ", c.first_name " + dir;
            case EMAIL -> " ORDER BY c.email " + dir + ", c.last_name ASC, c.first_name ASC";
            case LAST_REMINDER ->
                    " ORDER BY lr.last_reminder " + dir + " NULLS LAST, c.last_name ASC, c.first_name ASC";
        };
    }

    @Override
    public Optional<Cooperator> findByBarcodeBase(String barcodeBase) {
        return CooperatorEntity.<CooperatorEntity>find("barcodeBase", barcodeBase)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public Optional<Cooperator> findByEmail(String email) {
        return CooperatorEntity.<CooperatorEntity>find("email", email)
                .firstResultOptional().map(mapper::toDomain);
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

    private static final String WITHOUT_REGISTRATION_FROM =
            " FROM cooperator c LEFT JOIN slot_registration sr ON c.id = sr.cooperator_id";
    private static final String WITHOUT_REGISTRATION_WHERE = " WHERE sr.id IS NULL";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> findWithoutRegistration() {
        return em.createNativeQuery(
                "SELECT c.*" + WITHOUT_REGISTRATION_FROM + WITHOUT_REGISTRATION_WHERE
                        + orderBy(CooperatorSort.DEFAULT),
                CooperatorEntity.class)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> findWithoutRegistration(int offset, int limit, CooperatorSort sort) {
        return em.createNativeQuery(
                "SELECT c.*" + WITHOUT_REGISTRATION_FROM + reminderJoin(sort)
                        + WITHOUT_REGISTRATION_WHERE + orderBy(sort),
                CooperatorEntity.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countWithoutRegistration() {
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*)" + WITHOUT_REGISTRATION_FROM + WITHOUT_REGISTRATION_WHERE)
                .getSingleResult()).longValue();
    }

    private static final String SEARCH_WITHOUT_REGISTRATION_WHERE =
            " WHERE sr.id IS NULL" +
            " AND (LOWER(c.first_name) LIKE ?1 OR LOWER(c.last_name) LIKE ?1 OR LOWER(c.email) LIKE ?1)";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> searchWithoutRegistration(String q, int offset, int limit, CooperatorSort sort) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return em.createNativeQuery(
                "SELECT c.*" + WITHOUT_REGISTRATION_FROM + reminderJoin(sort)
                        + SEARCH_WITHOUT_REGISTRATION_WHERE + orderBy(sort),
                CooperatorEntity.class)
                .setParameter(1, pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countSearchWithoutRegistration(String q) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*)" + WITHOUT_REGISTRATION_FROM + SEARCH_WITHOUT_REGISTRATION_WHERE)
                .setParameter(1, pattern)
                .getSingleResult()).longValue();
    }

    private static final String NEVER_REMINDED_FROM_WHERE =
            " FROM cooperator c" +
            " LEFT JOIN slot_registration sr ON c.id = sr.cooperator_id" +
            " LEFT JOIN email_log el ON el.cooperator_id = c.id AND el.type = 'REMINDER'" +
            " WHERE sr.id IS NULL" +
            " AND el.id IS NULL" +
            " AND (LOWER(c.first_name) LIKE ?1 OR LOWER(c.last_name) LIKE ?1 OR LOWER(c.email) LIKE ?1)";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> searchWithoutRegistrationNeverReminded(String q, int offset, int limit, CooperatorSort sort) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        // Never-reminded → last_reminder is always NULL; ignore that sort field by falling back to NAME.
        CooperatorSort effective = sort.field() == CooperatorSort.Field.LAST_REMINDER
                ? new CooperatorSort(CooperatorSort.Field.NAME, sort.direction())
                : sort;
        return em.createNativeQuery(
                "SELECT DISTINCT c.*" + NEVER_REMINDED_FROM_WHERE + orderBy(effective),
                CooperatorEntity.class)
                .setParameter(1, pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countSearchWithoutRegistrationNeverReminded(String q) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(DISTINCT c.id)" + NEVER_REMINDED_FROM_WHERE)
                .setParameter(1, pattern)
                .getSingleResult()).longValue();
    }

    private static final String SEARCH_FROM = " FROM cooperator c";
    private static final String SEARCH_WHERE =
            " WHERE LOWER(c.first_name) LIKE ?1 OR LOWER(c.last_name) LIKE ?1 OR LOWER(c.email) LIKE ?1";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> search(String q, int offset, int limit, CooperatorSort sort) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return em.createNativeQuery(
                "SELECT c.*" + SEARCH_FROM + reminderJoin(sort) + SEARCH_WHERE + orderBy(sort),
                CooperatorEntity.class)
                .setParameter(1, pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    private static final String WITH_REGISTRATION_WHERE_BASE =
            " WHERE EXISTS (SELECT 1 FROM slot_registration sr WHERE sr.cooperator_id = c.id)";
    private static final String SEARCH_WITH_REGISTRATION_WHERE =
            WITH_REGISTRATION_WHERE_BASE +
            " AND (LOWER(c.first_name) LIKE ?1 OR LOWER(c.last_name) LIKE ?1 OR LOWER(c.email) LIKE ?1)";

    @Override
    @SuppressWarnings("unchecked")
    public List<Cooperator> searchWithRegistration(String q, int offset, int limit, CooperatorSort sort) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return em.createNativeQuery(
                "SELECT c.*" + SEARCH_FROM + reminderJoin(sort)
                        + SEARCH_WITH_REGISTRATION_WHERE + orderBy(sort),
                CooperatorEntity.class)
                .setParameter(1, pattern)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList().stream()
                .map(e -> mapper.toDomain((CooperatorEntity) e)).toList();
    }

    @Override
    public long countSearchWithRegistration(String q) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*)" + SEARCH_FROM + SEARCH_WITH_REGISTRATION_WHERE)
                .setParameter(1, pattern)
                .getSingleResult()).longValue();
    }

    @Override
    public long countSearch(String q) {
        String pattern = "%" + (q == null ? "" : q.trim().toLowerCase()) + "%";
        return ((Number) em.createNativeQuery(
                "SELECT COUNT(*)" + SEARCH_FROM + SEARCH_WHERE)
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
            CooperatorEntity entity = null;
            if (c.odooPartnerId() != null) {
                entity = CooperatorEntity.<CooperatorEntity>find("odooPartnerId", c.odooPartnerId())
                        .firstResult();
            }
            if (entity == null && c.email() != null && !c.email().isBlank()) {
                entity = CooperatorEntity.<CooperatorEntity>find("email", c.email()).firstResult();
            }
            if (entity == null) {
                entity = new CooperatorEntity();
                entity.id = c.id() != null ? c.id() : UUID.randomUUID();
            }
            entity.email = c.email();
            entity.firstName = c.firstName();
            entity.lastName = c.lastName();
            if (c.odooPartnerId() != null) entity.odooPartnerId = c.odooPartnerId();
            entity.barcodeBase = c.barcodeBase();
            entity.persist();
        }
    }
}
