package forcamente.api.cifra;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;

@Converter
@RequiredArgsConstructor
public class EmailCifradoConverter implements AttributeConverter<String, String> {

    private final CifraDeEmail cifraDeEmail;

    @Override
    public String convertToDatabaseColumn(String email) {
        return cifraDeEmail.cifrar(email);
    }

    @Override
    public String convertToEntityAttribute(String valorDoBanco) {
        return cifraDeEmail.decifrar(valorDoBanco);
    }
}
