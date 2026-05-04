package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import fr.sqq.choixcreneaux.domain.model.SlotTemplate;
import java.util.List;
public interface OdooSyncPort {
    List<SlotTemplate> pullSlotTemplates();
    List<Cooperator> pullCooperators();
    PushOutcome pushRegistration(long odooPartnerId, long odooTemplateId);
}
