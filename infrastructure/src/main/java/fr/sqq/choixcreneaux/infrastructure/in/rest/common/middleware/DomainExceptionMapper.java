package fr.sqq.choixcreneaux.infrastructure.in.rest.common.middleware;

import fr.sqq.choixcreneaux.domain.exception.DomainException;
import fr.sqq.choixcreneaux.infrastructure.in.rest.common.dto.ProblemDetailResponse;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final MediaType PROBLEM_JSON = MediaType.valueOf("application/problem+json");

    @Override
    public Response toResponse(DomainException e) {
        return buildResponse("about:blank", "Domain Error", 400, e.getMessage());
    }

    private Response buildResponse(String type, String title, int status, String detail) {
        var problem = new ProblemDetailResponse(type, title, status, detail);
        return Response.status(status)
                .type(PROBLEM_JSON)
                .entity(problem)
                .build();
    }
}
