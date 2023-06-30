package exception;

public class MoveOutOfStorageException extends Exception{
    private String message="Error: Cant move out of storage";

    @Override
    public String getMessage() {
        return message;
    }
}
