package cl.duocuc.rinconsur.discovery;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.netflix.eureka.EurekaServerContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(useMainMethod = SpringBootTest.UseMainMethod.ALWAYS)
class EurekaServerApplicationTest {
    @Autowired private WebApplicationContext contexto;
    @Autowired private EurekaServerContext eureka;

    @Test
    void iniciaUnRegistroLocalVacioYExponeElEstadoDeSalud() throws Exception {
        assertThat(eureka.getRegistry().getApplications().getRegisteredApplications()).isEmpty();

        MockMvcBuilders.webAppContextSetup(contexto).build()
                .perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
