package fr.sqq.choixcreneaux.acceptance.common.middleware;

import fr.sqq.choixcreneaux.domain.exception.AlreadyRegisteredException;
import fr.sqq.choixcreneaux.domain.exception.CampaignNotOpenException;
import fr.sqq.choixcreneaux.domain.exception.DomainException;
import fr.sqq.choixcreneaux.domain.exception.SlotFullException;
import fr.sqq.choixcreneaux.domain.exception.SlotLockedException;
import fr.sqq.choixcreneaux.acceptance.common.dto.ProblemDetailResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @Override
    public Response toResponse(DomainException e) {
        int status = switch (e) {
            case AlreadyRegisteredException ex -> 409;
            case SlotLockedException ex -> 423;
            case SlotFullException ex -> 409;
            case CampaignNotOpenException ex -> 403;
            default -> 400;
        };
        return buildResponse("about:blank", "Domain Error", status, e.getMessage());
    }

    private Response buildResponse(String type, String title, int status, String detail) {
        var problem = new ProblemDetailResponse(type, title, status, detail);
        return Response.status(status)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}
