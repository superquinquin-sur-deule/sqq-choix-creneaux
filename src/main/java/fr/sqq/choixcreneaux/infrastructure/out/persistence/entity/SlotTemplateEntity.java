package fr.sqq.choixcreneaux.infrastructure.out.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "slot_template")
public class SlotTemplateEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(nullable = false, length = 1) public String week;
    @Column(name = "day_of_week", nullable = false) public String dayOfWeek;
    @Column(name = "start_time", nullable = false) public LocalTime startTime;
    @Column(name = "end_time", nullable = false) public LocalTime endTime;
    @Column(name = "min_capacity", nullable = false) public int minCapacity;
    @Column(name = "max_capacity", nullable = false) public int maxCapacity;
    @Column(name = "odoo_template_id") public Long odooTemplateId;
    @Column(nullable = false, length = 20) public String status;
    @Version public int version;
}
