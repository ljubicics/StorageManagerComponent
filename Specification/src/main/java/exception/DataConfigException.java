package exception;

public class DataConfigException extends Exception{
    private String message= "Error: data config not loaded.";

    @Override
    public String getMessage() {
        return message;
    }
}
