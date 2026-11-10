package forcamente.api.dto;

import java.util.List;

public record PaginaDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalDeItens,
        int totalDePaginas
) {
}
