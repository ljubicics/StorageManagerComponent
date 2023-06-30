package exception;

public class AlreadyExistsException extends Exception{
    String message= "Error: Folder/File already exists.";

    @Override
    public String getMessage() {
        return message;
    }
}
