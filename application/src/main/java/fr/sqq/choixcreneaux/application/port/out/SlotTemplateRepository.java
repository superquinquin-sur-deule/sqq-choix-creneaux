package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import java.util.*;
public interface SlotTemplateRepository {
    List<SlotTemplate> findAll();
    Optional<SlotTemplate> findById(UUID id);
    Map<UUID, Integer> countRegistrationsPerSlot();
    void saveAll(List<SlotTemplate> templates);
}
