package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotTemplateEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.*;

@ApplicationScoped
public class PanacheSlotTemplateRepository implements SlotTemplateRepository {
    @Inject EntityMapper mapper;
    @Inject EntityManager em;

    @Override
    public List<SlotTemplate> findAll() {
        return SlotTemplateEntity.<SlotTemplateEntity>findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SlotTemplate> findById(UUID id) {
        return SlotTemplateEntity.<SlotTemplateEntity>findByIdOptional(id).map(mapper::toDomain);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<UUID, Integer> countRegistrationsPerSlot() {
        List<Object[]> results = em.createNativeQuery(
                "SELECT slot_template_id, COUNT(*) FROM slot_registration GROUP BY slot_template_id")
                .getResultList();
        Map<UUID, Integer> counts = new HashMap<>();
        for (Object[] row : results) {
            counts.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return counts;
    }

    @Override
    @Transactional
    public void saveAll(List<SlotTemplate> templates) {
        for (var t : templates) {
            var entity = new SlotTemplateEntity();
            entity.id = t.id() != null ? t.id() : UUID.randomUUID();
            entity.week = t.week().name();
            entity.dayOfWeek = t.dayOfWeek().name();
            entity.startTime = t.startTime();
            entity.endTime = t.endTime();
            entity.minCapacity = t.minCapacity();
            entity.maxCapacity = t.maxCapacity();
            entity.odooTemplateId = t.odooTemplateId();
            entity.persist();
        }
    }
}
