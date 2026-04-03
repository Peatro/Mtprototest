package com.peatroxd.mtprototest.api;

import com.peatroxd.mtprototest.common.api.GlobalApiExceptionHandler;
import com.peatroxd.mtprototest.common.web.ClientRequestKeyResolver;
import com.peatroxd.mtprototest.common.web.RateLimitInterceptor;
import com.peatroxd.mtprototest.common.web.RateLimitProperties;
import com.peatroxd.mtprototest.proxy.controller.ProxyController;
import com.peatroxd.mtprototest.proxy.dto.response.ProxyFeedbackResponse;
import com.peatroxd.mtprototest.proxy.service.ProxyFeedbackService;
import com.peatroxd.mtprototest.proxy.service.ProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProxyApiHardeningWebTest {

    @Mock
    private ProxyService proxyService;

    @Mock
    private ProxyFeedbackService proxyFeedbackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RateLimitProperties rateLimitProperties = new RateLimitProperties();
        rateLimitProperties.setEnabled(true);
        rateLimitProperties.setPublicReadLimit(2);
        rateLimitProperties.setPublicReadWindowMs(60_000);
        rateLimitProperties.setFeedbackWriteLimit(1);
        rateLimitProperties.setFeedbackWriteWindowMs(60_000);

        ClientRequestKeyResolver clientRequestKeyResolver = new ClientRequestKeyResolver();
        ProxyController controller = new ProxyController(proxyService, proxyFeedbackService, clientRequestKeyResolver);
        RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor(clientRequestKeyResolver, rateLimitProperties);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalApiExceptionHandler())
                .addInterceptors(rateLimitInterceptor)
                .setValidator(validator)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void shouldReturnStructuredValidationErrorForInvalidQueryParams() throws Exception {
        mockMvc.perform(get("/api/proxies")
                        .param("page", "-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnStructuredNotFoundError() throws Exception {
        when(proxyService.getById(999999L))
                .thenThrow(new ResponseStatusException(NOT_FOUND, "Proxy not found"));

        mockMvc.perform(get("/api/proxies/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRateLimitPublicReadEndpoints() throws Exception {
        when(proxyService.getBest()).thenReturn(List.of());

        mockMvc.perform(get("/api/proxies/best").header("X-Client-Fingerprint", "reader-1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/proxies/best").header("X-Client-Fingerprint", "reader-1"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/proxies/best").header("X-Client-Fingerprint", "reader-1"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    void shouldRateLimitFeedbackEndpoint() throws Exception {
        when(proxyFeedbackService.submitFeedback(eq(1L), any(), eq("writer-1")))
                .thenReturn(new ProxyFeedbackResponse(true, 1L, "WORKED", "DESKTOP"));

        String payload = "{\"result\":\"WORKED\",\"platform\":\"DESKTOP\"}";

        mockMvc.perform(post("/api/proxies/1/feedback")
                        .header("X-Client-Fingerprint", "writer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/proxies/1/feedback")
                        .header("X-Client-Fingerprint", "writer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isTooManyRequests());
    }
}
