package forcamente.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class SemMarcacaoValidator implements ConstraintValidator<SemMarcacao, String> {

    private static final Pattern ABERTURA_DE_TAG = Pattern.compile("<[a-zA-Z/!?]");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }

        return !ABERTURA_DE_TAG.matcher(valor).find();
    }
}
