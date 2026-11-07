package forcamente.api.dto;

public final class Mascaras {

    private static final String OCULTO = "***";

    private static final int TAMANHO_DA_DATA = 10;

    private Mascaras() {
    }

    public static String email(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }

        int arroba = email.indexOf('@');

        if (arroba < 1) {
            return OCULTO;
        }

        return email.charAt(0) + OCULTO + email.substring(arroba);
    }

    public static String dataNascimento(String dataNascimento) {
        if (dataNascimento == null || dataNascimento.length() != TAMANHO_DA_DATA) {
            return dataNascimento;
        }

        return "**/**/" + dataNascimento.substring(6);
    }

    public static String cref(String cref) {
        if (cref == null || cref.isBlank()) {
            return cref;
        }

        if (cref.length() < 6) {
            return OCULTO;
        }

        return cref.charAt(0) + OCULTO + cref.substring(4);
    }

    public static UsuarioDetalheResponseDTO mascarar(UsuarioDetalheResponseDTO detalhe) {
        if (detalhe == null) {
            return null;
        }

        return new UsuarioDetalheResponseDTO(
                detalhe.id(),
                detalhe.nomeCompleto(),
                email(detalhe.email()),
                detalhe.papel(),
                dataNascimento(detalhe.dataNascimento()),
                cref(detalhe.cref()),
                detalhe.formacao(),
                detalhe.situacaoCref(),
                detalhe.motivoRejeicaoCref(),
                null,
                null,
                null,
                null,
                null,
                detalhe.cidade(),
                detalhe.estado(),
                detalhe.ativo(),
                detalhe.criadoEm(),
                detalhe.anonimizacaoSolicitadaEm(),
                detalhe.anonimizadoEm());
    }

    public static UsuarioResponseDTO mascarar(UsuarioResponseDTO usuario) {
        if (usuario == null) {
            return null;
        }

        return new UsuarioResponseDTO(
                usuario.id(),
                usuario.nomeCompleto(),
                email(usuario.email()),
                usuario.papel(),
                cref(usuario.cref()),
                usuario.formacao(),
                usuario.cidade(),
                usuario.estado(),
                usuario.ativo(),
                usuario.criadoEm());
    }

    public static VerificacaoPendenteDTO mascarar(VerificacaoPendenteDTO pendente) {
        if (pendente == null) {
            return null;
        }

        return new VerificacaoPendenteDTO(
                pendente.id(),
                pendente.nomeCompleto(),
                email(pendente.email()),
                cref(pendente.cref()),
                pendente.formacao(),
                pendente.criadoEm());
    }
}
