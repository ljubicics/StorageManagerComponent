package exception;

public class FileIsNotADirectoryException extends Exception{
    private String message="Error: File is not a directory.";

    @Override
    public String getMessage() {
        return message;
    }
}
