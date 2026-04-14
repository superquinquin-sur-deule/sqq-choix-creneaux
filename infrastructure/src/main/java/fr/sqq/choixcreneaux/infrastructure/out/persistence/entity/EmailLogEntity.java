package fr.sqq.choixcreneaux.infrastructure.out.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "email_log")
public class EmailLogEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "cooperator_id", nullable = false) public UUID cooperatorId;
    @Column(nullable = false) public String type;
    @Column(name = "sent_at", nullable = false) public Instant sentAt;
}
