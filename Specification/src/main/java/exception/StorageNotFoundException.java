package exception;

public class StorageNotFoundException extends Exception{
private String message= "Error: Storage not found.";
@Override
    public String getMessage(){
    return message;
}

}
