package fr.sqq.choixcreneaux.infrastructure.out.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "campaign")
public class CampaignEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(nullable = false) public String status;
    @Column(name = "start_date", nullable = false) public Instant startDate;
    @Column(name = "end_date") public Instant endDate;
    @Column(name = "store_opening", nullable = false) public LocalDate storeOpening;
    @Column(name = "week_a_reference", nullable = false) public LocalDate weekAReference;
}
