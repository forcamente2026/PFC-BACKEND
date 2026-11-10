package forcamente.api.entity.enums;

import lombok.Getter;

@Getter
public enum NivelAtividadeEnum {
    SEDENTARIO("Sedentário, sem treino regular", 1.2),
    LEVE("Leve - 1 a 3 vezes por semana", 1.375),
    MODERADO("Moderado - 3 a 5 vezes por semana", 1.55),
    INTENSO("Intenso - 6 a 7 vezes por semana", 1.725),
    MUITO_INTENSO("Muito intenso - 2 treinos por dia", 1.9);


    private final String descricao;
    private final double fator;

    NivelAtividadeEnum(String descricao, double fator){
        this.descricao = descricao;
        this.fator = fator;
    }
}
