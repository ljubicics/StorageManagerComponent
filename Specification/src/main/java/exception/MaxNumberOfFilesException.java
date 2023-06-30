package exception;

public class MaxNumberOfFilesException extends Exception{
    private String message="Error: Max number of files reached.";

    @Override
    public String getMessage() {
        return message;
    }
}
