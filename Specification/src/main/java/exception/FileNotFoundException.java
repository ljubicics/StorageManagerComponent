package exception;

public class FileNotFoundException extends Exception{
    private String message = "Error: File not found.";

    @Override
    public String getMessage() {
        return message;
    }
}
