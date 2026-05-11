package fr.sqq.choixcreneaux.infrastructure.out.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "cooperator")
public class CooperatorEntity extends PanacheEntityBase {
    @Id public UUID id;
    @Column(nullable = false, unique = true) public String email;
    @Column(name = "first_name", nullable = false) public String firstName;
    @Column(name = "last_name", nullable = false) public String lastName;
    @Column(name = "odoo_partner_id", unique = true) public Long odooPartnerId;
    @Column(name = "barcode_base", nullable = false, unique = true) public String barcodeBase;
    @Column(name = "exemption_reason") public String exemptionReason;
}
