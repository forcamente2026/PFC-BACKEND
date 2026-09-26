package forcamente.api.service;

import forcamente.api.dto.EsqueciSenhaRequestDTO;
import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.dto.LoginResponseDTO;
import forcamente.api.dto.RedefinirSenhaRequestDTO;

public interface IAuthService {
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
    void esqueciSenha(EsqueciSenhaRequestDTO esqueciSenhaRequestDTO);

    void redefinirSenha(RedefinirSenhaRequestDTO redefinirSenhaRequestDTO);
}
