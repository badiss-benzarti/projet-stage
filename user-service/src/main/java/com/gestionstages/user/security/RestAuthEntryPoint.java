package com.gestionstages.user.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** 401 quand le jeton manque, 403 quand le role ne suffit pas. */
@Component
@RequiredArgsConstructor
public class RestAuthEntryPoint implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;

    @Override
    public void commence(HttpServletRequest rq, HttpServletResponse rs, AuthenticationException e) throws IOException {
        write(rs, HttpStatus.UNAUTHORIZED, "Authentification requise");
    }

    @Override
    public void handle(HttpServletRequest rq, HttpServletResponse rs, AccessDeniedException e) throws IOException {
        write(rs, HttpStatus.FORBIDDEN, "Votre role ne permet pas cette action");
    }

    private void write(HttpServletResponse rs, HttpStatus status, String message) throws IOException {
        rs.setStatus(status.value());
        rs.setContentType(MediaType.APPLICATION_JSON_VALUE);
        rs.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        mapper.writeValue(rs.getOutputStream(), body);
    }
}
