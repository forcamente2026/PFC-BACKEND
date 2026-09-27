package forcamente.api.entity;


import forcamente.api.entity.enums.FormacaoEnum;
import forcamente.api.entity.enums.PapelUsuarioEnum;
import forcamente.api.entity.enums.TipoCodigoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "nome_completo", nullable = false)
    private String nomeCompleto;

    @Column(name = "cpf", unique = true, length = 11)
    private String cpf;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "senha_hash", nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel", nullable = false, length = 20)
    private PapelUsuarioEnum papel;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(name = "cref", length = 11, unique = true)
    private String cref;

    @Enumerated(EnumType.STRING)
    @Column(name = "Formacao", length = 20)
    private FormacaoEnum formacao;

    @Column(name = "instituicao", length = 120)
    private String instituicao;

    @Column(name = "aceitou_termos_uso_em")
    private LocalDateTime aceitouTermosUsoEm;

    @Column(name = "versao_termos_uso", length = 10)
    private String versaoTermosUso;

    @Column(name = "aceitou_politica_privacidade_em")
    private LocalDateTime aceitouPoliticaPrivacidadeEm;

    @Column(name = "versao_politica_privacidade", length = 10)
    private String versaoPoliticaPrivacidade;

    @Column(name = "codigo_hash", length = 60)
    private String codigoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_tipo", length = 20)
    private TipoCodigoEnum codigoTipo;

    @Column(name = "codigo_expira_em")
    private LocalDateTime codigoExpiraEm;

    @Column(name = "codigo_tentativas")
    private Integer codigoTentativas;

    @Column(name = "mfa_pedidos")
    private Integer mfaPedidos;

    @Column(name = "mfa_janela_inicio")
    private LocalDateTime mfaJanelaInicio;

    @Column(name = "mfa_bloqueado_ate")
    private LocalDateTime mfaBloqueadoAte;

    @Column(name = "mfa_ocorrencias_bloqueio")
    private Integer mfaOcorrenciasBloqueio;

    @Column(name = "redefinicao_pedidos")
    private Integer redefinicaoPedidos;

    @Column(name = "redefinicao_dia")
    private LocalDate redefinicaoDia;

    @Column(name = "redefinicao_bloqueado_ate")
    private LocalDateTime redefinicaoBloqueadoAte;

    @Column(name = "redefinicao_erros_consecutivos")
    private Integer redefinicaoErrosConsecutivos;

    @Column(name = "cep", length = 8)
    private String cep;

    @Column(name = "logradouro")
    private String logradouro;

    @Column(name = "numero", length = 10)
    private String numero;

    @Column(name = "complemento")
    private String complemento;

    @Column(name = "bairro")
    private String bairro;

    @Column(name = "cidade")
    private String cidade;

    @Column(name = "estado", length = 2)
    private String estado;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
