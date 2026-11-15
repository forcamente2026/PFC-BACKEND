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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnderecoService implements IEnderecoService {

    private static final int CEPS_EM_CACHE = 500;

    private final ViaCepClient viaCepClient;

    private final Map<String, EnderecoResponseDTO> cache = new ConcurrentHashMap<>();

    @Override
    public EnderecoResponseDTO buscarPorCep(String cep) {
        String apenasDigitos = cep == null ? "" : cep.replaceAll("\\D", "");

        if (apenasDigitos.length() != 8) {
            throw new RegraDeNegocioException("O CEP deve conter 8 dígitos");
        }
        log.info("buscarPorCep");

        EnderecoResponseDTO guardado = cache.get(apenasDigitos);

        if (guardado != null) {
            return guardado;
        }

        ViaCepResponseDTO resposta = viaCepClient.buscar(apenasDigitos);

        if (resposta == null || Boolean.TRUE.equals(resposta.erro())) {
            throw new RecursoNaoEncontradoException("Endereço não encontrado para este CEP");
        }

        var endereco = new EnderecoResponseDTO(
                apenasDigitos,
                resposta.logradouro(),
                resposta.bairro(),
                resposta.localidade(),
                resposta.uf());

        if (cache.size() >= CEPS_EM_CACHE) {
            cache.clear();
        }

        cache.put(apenasDigitos, endereco);

        return endereco;
    }
}
