package com.ondeehoje;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ondeehoje.shared.config.ClockConfiguration;
import com.ondeehoje.shared.config.SecurityConfiguration;
import com.ondeehoje.shared.error.ApiErrorFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(excludeAutoConfiguration = UserDetailsServiceAutoConfiguration.class)
@Import({SecurityConfiguration.class, ClockConfiguration.class, ApiErrorFactory.class})
@ActiveProfiles({"local", "prod"})
class ProductionExposureTest {
    @Autowired MockMvc mvc;

    @Test
    void productionDeniesDocsEvenWhenLocalIsAlsoActive() throws Exception {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui/index.html", "/actuator/env"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }
    }
}
