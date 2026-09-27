package forcamente.api.service;

import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.TipoDocumentoLegalEnum;
import forcamente.api.exception.ConflitoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.mapper.UsuarioMapper;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.*;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioService - cadastro de usuario e seguranca da senha")
class UsuarioServiceTest {

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private UsuarioMapper usuarioMapper;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private UsuarioService usuarioService;

    @Mock
    private IDocumentoLegalService documentoLegalService;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-09-22T13:00:00Z"), ZoneOffset.UTC);



    @Test
    @DisplayName("deve cadastrar usuario novo e devolver o DTO sem a senha")
    void deveCadastrarUsuarioNovo() {
        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade();
        var responseDTO = umaResposta(entity);

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(false);
        when(usuarioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(usuarioRepository.save(entity)).thenReturn(entity);
        when(usuarioMapper.toDTO(entity)).thenReturn(responseDTO);

        var resultado = usuarioService.criarUsuario(requestDTO);

        assertThat(resultado).isNotNull();
        assertThat(resultado.email()).isEqualTo("joao@umc.br");
        assertThat(resultado.papel()).isEqualTo(PapelUsuarioEnum.ALUNO);
        assertThat(resultado.ativo()).isTrue();

        verify(usuarioRepository).save(entity);
    }

    @Test
    @DisplayName("deve gravar a senha como hash BCrypt, nunca em texto puro")
    void deveGravarSenhaComHashBCrypt() {
        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade();

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(false);
        when(usuarioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(entity);
        when(usuarioMapper.toDTO(entity)).thenReturn(umaResposta(entity));

        usuarioService.criarUsuario(requestDTO);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());

        String senhaGravada = captor.getValue().getSenhaHash();

        assertThat(senhaGravada).isNotEqualTo("Senha@123");

        assertThat(senhaGravada).startsWith("$2");

        assertThat(new BCryptPasswordEncoder().matches("Senha@123", senhaGravada)).isTrue();
    }

    @Test
    @DisplayName("nao deve cadastrar usuario com e-mail ja existente")
    void naoDeveCadastrarUsuarioComEmailDuplicado() {
        var requestDTO = umaRequisicaoValida();

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criarUsuario(requestDTO))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("Ja existe um usuario cadastrado com o e-mail");

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("nao deve cadastrar professor com CPF ja existente")
    void naoDeveCadastrarUsuarioComCpfDuplicado() {
        var requestDTO = umProfessor("123456-G/SP");

        when(usuarioRepository.existsByEmail("maria@umc.br")).thenReturn(false);
        when(usuarioRepository.existsByCpf("98765432100")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criarUsuario(requestDTO))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("CPF");

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }



    @Test
    @DisplayName("nao deve cadastrar administrador pelo cadastro publico")
    void naoDeveCadastrarAdministrador() {
        var requestDTO = umaRequisicaoComPapel(PapelUsuarioEnum.ADMINISTRADOR);

        assertThatThrownBy(() -> usuarioService.criarUsuario(requestDTO))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Administrador");

        verify(usuarioRepository, never()).existsByEmail(any());
        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("nao deve cadastrar professor com CREF ja existente")
    void naoDeveCadastrarProfessorComCrefDuplicado() {
        var requestDTO = umProfessor("123456-G/SP");

        when(usuarioRepository.existsByEmail("maria@umc.br")).thenReturn(false);
        when(usuarioRepository.existsByCpf("98765432100")).thenReturn(false);
        when(usuarioRepository.existsByCref("123456-G/SP")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criarUsuario(requestDTO))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("CREF");

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("deve listar as duas formacoes como codigo e descricao")
    void deveListarFormacoes() {
        var resultado = usuarioService.listarFormacoes();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.getFirst().codigo()).isEqualTo("BACHARELADO");
        assertThat(resultado.getFirst().descricao()).isEqualTo("Bacharelado em Educação Física");
    }
    @Test
    @DisplayName("deve registrar data e versao do aceite dos dois documentos")
    void deveRegistrarAceiteComDataEVersao() {
        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade();

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(false);
        when(usuarioMapper.toEntity(requestDTO)).thenReturn(entity);
        when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(entity);
        when(usuarioMapper.toDTO(entity)).thenReturn(umaResposta(entity));
        when(documentoLegalService.versaoVigente(TipoDocumentoLegalEnum.TERMOS_USO)).thenReturn("1.0");
        when(documentoLegalService.versaoVigente(TipoDocumentoLegalEnum.POLITICA_PRIVACIDADE)).thenReturn("2.0");

        usuarioService.criarUsuario(requestDTO);

        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());
        var salvo = captor.getValue();

        assertThat(salvo.getVersaoTermosUso()).isEqualTo("1.0");
        assertThat(salvo.getVersaoPoliticaPrivacidade()).isEqualTo("2.0");
        assertThat(salvo.getAceitouTermosUsoEm()).isEqualTo(LocalDateTime.now(clock));
        assertThat(salvo.getAceitouPoliticaPrivacidadeEm()).isEqualTo(LocalDateTime.now(clock));
    }



    private UsuarioRequestDTO umaRequisicaoValida() {
        return new UsuarioRequestDTO(
                "Joao da Silva",
                null,
                LocalDate.of(2000,1,1),
                "joao@umc.br",
                "Senha@123",
                PapelUsuarioEnum.ALUNO,
                null,
                null,
                null,
                "08780000",
                "Rua das Palmeiras",
                "100",
                "Apto 12",
                "Centro",
                "Mogi das Cruzes",
                "SP",
                true,
                true);
    }

    private UsuarioRequestDTO umaRequisicaoComPapel(PapelUsuarioEnum papel) {
        return new UsuarioRequestDTO(
                "Joao da Silva",
                "12345678901",
                LocalDate.of(2000, 1, 1),
                "joao@umc.br",
                "Senha@123",
                papel,
                null,
                null,
                null,
                "08780000",
                "Rua das Palmeiras",
                "100",
                "Apto 12",
                "Centro",
                "Mogi das Cruzes",
                "SP",
                true,
                true);
    }


    private UsuarioRequestDTO umProfessor(String cref) {
        return new UsuarioRequestDTO(
                "Maria Souza",
                "98765432100",
                LocalDate.of(1990,5,20),
                "maria@umc.br",
                "Senha@123",
                PapelUsuarioEnum.PROFESSOR,
                cref,
                FormacaoEnum.BACHARELADO,
                "Universidade de Mogi das Cruzes",
                "08780000",
                "Rua B",
                "2",
                null,
                "Centro",
                "Mogi das Cruzes",
                "SP",
                true,
                true
        );

    }

    private UsuarioEntity umaEntidade() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Joao da Silva");
        entity.setCpf("12345678901");
        entity.setDataNascimento(LocalDate.of(2000, 1, 1));
        entity.setEmail("joao@umc.br");
        entity.setPapel(PapelUsuarioEnum.ALUNO);
        entity.setCidade("Mogi das Cruzes");
        entity.setEstado("SP");
        entity.setAtivo(true);
        entity.setCriadoEm(LocalDateTime.now());
        return entity;
    }

    private UsuarioResponseDTO umaResposta(UsuarioEntity entity) {
        return new UsuarioResponseDTO(
                entity.getId(),
                entity.getNomeCompleto(),
                entity.getEmail(),
                entity.getPapel(),
                entity.getCref(),
                entity.getFormacao(),
                entity.getInstituicao(),
                entity.getCidade(),
                entity.getEstado(),
                entity.isAtivo(),
                entity.getCriadoEm());
    }
}
