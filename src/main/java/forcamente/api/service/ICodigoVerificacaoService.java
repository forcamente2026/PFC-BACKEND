package forcamente.api.service;

import forcamente.api.entity.UsuarioEntity;
import forcamente.api.entity.enums.TipoCodigoEnum;

public interface ICodigoVerificacaoService {

    String gerarCodigo(UsuarioEntity usuario, TipoCodigoEnum tipo);

    void validarCodigo(UsuarioEntity usuario, TipoCodigoEnum tipo, String codigo);
}
