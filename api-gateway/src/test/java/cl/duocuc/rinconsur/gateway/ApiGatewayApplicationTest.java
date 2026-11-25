package cl.duocuc.rinconsur.gateway;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS, properties = "eureka.client.enabled=false")
class ApiGatewayApplicationTest {
    @Autowired private WebApplicationContext contexto;

    @Test
    void cargaLasRutasPublicasYDejaLosEndpointsInternosFueraDelGateway() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(contexto).build();

        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/login")).andExpect(status().isNotFound());
        mvc.perform(post("/internal/credentials")).andExpect(status().isNotFound());

        assertThatThrownBy(() -> mvc.perform(post("/api/v1/auth/login")))
                .hasRootCauseInstanceOf(HttpServerErrorException.class)
                .hasRootCauseMessage("503 Unable to find instance for auth-service");
    }
}
