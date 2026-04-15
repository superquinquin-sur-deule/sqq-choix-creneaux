package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import java.util.*;
public interface SlotRegistrationRepository {
    Optional<SlotRegistration> findByCooperatorId(UUID cooperatorId);
    SlotRegistration save(UUID slotTemplateId, UUID cooperatorId);
    int countBySlotTemplateId(UUID slotTemplateId);
    List<SlotRegistration> findAll();
}
