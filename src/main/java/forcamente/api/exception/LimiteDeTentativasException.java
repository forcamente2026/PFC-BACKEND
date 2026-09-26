package forcamente.api.exception;

public class LimiteDeTentativasException extends RuntimeException {
    public LimiteDeTentativasException(String message) {
        super(message);
    }
}
