package forcamente.api.service;

import forcamente.api.dto.*;

public interface IAuthService {

    LoginPendenteResponseDTO login(LoginRequestDTO loginRequestDTO);

    LoginResponseDTO verificarCodigo(VerificarCodigoRequestDTO verificarCodigoRequestDTO);

    void esqueciSenha(EsqueciSenhaRequestDTO esqueciSenhaRequestDTO);

    void redefinirSenha(RedefinirSenhaRequestDTO redefinirSenhaRequestDTO);


}
