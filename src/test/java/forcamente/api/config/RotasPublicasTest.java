package forcamente.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Seguranca - rotas que o front ja consome continuam publicas")
class RotasPublicasTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("rota publica responde 200 sem token")
    void rotaPublicaRespondeSemToken() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token invalido recebe 401 mesmo em rota publica")
    void tokenInvalidoRecebe401() throws Exception {
        mockMvc.perform(get("/api/exercicios/niveis").header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }
}