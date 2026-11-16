package forcamente.api.controller;

import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.StatusAprovacaoEnum;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.IEmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Cadastro - a conta nova e gravada no banco com o JSON que o front envia")
class CadastroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @MockitoBean
    private IEmailService emailService;

    @Test
    @DisplayName("o cadastro de aluno responde 201 e a linha entra no banco: nenhuma coluna obrigatoria fica sem valor")
    void deveGravarUmAlunoNovoNoBanco() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "Aluna Nova do Cadastro",
                                  "email": "aluna.nova.cadastro@exemplo.com",
                                  "senha": "Senha@Forte123",
                                  "dataNascimento": "1998-04-12",
                                  "papel": "ALUNO",
                                  "aceitouTermosUso": true,
                                  "aceitouPoliticaPrivacidade": true,
                                  "versaoTermosUso": "1.0",
                                  "versaoPoliticaPrivacidade": "1.0"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("ALUNO"));

        usuarioRepository.flush();

        var gravado = usuarioRepository.findByEmail("aluna.nova.cadastro@exemplo.com").orElseThrow();
        assertThat(gravado.getPapel()).isEqualTo(PapelUsuarioEnum.ALUNO);
        assertThat(gravado.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("o cadastro de professor responde 201 e a linha entra no banco com o CREF aguardando verificacao")
    void deveGravarUmProfessorNovoNoBanco() throws Exception {
        mockMvc.perform(post("/api/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nomeCompleto": "Professor Novo do Cadastro",
                                  "email": "professor.novo.cadastro@exemplo.com",
                                  "senha": "Senha@Forte123",
                                  "dataNascimento": "1990-08-25",
                                  "papel": "PROFESSOR",
                                  "aceitouTermosUso": true,
                                  "aceitouPoliticaPrivacidade": true,
                                  "versaoTermosUso": "1.0",
                                  "versaoPoliticaPrivacidade": "1.0",
                                  "cref": "913572-G/AC",
                                  "formacao": "BACHARELADO",
                                  "cep": "69900062",
                                  "logradouro": "Rua do Cadastro",
                                  "numero": "100",
                                  "complemento": "",
                                  "bairro": "Centro",
                                  "cidade": "Rio Branco",
                                  "estado": "AC"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.papel").value("PROFESSOR"));

        usuarioRepository.flush();

        var gravado = usuarioRepository.findByEmail("professor.novo.cadastro@exemplo.com").orElseThrow();
        assertThat(gravado.getSituacaoCref()).isEqualTo(StatusAprovacaoEnum.PENDENTE);
        assertThat(gravado.getCref()).isEqualTo("913572-G/AC");
    }
}
