package com.peatroxd.mtprototest.proxy.ranking.segment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/session")
public class SessionDiagnosticController {

    private static final Logger DIAGNOSTIC_LOG = LoggerFactory.getLogger("session.diagnostics");

    private final ClientRequestKeyResolver clientKeyResolver;
    private final ObjectMapper objectMapper;

    public SessionDiagnosticController(ClientRequestKeyResolver clientKeyResolver, ObjectMapper objectMapper) {
        this.clientKeyResolver = clientKeyResolver;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/diagnostic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reportDiagnostic(@Valid @RequestBody SessionDiagnosticRequest body,
                                  HttpServletRequest request) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("clientKey", clientKeyResolver.resolve(request));
        entry.put("failedAttempts", body.failedAttempts());
        if (body.isp() != null && !body.isp().isBlank()) entry.put("isp", body.isp());
        if (body.notes() != null && !body.notes().isBlank()) entry.put("notes", body.notes());

        try {
            DIAGNOSTIC_LOG.info(objectMapper.writeValueAsString(entry));
        } catch (JsonProcessingException e) {
            DIAGNOSTIC_LOG.warn("Failed to serialize diagnostic entry: {}", e.getMessage());
        }
    }
}
