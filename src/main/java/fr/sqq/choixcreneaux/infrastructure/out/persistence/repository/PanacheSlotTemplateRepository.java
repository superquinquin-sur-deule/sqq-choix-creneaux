package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.SlotTemplateRepository;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotTemplateEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;

@ApplicationScoped
public class PanacheSlotTemplateRepository implements SlotTemplateRepository {
    @Inject EntityMapper mapper;

    @Override
    public List<SlotTemplate> findAll() {
        return SlotTemplateEntity.<SlotTemplateEntity>findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<SlotTemplate> findById(UUID id) {
        return SlotTemplateEntity.<SlotTemplateEntity>findByIdOptional(id).map(mapper::toDomain);
    }
}
