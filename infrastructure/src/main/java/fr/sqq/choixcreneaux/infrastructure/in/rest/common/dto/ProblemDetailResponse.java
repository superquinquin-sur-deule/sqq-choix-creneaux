package fr.sqq.choixcreneaux.infrastructure.in.rest.common.dto;

public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail
) {
}
