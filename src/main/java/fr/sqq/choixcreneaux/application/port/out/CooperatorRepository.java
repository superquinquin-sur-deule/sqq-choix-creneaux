package fr.sqq.choixcreneaux.application.port.out;
import fr.sqq.choixcreneaux.domain.model.Cooperator;
import java.util.*;
public interface CooperatorRepository {
    Optional<Cooperator> findByBarcodeBase(String barcodeBase);
    Optional<Cooperator> findByEmail(String email);
    Optional<Cooperator> findById(UUID id);
    List<Cooperator> findAllById(Collection<UUID> ids);
    List<Cooperator> findWithoutRegistration();
    List<Cooperator> findWithoutRegistration(int offset, int limit);
    long countWithoutRegistration();
    List<Cooperator> searchWithoutRegistration(String q, int offset, int limit);
    long countSearchWithoutRegistration(String q);
    List<Cooperator> searchWithoutRegistrationNeverReminded(String q, int offset, int limit);
    long countSearchWithoutRegistrationNeverReminded(String q);
    List<Cooperator> search(String q, int offset, int limit);
    long countSearch(String q);
    long countTotal();
    long countWithRegistration();
    void saveAll(List<Cooperator> cooperators);
}
