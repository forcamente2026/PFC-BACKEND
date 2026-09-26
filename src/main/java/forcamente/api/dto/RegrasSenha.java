package forcamente.api.dto;

public final class RegrasSenha {

    public  static final String PADRAO =
            "(?=.*\\p{Ll})(?=.*\\p{Lu})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}\\s]).*";

    public static final String MENSAGEM = "A senha deve ter letra minuscula, maiuscula, numero e caractere especial";

    public static final int TAMANHO_MINIMO = 8;

    private RegrasSenha(){
    }
}
