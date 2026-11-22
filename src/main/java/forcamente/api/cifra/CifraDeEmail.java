package forcamente.api.cifra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.AesGcmBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Component
public class CifraDeEmail {

    public static final String PREFIXO = "v1:";

    private static final int BYTES_DA_CHAVE = 32;

    private static final String ALGORITMO_DO_INDICE = "HmacSHA256";

    private final BytesEncryptor cifra;

    private final SecretKeySpec chaveDoIndice;

    public CifraDeEmail(@Value("${cifra-de-email.chave}") String chave,
                        @Value("${cifra-de-email.chave-indice}") String chaveIndice) {
        this.cifra = AesGcmBytesEncryptor
                .withSecretKey(new SecretKeySpec(decodificar(chave, "cifra-de-email.chave"), "AES"))
                .build();
        this.chaveDoIndice = new SecretKeySpec(
                decodificar(chaveIndice, "cifra-de-email.chave-indice"), ALGORITMO_DO_INDICE);
    }

    public String cifrar(String email) {
        if (email == null) {
            return null;
        }

        byte[] cifrado = cifra.encrypt(email.getBytes(StandardCharsets.UTF_8));

        return PREFIXO + Base64.getEncoder().encodeToString(cifrado);
    }

    public String decifrar(String valor) {
        if (!estaCifrado(valor)) {
            return valor;
        }

        byte[] cifrado = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));

        return new String(cifra.decrypt(cifrado), StandardCharsets.UTF_8);
    }

    public String indice(String email) {
        if (email == null) {
            return null;
        }

        try {
            Mac mac = Mac.getInstance(ALGORITMO_DO_INDICE);
            mac.init(chaveDoIndice);

            byte[] codigo = mac.doFinal(email.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(codigo);
        } catch (GeneralSecurityException excecao) {
            throw new IllegalStateException("Nao foi possivel calcular o indice do e-mail", excecao);
        }
    }

    public static boolean estaCifrado(String valor) {
        return valor != null && valor.startsWith(PREFIXO);
    }

    private static byte[] decodificar(String chave, String nome) {
        byte[] bytes;

        try {
            bytes = Base64.getDecoder().decode(chave.trim());
        } catch (IllegalArgumentException excecao) {
            throw new IllegalStateException(nome + " precisa estar em Base64", excecao);
        }

        if (bytes.length != BYTES_DA_CHAVE) {
            throw new IllegalStateException(nome + " precisa ter " + BYTES_DA_CHAVE + " bytes");
        }

        return bytes;
    }
}
