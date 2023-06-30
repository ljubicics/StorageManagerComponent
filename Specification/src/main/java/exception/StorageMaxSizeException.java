package exception;

public class StorageMaxSizeException extends Exception{
    private String message = "Error: Storage reached max size.";

    @Override
    public String getMessage() {
        return message;
    }
}
