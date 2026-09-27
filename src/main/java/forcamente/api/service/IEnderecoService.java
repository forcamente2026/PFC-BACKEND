package forcamente.api.service;

import forcamente.api.dto.EnderecoResponseDTO;

public interface IEnderecoService {

    EnderecoResponseDTO buscarPorCep(String cep);
}
