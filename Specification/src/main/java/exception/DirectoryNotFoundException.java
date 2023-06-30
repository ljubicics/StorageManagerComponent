package exception;


public class DirectoryNotFoundException extends Exception {
    private String message= "Error: Directory does not exist";

    @Override
    public String getMessage() {
        return message;
    }
}
