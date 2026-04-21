package fr.sqq.choixcreneaux.infrastructure.out.persistence.repository;

import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import fr.sqq.choixcreneaux.domain.model.Week;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotRegistrationEntity;
import fr.sqq.choixcreneaux.infrastructure.out.persistence.entity.SlotTemplateEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.time.DayOfWeek;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class PanacheSlotRepository implements SlotRepository {

    @Inject
    EntityManager em;

    @Override
    public Optional<Slot> findById(UUID id) {
        SlotTemplateEntity entity = SlotTemplateEntity.findById(id);
        if (entity == null) return Optional.empty();
        return Optional.of(toAggregate(entity, loadRegistrations(id)));
    }

    @Override
    public List<Slot> findAll() {
        List<SlotTemplateEntity> entities = SlotTemplateEntity.listAll();
        List<SlotRegistrationEntity> allRegs = SlotRegistrationEntity.listAll();
        return entities.stream().map(entity -> {
            List<SlotRegistration> regs = allRegs.stream()
                    .filter(r -> r.slotTemplateId.equals(entity.id))
                    .map(this::toDomain)
                    .toList();
            return toAggregate(entity, regs);
        }).toList();
    }

    @Override
    public void save(Slot slot) {
        SlotTemplateEntity entity = SlotTemplateEntity.findById(slot.id());
        if (entity == null) {
            entity = new SlotTemplateEntity();
            entity.id = slot.id();
            copyTemplateFields(slot, entity);
            entity.persist();
        } else {
            copyTemplateFields(slot, entity);
            em.lock(entity, LockModeType.OPTIMISTIC_FORCE_INCREMENT);
        }

        reconcileRegistrations(slot);
    }

    @Override
    public void saveAll(List<Slot> slots) {
        for (Slot slot : slots) {
            save(slot);
        }
    }

    private void reconcileRegistrations(Slot slot) {
        List<SlotRegistrationEntity> existing = SlotRegistrationEntity.list("slotTemplateId", slot.id());
        Set<UUID> aggCoops = new HashSet<>();
        for (SlotRegistration r : slot.registrations()) aggCoops.add(r.cooperatorId());
        Set<UUID> dbCoops = new HashSet<>();
        for (SlotRegistrationEntity e : existing) dbCoops.add(e.cooperatorId);

        for (SlotRegistrationEntity e : existing) {
            if (!aggCoops.contains(e.cooperatorId)) e.delete();
        }
        for (SlotRegistration r : slot.registrations()) {
            if (dbCoops.contains(r.cooperatorId())) continue;
            SlotRegistrationEntity e = new SlotRegistrationEntity();
            e.id = r.id();
            e.slotTemplateId = r.slotTemplateId();
            e.cooperatorId = r.cooperatorId();
            e.registeredAt = r.registeredAt();
            e.persist();
        }
    }

    private void copyTemplateFields(Slot slot, SlotTemplateEntity entity) {
        entity.week = slot.week().name();
        entity.dayOfWeek = slot.dayOfWeek().name();
        entity.startTime = slot.startTime();
        entity.endTime = slot.endTime();
        entity.minCapacity = slot.minCapacity();
        entity.maxCapacity = slot.maxCapacity();
        entity.odooTemplateId = slot.odooTemplateId();
    }

    private List<SlotRegistration> loadRegistrations(UUID slotId) {
        return SlotRegistrationEntity.<SlotRegistrationEntity>list("slotTemplateId", slotId)
                .stream().map(this::toDomain).toList();
    }

    private SlotRegistration toDomain(SlotRegistrationEntity entity) {
        return new SlotRegistration(entity.id, entity.slotTemplateId, entity.cooperatorId, entity.registeredAt);
    }

    private Slot toAggregate(SlotTemplateEntity entity, List<SlotRegistration> registrations) {
        return Slot.rehydrate(
                entity.id,
                Week.valueOf(entity.week),
                DayOfWeek.valueOf(entity.dayOfWeek),
                entity.startTime,
                entity.endTime,
                entity.minCapacity,
                entity.maxCapacity,
                entity.odooTemplateId,
                entity.version,
                registrations
        );
    }
}
