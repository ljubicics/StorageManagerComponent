package model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FileStorageModel {
    private String rootPath;
    private String currentPath;
    private long maxSize;
    private List<String> extensionRestrictions;
    private HashMap<String, Integer> numberOfFilesOrDirs;

    public FileStorageModel() {
    }

    public FileStorageModel(String rootPath) {
        this.rootPath = rootPath;
        this.currentPath = rootPath;
        this.maxSize = 4096;
        this.numberOfFilesOrDirs = new HashMap<>();
        numberOfFilesOrDirs.put(rootPath, 1000);
        this.extensionRestrictions = new ArrayList<>();
    }

    public FileStorageModel(StorageBuilder fileStorageBuilder) {
        this.rootPath = fileStorageBuilder.rootPath;
        this.maxSize = fileStorageBuilder.maxSize;
        this.extensionRestrictions = fileStorageBuilder.extensionRestrictions;
        this.numberOfFilesOrDirs = fileStorageBuilder.numberOfFilesOrDirs;
        this.currentPath = this.rootPath;
    }

    public static class StorageBuilder {
        private String rootPath;
        private long maxSize;
        private List<String> extensionRestrictions;
        private HashMap<String, Integer> numberOfFilesOrDirs;

        public StorageBuilder withRootPath(String rootPath) {
            this.rootPath = rootPath;
            return this;
        }

        public StorageBuilder withMaxSize(long maxSize) {
            if(maxSize == 0) {
                this.maxSize = 100000;
            } else {
                this.maxSize = maxSize;
            }
            return this;
        }

        public StorageBuilder withExtensionRestrictions(List<String> extensionRestrictions) {
            this.extensionRestrictions = extensionRestrictions;
            return this;
        }

        public StorageBuilder withNumberOfFilesOrDirs(int number) {
            this.numberOfFilesOrDirs = new HashMap<>();
            if (number == 0) {
                numberOfFilesOrDirs.put(this.rootPath, 1000);
            } else {
                numberOfFilesOrDirs.put(this.rootPath, number);
            }
            return this;
        }

        public FileStorageModel build() {
            return new FileStorageModel(this);
        }
    }

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    public long getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public List<String> getExtensionRestrictions() {
        return extensionRestrictions;
    }

    public void setExtensionRestrictions(List<String> extensionRestrictions) {
        this.extensionRestrictions = extensionRestrictions;
    }

    public HashMap<String, Integer> getNumberOfFilesOrDirs() {
        return numberOfFilesOrDirs;
    }

    public void setNumberOfFilesOrDirs(HashMap<String, Integer> numberOfFilesOrDirs) {
        this.numberOfFilesOrDirs = numberOfFilesOrDirs;
    }
}
