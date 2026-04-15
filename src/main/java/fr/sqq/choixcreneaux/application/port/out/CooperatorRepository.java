package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import java.util.*;
public interface CooperatorRepository {
    Optional<Cooperator> findByKeycloakSubject(String subject);
    Optional<Cooperator> findByEmail(String email);
    void linkKeycloakSubject(UUID cooperatorId, String keycloakSubject);
    Optional<Cooperator> findById(UUID id);
    List<Cooperator> findAllById(Collection<UUID> ids);
    List<Cooperator> findWithoutRegistration();
    long countTotal();
    long countWithRegistration();
    void saveAll(List<Cooperator> cooperators);
}
