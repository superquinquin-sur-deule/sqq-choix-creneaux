package fr.sqq.choixcreneaux.infrastructure.out.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "slot_registration")
public class SlotRegistrationEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(name = "slot_template_id", nullable = false) public UUID slotTemplateId;
    @Column(name = "cooperator_id", nullable = false, unique = true) public UUID cooperatorId;
    @Column(name = "registered_at", nullable = false) public Instant registeredAt;
}
