package forcamente.api.service;

import forcamente.api.exception.RecursoNaoEncontradoException;
import forcamente.api.exception.RegraDeNegocioException;
import forcamente.api.exception.ServicoIndisponivelException;
import forcamente.api.integracao.ViaCepClient;
import forcamente.api.integracao.ViaCepResponseDTO;
import forcamente.api.service.impl.EnderecoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnderecoService - consulta de CEP no ViaCEP")
class EnderecoServiceTest {

    @Mock
    private ViaCepClient viaCepClient;

    @InjectMocks
    private EnderecoService enderecoService;

    @Test
    @DisplayName("deve devolver o endereco com os nomes de campo da nossa API")
    void deveDevolverEnderecoMapeado() {
        when(viaCepClient.buscar("08780000")).thenReturn(new ViaCepResponseDTO(
                "08780-000", "Rua das Palmeiras", "Centro", "Mogi das Cruzes", "SP", null));

        var resultado = enderecoService.buscarPorCep("08780-000");

        assertThat(resultado.cep()).isEqualTo("08780000");
        assertThat(resultado.logradouro()).isEqualTo("Rua das Palmeiras");
        assertThat(resultado.bairro()).isEqualTo("Centro");
        assertThat(resultado.cidade()).isEqualTo("Mogi das Cruzes");
        assertThat(resultado.estado()).isEqualTo("SP");
    }

    @Test
    @DisplayName("CEP geral volta com logradouro e bairro vazios, para a tela completar")
    void deveDevolverCepGeralIncompleto() {
        when(viaCepClient.buscar("01000000")).thenReturn(new ViaCepResponseDTO(
                "01000-000", "", "", "São Paulo", "SP", null));

        var resultado = enderecoService.buscarPorCep("01000000");

        assertThat(resultado.logradouro()).isEmpty();
        assertThat(resultado.bairro()).isEmpty();
        assertThat(resultado.cidade()).isEqualTo("São Paulo");
    }

    @Test
    @DisplayName("CEP inexistente vira 404, mesmo o ViaCEP respondendo 200 com erro")
    void deveRejeitarCepInexistente() {
        when(viaCepClient.buscar("99999999")).thenReturn(new ViaCepResponseDTO(
                null, null, null, null, null, true));

        assertThatThrownBy(() -> enderecoService.buscarPorCep("99999999"))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("nao encontrado");
    }

    @Test
    @DisplayName("CEP mal formado nem chega a sair na rede")
    void deveRejeitarCepMalFormadoSemChamarOViaCep() {
        assertThatThrownBy(() -> enderecoService.buscarPorCep("123"))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("8 digitos");

        verify(viaCepClient, never()).buscar(any());
    }

    @Test
    @DisplayName("falha do ViaCEP chega ao chamador como servico indisponivel")
    void devePropagarServicoIndisponivel() {
        when(viaCepClient.buscar("08780000"))
                .thenThrow(new ServicoIndisponivelException("Servico indisponivel"));

        assertThatThrownBy(() -> enderecoService.buscarPorCep("08780000"))
                .isInstanceOf(ServicoIndisponivelException.class);
    }
}