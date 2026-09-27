package com.ondeehoje.establishment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ondeehoje.establishment.model.EstablishmentStatus;
import com.ondeehoje.shared.config.ClockConfiguration;
import com.ondeehoje.shared.error.ApiErrorFactory;
import com.ondeehoje.shared.error.GlobalExceptionHandler;
import jakarta.persistence.Entity;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.*;

@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = EstablishmentDtoTest.FixtureController.class)
@Import({GlobalExceptionHandler.class, ApiErrorFactory.class, ClockConfiguration.class})
class EstablishmentDtoTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void validCreateRequestPassesValidation() throws Exception {
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, validRequest()).andExpect(status().isOk());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "street", "city", "state", "timeZone", "countryCode"})
    void blankRequiredFieldsReturnValidationError(String field) throws Exception {
        invalid(validRequest().put(field, " \t "), field);
    }

    @Test
    void missingCategoryIdReturnsValidationError() throws Exception {
        ObjectNode body = validRequest();
        body.remove("categoryId");
        invalid(body, "categoryId");
    }

    @ParameterizedTest
    @ValueSource(strings = {"categoryId", "name", "street", "city", "state",
            "countryCode", "latitude", "longitude", "timeZone"})
    void missingAndNullRequiredFieldsReturnValidationError(String field) throws Exception {
        ObjectNode body = validRequest();
        body.remove(field);
        invalid(body, field);
        invalid(body.putNull(field), field);
    }

    @ParameterizedTest
    @ValueSource(doubles = {-90.1, 90.1})
    void invalidLatitudeReturnsValidationError(double value) throws Exception {
        invalid(validRequest().put("latitude", value), "latitude");
    }

    @ParameterizedTest
    @ValueSource(doubles = {-180.1, 180.1})
    void invalidLongitudeReturnsValidationError(double value) throws Exception {
        invalid(validRequest().put("longitude", value), "longitude");
    }

    @ParameterizedTest
    @ValueSource(strings = {"br", "BRA", "B1", "B", " BR", "BR ", "БR"})
    void invalidCountryCodeReturnsValidationError(String value) throws Exception {
        invalid(validRequest().put("countryCode", value), "countryCode");
    }

    @Test
    void maximumNameLengthAccepted() throws Exception {
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, validRequest().put("name", "a".repeat(150)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void overMaximumNameLengthRejected() throws Exception {
        invalid(validRequest().put("name", "a".repeat(151)), "name");
    }

    @ParameterizedTest
    @CsvSource({"-90,-180", "90,180", "0,0"})
    void coordinateBoundariesAccepted(double latitude, double longitude) throws Exception {
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, validRequest().put("latitude", latitude).put("longitude", longitude))
                    .andExpect(status().isOk());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"ownerId", "status", "version", "createdAt", "updatedAt", "id", "unknown"})
    void serverManagedFieldsAreRejected(String field) throws Exception {
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, validRequest().put(field, "client-value"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                    .andExpect(jsonPath("$.timestamp").isString())
                    .andExpect(jsonPath("$.message").value("The request could not be processed."))
                    .andExpect(jsonPath("$.path").value("/dto-fixture/" + operation))
                    .andExpect(jsonPath("$.fieldErrors").isEmpty());
        }
    }

    @Test
    void optionalTextFieldsAreAcceptedWithoutUndocumentedLimits() throws Exception {
        ObjectNode body = validRequest();
        for (String field : new String[]{"description", "phone", "websiteUrl", "instagramHandle",
                "number", "complement", "neighborhood", "postalCode"}) {
            body.put(field, "a".repeat(1000));
        }
        body.put("timeZone", "Domain-validation-is-deferred");
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, body).andExpect(status().isOk());
        }
    }

    @Test
    void responseDtoDoesNotContainJpaEntities() throws Exception {
        for (Class<?> dto : new Class<?>[]{EstablishmentCreateRequest.class,
                EstablishmentUpdateRequest.class, EstablishmentResponse.class}) {
            assertThat(dto.isRecord()).isTrue();
            assertThat(Arrays.stream(dto.getRecordComponents())
                    .anyMatch(component -> component.getType().isAnnotationPresent(Entity.class))).isFalse();
        }
        UUID id = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-27T12:00:00Z");
        var response = new EstablishmentResponse(id, categoryId, "Venue", null, null, null, null,
                "Street", null, null, null, "Manaus", "AM", null, "BR", -3.1, -60.0,
                "America/Manaus", EstablishmentStatus.PENDING_REVIEW, 0, now, now);
        var json = mapper.readTree(mapper.writeValueAsString(response));
        assertThat(json.properties()).extracting(java.util.Map.Entry::getKey).containsExactlyInAnyOrder(
                "id", "categoryId", "name", "description", "phone", "websiteUrl", "instagramHandle",
                "street", "number", "complement", "neighborhood", "city", "state", "postalCode",
                "countryCode", "latitude", "longitude", "timeZone", "status", "version", "createdAt", "updatedAt");
        assertThat(json.get("id").asText()).isEqualTo(id.toString());
        assertThat(json.get("categoryId").asText()).isEqualTo(categoryId.toString());
        assertThat(json.get("status").asText()).isEqualTo("PENDING_REVIEW");
        assertThat(json.get("version").asLong()).isZero();
        assertThat(json.get("createdAt").asText()).isEqualTo(now.toString());
        assertThat(json.get("updatedAt").asText()).isEqualTo(now.toString());
    }

    private ObjectNode validRequest() {
        return mapper.createObjectNode()
                .put("categoryId", "00000000-0000-4000-8000-000000000001")
                .put("name", "Venue").put("street", "Street").put("city", "Manaus")
                .put("state", "AM").put("countryCode", "BR")
                .put("latitude", -3.1).put("longitude", -60.0).put("timeZone", "America/Manaus");
    }

    private ResultActions submit(String operation, ObjectNode body) throws Exception {
        return mvc.perform(post("/dto-fixture/" + operation).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsBytes(body)));
    }

    private void invalid(ObjectNode body, String field) throws Exception {
        for (String operation : new String[]{"create", "update"}) {
            submit(operation, body).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").isString())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("Request validation failed."))
                    .andExpect(jsonPath("$.path").value("/dto-fixture/" + operation))
                    .andExpect(jsonPath("$.fieldErrors[*].field", hasItem(field)))
                    .andExpect(jsonPath("$.fieldErrors[0].message").isNotEmpty());
        }
    }

    @RestController
    static class FixtureController {
        @PostMapping("/dto-fixture/create")
        void create(@Valid @RequestBody EstablishmentCreateRequest request) {}

        @PostMapping("/dto-fixture/update")
        void update(@Valid @RequestBody EstablishmentUpdateRequest request) {}
    }
}
