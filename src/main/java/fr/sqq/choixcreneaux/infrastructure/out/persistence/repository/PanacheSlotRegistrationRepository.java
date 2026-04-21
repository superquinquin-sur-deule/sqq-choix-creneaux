package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.SlotRegistrationRepository;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotRegistrationEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.mapper.EntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
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
    public List<SlotRegistration> findAll() {
        return SlotRegistrationEntity.<SlotRegistrationEntity>findAll().stream()
                .map(mapper::toDomain).toList();
    }
}
