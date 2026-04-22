package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import java.util.*;
public interface SlotTemplateFinder {
    List<SlotTemplate> findAll();
    Optional<SlotTemplate> findById(UUID id);
}
