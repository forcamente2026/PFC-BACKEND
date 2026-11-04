package forcamente.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;

public class LimiteDeBytesValidator implements ConstraintValidator<LimiteDeBytes, String> {

    private int max;

    @Override
    public void initialize(LimiteDeBytes anotacao) {
        max = anotacao.max();
    }

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }

        return valor.getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
