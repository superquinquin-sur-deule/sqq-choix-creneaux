package fr.sqq.choixcreneaux.application.port.out;

import fr.sqq.choixcreneaux.domain.model.Slot;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SlotRepository {
    Optional<Slot> findById(UUID id);
    List<Slot> findAll();
    void save(Slot slot);
    void saveAll(List<Slot> slots);
    boolean anyUnderMinimum();
}
