package forcamente.api.service.impl;

import forcamente.api.dto.EnderecoResponseDTO;
import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.integracao.ViaCepClient;
import forcamente.api.integracao.ViaCepResponseDTO;
import forcamente.api.service.IEnderecoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnderecoService implements IEnderecoService {

    private final ViaCepClient viaCepClient;

    @Override
    public EnderecoResponseDTO buscarPorCep(String cep) {
        String apenasDigitos = cep == null ? "" : cep.replaceAll("\\D", "");

        if (apenasDigitos.length() != 8) {
            throw new RegraDeNegocioException("O CEP deve conter 8 dígitos");
        }
        log.info("buscarPorCep: {}", apenasDigitos);

        ViaCepResponseDTO resposta = viaCepClient.buscar(apenasDigitos);

        if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
            throw new RecursoNaoEncontradoException("Endereço não encontrado para o CEP: " + cep);
        }

        return new EnderecoResponseDTO(
                apenasDigitos,
                resposta.logradouro(),
                resposta.bairro(),
                resposta.localidade(),
                resposta.uf());


    }
}
