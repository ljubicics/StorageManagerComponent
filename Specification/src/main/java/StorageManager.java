public class StorageManager {
    private static SpecificationInterface specificationInterface;

    public static void registerStorage(SpecificationInterface sp) {
        specificationInterface = sp;
    }

    public static SpecificationInterface getStorage() {
        return specificationInterface;
    }
}