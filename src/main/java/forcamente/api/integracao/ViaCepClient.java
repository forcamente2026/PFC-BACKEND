package forcamente.api.integracao;

import forcamente.api.exception.ServicoIndisponivelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ViaCepClient {
    private final RestClient viaCepRestClient;

    public ViaCepResponseDTO buscar(String cep) {
        try {
            return viaCepRestClient.get()
                    .uri("/{cep}/json/", cep)
                    .retrieve()
                    .body(ViaCepResponseDTO.class);
        } catch (RestClientException excecao) {
            log.error("Falha ao consultar o ViaCEP para o CEP {}", cep, excecao);
            throw new ServicoIndisponivelException("Servico de consulta de CEP indisponivel no momento, tente mais tarde");
        }
    }
}
