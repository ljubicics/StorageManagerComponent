package exception;

public class FileNotLoadedException extends Exception{
    private String message="Error: File could not be loaded.";

    @Override
    public String getMessage() {
        return message;
    }
}
