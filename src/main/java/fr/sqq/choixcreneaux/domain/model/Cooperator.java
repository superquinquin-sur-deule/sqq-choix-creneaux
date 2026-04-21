package fr.sqq.choixcreneaux.domain.model;

import java.util.UUID;

public record Cooperator(
    UUID id, String email, String firstName, String lastName,
    Long odooPartnerId, String barcodeBase
) {}
