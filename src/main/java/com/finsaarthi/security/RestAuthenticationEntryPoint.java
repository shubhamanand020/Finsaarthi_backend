package com.finsaarthi.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException, ServletException {
        Object message = request.getAttribute("jwt.error.message");
        String errorMessage = message instanceof String
                ? (String) message
                : "Authentication is required to access this resource.";

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(buildErrorResponse(errorMessage));
    }

    private String buildErrorResponse(String errorMessage) {
        String escapedMessage = errorMessage
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");

        return "{\"success\":false,\"message\":\"" + escapedMessage + "\"}";
    }
}
