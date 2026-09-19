package forcamente.api.service;

import forcamente.api.dto.LoginRequestDTO;
import forcamente.api.dto.LoginResponseDTO;

public interface IAuthService {
    LoginResponseDTO login(LoginRequestDTO loginRequestDTO);
}
