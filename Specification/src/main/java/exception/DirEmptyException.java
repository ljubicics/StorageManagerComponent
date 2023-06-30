package exception;

public class DirEmptyException extends Exception{
    private String message= "Error: Directory is empty.";

    @Override
    public String getMessage() {
        return message;
    }
}
