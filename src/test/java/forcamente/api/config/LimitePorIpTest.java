package forcamente.api.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "limite-por-ip.requisicoes=3",
        "limite-por-ip.janela-minutos=5"
})
@DisplayName("Limite por IP - o contador por conta nao cobre pulverizacao nem cadastro em massa")
class LimitePorIpTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("passado o teto, o login responde 429 mesmo variando o e-mail a cada tentativa")
    void deveBarrarLoginPorIpAindaQueOEmailMude() throws Exception {
        var deUmIp = vindoDe("203.0.113.10");

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login").with(deUmIp)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"ninguem" + i + "@umc.br\",\"senha\":\"Senha@123\"}"))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(deUmIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ninguem9@umc.br\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message")
                        .value("Muitas requisicoes deste endereco. Tente novamente mais tarde."));
    }

    @Test
    @DisplayName("o teto e por endereco, entao outro IP nao herda o balde gasto")
    void outroIpNaoHerdaOBaldeGasto() throws Exception {
        var gasto = vindoDe("203.0.113.20");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login").with(gasto)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"alguem@umc.br\",\"senha\":\"Senha@123\"}"));
        }

        mockMvc.perform(post("/api/auth/login").with(vindoDe("203.0.113.21"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alguem@umc.br\",\"senha\":\"Senha@123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("o cadastro tem balde proprio, para o login gasto nao fechar a porta de quem vai se cadastrar")
    void cadastroTemBaldeSeparadoDoLogin() throws Exception {
        var mesmoIp = vindoDe("203.0.113.30");

        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/auth/login").with(mesmoIp)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"alguem@umc.br\",\"senha\":\"Senha@123\"}"));
        }

        mockMvc.perform(post("/api/usuarios").with(mesmoIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("rota autenticada do dia a dia nao entra no limite, senao o uso normal da conta travaria")
    void rotaAutenticadaNaoEntraNoLimite() throws Exception {
        var mesmoIp = vindoDe("203.0.113.40");

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(get("/api/usuarios/me").with(mesmoIp))
                    .andExpect(status().isUnauthorized());
        }
    }

    private RequestPostProcessor vindoDe(String endereco) {
        return request -> {
            request.setRemoteAddr(endereco);
            return request;
        };
    }
}
