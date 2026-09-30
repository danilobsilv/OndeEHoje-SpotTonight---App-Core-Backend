package com.spotnight.shared.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;

class GlobalExceptionHandlerTest {
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC);
        mvc = MockMvcBuilders.standaloneSetup(new FixtureController())
                .setControllerAdvice(new GlobalExceptionHandler(new ApiErrorFactory(clock))).build();
    }

    @Test
    void validationUsesDocumentedShape() throws Exception {
        mvc.perform(post("/fixture").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value("/fixture"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void malformedJsonDoesNotExposeBody() throws Exception {
        mvc.perform(post("/fixture").contentType(MediaType.APPLICATION_JSON).content("secret-invalid"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-invalid"))));
    }

    @Test
    void unexpectedFailureDoesNotExposeExceptionMessage() throws Exception {
        mvc.perform(get("/fixture")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.fieldErrors").isEmpty());
    }

    @Test
    void frameworkErrorsKeepTheirStatusAndHeaders() throws Exception {
        mvc.perform(delete("/fixture")).andExpect(status().isMethodNotAllowed())
                .andExpect(header().exists("Allow"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        mvc.perform(get("/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @RestController
    static class FixtureController {
        @PostMapping("/fixture")
        void validate(@Valid @RequestBody Input input) {}

        @GetMapping("/fixture")
        void fail() { throw new IllegalStateException("secret-value"); }
    }

    record Input(@NotBlank String name) {}
}
