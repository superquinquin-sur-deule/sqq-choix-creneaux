package fr.sqq.choixcreneaux.acceptance.fixtures;

import fr.sqq.choixcreneaux.application.port.out.CooperatorRepository;
import fr.sqq.choixcreneaux.application.port.out.SlotRepository;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.Slot;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import fr.sqq.choixcreneaux.domain.model.SlotStatus;
import fr.sqq.choixcreneaux.domain.model.Week;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class TestFixtures {

    @Inject SlotRepository slotRepo;
    @Inject CooperatorRepository cooperatorRepo;
    @Inject EntityManager em;

    @Transactional
    public void cleanAll() {
        em.createNativeQuery("DELETE FROM email_log").executeUpdate();
        em.createNativeQuery("DELETE FROM slot_registration").executeUpdate();
        em.createNativeQuery("DELETE FROM slot_template").executeUpdate();
        em.createNativeQuery("DELETE FROM cooperator").executeUpdate();
    }

    @Transactional
    public UUID seedSlot(Week week, DayOfWeek day, LocalTime start, LocalTime end,
                         int min, int max, int preRegistrations) {
        UUID id = UUID.randomUUID();
        List<SlotRegistration> regs = new ArrayList<>();
        for (int i = 0; i < preRegistrations; i++) {
            UUID coopId = UUID.randomUUID();
            String shortId = coopId.toString().substring(0, 8);
            seedCooperator(coopId, shortId + "@ex.fr", "Seed" + i, "Reg" + i, "S" + shortId);
            regs.add(new SlotRegistration(UUID.randomUUID(), id, coopId, Instant.now()));
        }
        SlotStatus status = regs.size() >= max ? SlotStatus.FULL
                : regs.size() < min ? SlotStatus.NEEDS_PEOPLE : SlotStatus.OPEN;
        Slot slot = Slot.rehydrate(id, week, day, start, end, min, max, null, 0, regs, status);
        slotRepo.save(slot);
        return id;
    }

    public UUID seedSlot(Week week, DayOfWeek day, int min, int max, int preRegistrations) {
        return seedSlot(week, day, LocalTime.of(8, 15), LocalTime.of(11, 0), min, max, preRegistrations);
    }

    @Transactional
    public UUID seedCooperator(String barcodeBase, String firstName, String lastName) {
        UUID id = UUID.randomUUID();
        seedCooperator(id, firstName.toLowerCase() + "+" + barcodeBase + "@ex.fr",
                firstName, lastName, barcodeBase);
        return id;
    }

    @Transactional
    public void seedCooperator(UUID id, String email, String firstName, String lastName, String barcodeBase) {
        cooperatorRepo.saveAll(List.of(
                new Cooperator(id, email, firstName, lastName, null, barcodeBase)
        ));
    }
}
