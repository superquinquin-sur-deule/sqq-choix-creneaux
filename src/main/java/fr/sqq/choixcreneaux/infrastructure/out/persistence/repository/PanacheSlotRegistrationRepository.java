package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotRegistrationEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class PanacheSlotRegistrationRepository implements SlotRegistrationRepository {
    @Inject EntityMapper mapper;

    @Override
    public Optional<SlotRegistration> findByCooperatorId(UUID cooperatorId) {
        return SlotRegistrationEntity.<SlotRegistrationEntity>find("cooperatorId", cooperatorId)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public SlotRegistration save(UUID slotTemplateId, UUID cooperatorId) {
        var entity = new SlotRegistrationEntity();
        entity.id = UUID.randomUUID();
        entity.slotTemplateId = slotTemplateId;
        entity.cooperatorId = cooperatorId;
        entity.registeredAt = Instant.now();
        entity.persist();
        return mapper.toDomain(entity);
    }

    @Override
    public int countBySlotTemplateId(UUID slotTemplateId) {
        return (int) SlotRegistrationEntity.count("slotTemplateId", slotTemplateId);
    }

    @Override
    public List<SlotRegistration> findAll() {
        return SlotRegistrationEntity.<SlotRegistrationEntity>findAll().stream()
                .map(mapper::toDomain).toList();
    }
}
