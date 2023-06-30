package exception;

public class ForbiddenExtException extends Exception{
    private String message="Error: Extension is forbidden.";

    @Override
    public String getMessage() {
        return message;
    }
}
