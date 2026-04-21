package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.SlotRegistration;
import java.util.*;
public interface SlotRegistrationRepository {
    Optional<SlotRegistration> findByCooperatorId(UUID cooperatorId);
    List<SlotRegistration> findAll();
}
