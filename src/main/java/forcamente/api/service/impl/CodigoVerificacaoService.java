package forcamente.api.service.impl;


import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.TipoCodigoEnum;
import forcamente.api.exception.CredenciaisInvalidasException;
import forcamente.api.exception.LimiteDeTentativasException;
import forcamente.api.service.ICodigoVerificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodigoVerificacaoService implements ICodigoVerificacaoService {


    private static final int TENTATIVAS_POR_CODIGO = 2;

    private static final int MFA_PEDIDOS_POR_HORA = 6;

    private static final int MFA_VALIDADE_MINUTOS = 2;

    private static final int REDEFINICAO_PEDIDOS_POR_DIA = 6;

    private static final int REDEFINICAO_VALIDADE_MINUTOS = 15;

    private static final int REDEFINICAO_ERROS_PARA_BLOQUEIO = 6;

    private static final int REDEFINICAO_BLOQUEIO_HORAS = 4;

    private static final int MFA_BLOQUEIO_MAXIMO_HORAS = 12;

    private static final int MFA_OCORRENCIAS_CADUCAM_DIAS = 7;

    private final PasswordEncoder passwordEncoder;

    private final Clock clock;

    private final SecureRandom sorteio = new SecureRandom();


    @Override
    public String gerarCodigo(UsuarioEntity usuario, TipoCodigoEnum tipo) {
        LocalDateTime agora = LocalDateTime.now(clock);

        verificarBloqueio(usuario, tipo, agora);
        contarPedido(usuario,tipo,agora);
        String codigo = String.format("%04d", sorteio.nextInt(10000));

        usuario.setCodigoHash(passwordEncoder.encode(codigo));
        usuario.setCodigoTipo(tipo);
        usuario.setCodigoExpiraEm(agora.plusMinutes(validadeEmMinutos(tipo)));
        usuario.setCodigoTentativas(0);

        log.info("Codigo gerado: tipo={} usuario={}", tipo, usuario.getId());
        return codigo;
    }

    @Override
    public void validarCodigo(UsuarioEntity usuario, TipoCodigoEnum tipo, String codigo) {
        LocalDateTime agora = LocalDateTime.now(clock);

        verificarBloqueio(usuario, tipo, agora);

        if (usuario.getCodigoHash() == null
                || usuario.getCodigoTipo() != tipo
                || usuario.getCodigoExpiraEm() == null
                || usuario.getCodigoExpiraEm().isBefore(agora)) {
            registrarErro(usuario, tipo, agora);
            throw new CredenciaisInvalidasException("Codigo invalido ou expirado");
        }

        if (!passwordEncoder.matches(codigo, usuario.getCodigoHash())) {
            usuario.setCodigoTentativas(usuario.getCodigoTentativas() + 1);


            if (usuario.getCodigoTentativas() >= TENTATIVAS_POR_CODIGO) {
                anularCodigo(usuario);
            }
            registrarErro(usuario, tipo, agora);
            throw new CredenciaisInvalidasException("Codigo invalido ou expirado");
        }
        anularCodigo(usuario);

        zerarContadores(usuario, tipo);

        log.info("Codigo validado: tipo={} usuario={}",tipo,usuario.getId());

    }

    private int validadeEmMinutos(TipoCodigoEnum tipo) {
        return tipo == TipoCodigoEnum.MFA ? MFA_VALIDADE_MINUTOS : REDEFINICAO_VALIDADE_MINUTOS;
    }

    private void anularCodigo(UsuarioEntity usuario) {
        usuario.setCodigoHash(null);
        usuario.setCodigoTipo(null);
        usuario.setCodigoExpiraEm(null);
        usuario.setCodigoTentativas(null);
    }

    private void verificarBloqueio(UsuarioEntity usuario, TipoCodigoEnum tipo, LocalDateTime agora) {
        LocalDateTime bloqueadoAte = tipo == TipoCodigoEnum.MFA ? usuario.getMfaBloqueadoAte() : usuario.getRedefinicaoBloqueadoAte();

        if (bloqueadoAte != null && bloqueadoAte.isAfter(agora)) {
            throw new LimiteDeTentativasException("Muitas tentativas. Tente novamente mais tarde.");
        }
    }

    private void contarPedido(UsuarioEntity usuario, TipoCodigoEnum tipo, LocalDateTime agora) {

        if (tipo == TipoCodigoEnum.MFA) {
            contarPedidoMfa(usuario, agora);
        } else {
            contarPedidoRedefinicao(usuario, agora);
        }
    }

    private void contarPedidoMfa(UsuarioEntity usuario, LocalDateTime agora) {
        caducarOcorrenciasDeBloqueio(usuario, agora);

        if (usuario.getMfaJanelaInicio() == null
        || usuario.getMfaJanelaInicio().plusHours(1).isBefore(agora)) {
            usuario.setMfaJanelaInicio(agora);
            usuario.setMfaPedidos(0);
        }
        if (usuario.getMfaPedidos() >= MFA_PEDIDOS_POR_HORA) {
            int ocorrencias = usuario.getMfaOcorrenciasBloqueio() == null ? 1 : usuario.getMfaOcorrenciasBloqueio() + 1;
            long horasDeBloqueio = Math.min(ocorrencias, MFA_BLOQUEIO_MAXIMO_HORAS);
            usuario.setMfaOcorrenciasBloqueio(ocorrencias);
            usuario.setMfaBloqueadoAte(agora.plusHours(horasDeBloqueio));
            usuario.setMfaPedidos(0);
            usuario.setMfaJanelaInicio(null);

            throw new LimiteDeTentativasException(
                    "Muitos pedidos de codigo. Tente novamente em " + horasDeBloqueio + " hora(s).");
        }

        usuario.setMfaPedidos(usuario.getMfaPedidos() + 1);
    }

    private void contarPedidoRedefinicao (UsuarioEntity usuario, LocalDateTime agora) {
        LocalDate hoje = agora.toLocalDate();

        if (!hoje.equals(usuario.getRedefinicaoDia())){
            usuario.setRedefinicaoDia(hoje);
            usuario.setRedefinicaoPedidos(0);
        }

        if (usuario.getRedefinicaoPedidos() >= REDEFINICAO_PEDIDOS_POR_DIA) {
            throw new LimiteDeTentativasException(
                    "Limite diario de pedidos atingido. Tente novamente amanha."
            );
        }

        usuario.setRedefinicaoPedidos(usuario.getRedefinicaoPedidos() + 1);
    }

    private void registrarErro(UsuarioEntity usuario, TipoCodigoEnum tipo, LocalDateTime agora) {
        if (tipo != TipoCodigoEnum.REDEFINICAO_SENHA) {
            return;
        }

        int erros = usuario.getRedefinicaoErrosConsecutivos() == null ? 1 : usuario.getRedefinicaoErrosConsecutivos() + 1;

        usuario.setRedefinicaoErrosConsecutivos(erros);

        if (erros >= REDEFINICAO_ERROS_PARA_BLOQUEIO) {
            usuario.setRedefinicaoBloqueadoAte(agora.plusHours(REDEFINICAO_BLOQUEIO_HORAS));
            usuario.setRedefinicaoErrosConsecutivos(0);
            anularCodigo(usuario);
        }
    }

    private void zerarContadores(UsuarioEntity usuario, TipoCodigoEnum tipo) {
        if (tipo == TipoCodigoEnum.MFA) {
            usuario.setMfaPedidos(0);
            usuario.setMfaJanelaInicio(null);
            usuario.setMfaOcorrenciasBloqueio(null);
            usuario.setMfaBloqueadoAte(null);
        } else{
            usuario.setRedefinicaoPedidos(0);
            usuario.setRedefinicaoDia(null);
            usuario.setRedefinicaoErrosConsecutivos(0);
            usuario.setRedefinicaoBloqueadoAte(null);
        }
    }

    private void caducarOcorrenciasDeBloqueio(UsuarioEntity usuario, LocalDateTime agora) {
        if (usuario.getMfaBloqueadoAte() == null) {
            return;
        }
        if (usuario.getMfaBloqueadoAte().plusDays(MFA_OCORRENCIAS_CADUCAM_DIAS).isBefore(agora)) {
            usuario.setMfaOcorrenciasBloqueio(null);
            usuario.setMfaBloqueadoAte(null);
        }
    }




}
