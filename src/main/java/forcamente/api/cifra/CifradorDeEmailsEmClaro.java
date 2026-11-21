package forcamente.api.cifra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CifradorDeEmailsEmClaro implements Callback {

    private static final String SELECIONAR_EM_CLARO = """
            select id, email, email_pendente
            from usuarios
            where email not like 'v1:%'
               or email_pendente not like 'v1:%'
               or email_hash is null
            """;

    private static final String GRAVAR_CIFRADO = """
            update usuarios
            set email = ?, email_pendente = ?, email_hash = ?
            where id = ?
            """;

    private final CifraDeEmail cifraDeEmail;

    @Override
    public boolean supports(Event evento, Context contexto) {
        return evento == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event evento, Context contexto) {
        return true;
    }

    @Override
    public void handle(Event evento, Context contexto) {
        try {
            int cifrados = cifrarEmailsEmClaro(contexto.getConnection());

            if (cifrados > 0) {
                log.info("E-mails cifrados ao subir: {}", cifrados);
            }
        } catch (SQLException excecao) {
            throw new FlywayException("Nao foi possivel cifrar os e-mails em claro", excecao);
        }
    }

    @Override
    public String getCallbackName() {
        return "cifrar-emails-em-claro";
    }

    public int cifrarEmailsEmClaro(Connection conexao) throws SQLException {
        List<ContaEmClaro> contas = new ArrayList<>();

        try (PreparedStatement consulta = conexao.prepareStatement(SELECIONAR_EM_CLARO);
             ResultSet linhas = consulta.executeQuery()) {

            while (linhas.next()) {
                contas.add(new ContaEmClaro(
                        linhas.getObject("id", UUID.class),
                        cifraDeEmail.decifrar(linhas.getString("email")),
                        cifraDeEmail.decifrar(linhas.getString("email_pendente"))));
            }
        }

        try (PreparedStatement gravacao = conexao.prepareStatement(GRAVAR_CIFRADO)) {
            for (ContaEmClaro conta : contas) {
                gravacao.setString(1, cifraDeEmail.cifrar(conta.email()));
                gravacao.setString(2, cifraDeEmail.cifrar(conta.emailPendente()));
                gravacao.setString(3, cifraDeEmail.indice(conta.email()));
                gravacao.setObject(4, conta.id());
                gravacao.addBatch();
            }

            gravacao.executeBatch();
        }

        return contas.size();
    }

    private record ContaEmClaro(UUID id, String email, String emailPendente) {
    }
}
