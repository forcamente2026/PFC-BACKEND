package forcamente.api.service;

import forcamente.api.dto.UsuarioRequestDTO;
import forcamente.api.dto.UsuarioResponseDTO;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.UsuarioEntity;
import forcamente.api.mapper.UsuarioMapper;
import forcamente.api.repository.IUsuarioRepository;
import forcamente.api.service.impl.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.time.LocalDate;
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

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("deve cadastrar usuario novo e devolver o DTO sem a senha")
    void deveCadastrarUsuarioNovo() {
        var requestDTO = umaRequisicaoValida();
        var entity = umaEntidade();
        var responseDTO = umaResposta(entity);

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(false);
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(false);
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
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(false);
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
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ja existe um usuario cadastrado com o e-mail");

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    @Test
    @DisplayName("nao deve cadastrar usuario com CPF ja existente")
    void naoDeveCadastrarUsuarioComCpfDuplicado() {
        var requestDTO = umaRequisicaoValida();

        when(usuarioRepository.existsByEmail("joao@umc.br")).thenReturn(false);
        when(usuarioRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.criarUsuario(requestDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("CPF");

        verify(usuarioRepository, never()).save(any(UsuarioEntity.class));
    }

    private UsuarioRequestDTO umaRequisicaoValida() {
        return new UsuarioRequestDTO(
                "Joao da Silva",
                "12345678901",
                LocalDate.of(2000,1,1),
                "joao@umc.br",
                "Senha@123",
                PapelUsuarioEnum.ALUNO,
                null,
                null,
                "08780000",
                "Rua das Palmeiras",
                "100",
                "Apto 12",
                "Centro",
                "Mogi das Cruzes",
                "SP");
    }

    private UsuarioEntity umaEntidade() {
        var entity = new UsuarioEntity();
        entity.setId(UUID.randomUUID());
        entity.setNomeCompleto("Joao da Silva");
        entity.setCpf("12345678901");
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
                entity.getCpf(),
                entity.getEmail(),
                entity.getPapel(),
                entity.getCidade(),
                entity.getEstado(),
                entity.isAtivo(),
                entity.getCriadoEm());
    }
}
