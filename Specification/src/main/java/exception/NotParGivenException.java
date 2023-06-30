package exception;

public class NotParGivenException extends Exception{
    private String message= "Error: Parametar not given";

    @Override
    public String getMessage() {
        return message;
    }
}
