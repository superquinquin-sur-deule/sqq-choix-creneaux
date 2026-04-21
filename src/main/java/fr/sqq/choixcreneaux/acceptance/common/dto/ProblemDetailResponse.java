package fr.sqq.choixcreneaux.acceptance.common.dto;

public record ProblemDetailResponse(
        String type,
        String title,
        int status,
        String detail
) {
}
