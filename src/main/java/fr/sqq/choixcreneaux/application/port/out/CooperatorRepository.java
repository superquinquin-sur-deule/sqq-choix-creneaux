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
    List<Cooperator> findWithoutRegistration(int offset, int limit);
    long countWithoutRegistration();
    List<Cooperator> search(String q, int offset, int limit);
    long countSearch(String q);
    long countTotal();
    long countWithRegistration();
    void saveAll(List<Cooperator> cooperators);
}
