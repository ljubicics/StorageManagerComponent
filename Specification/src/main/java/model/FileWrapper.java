package model;

public class FileWrapper {
    String name;
    String path;
    String creationTime;
    String modTime;
    long size;

    public FileWrapper(String name, String path, String creationTime, String modTime, long size) {
        this.name = name;
        this.path = path;
        this.creationTime = creationTime;
        this.modTime = modTime;
        this.size = size;
    }

    public FileWrapper() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
    }

    public String getModTime() {
        return modTime;
    }

    public void setModTime(String modTime) {
        this.modTime = modTime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "name='" + name +
                ", path='" + path +
                ", creationTime='" + creationTime +
                ", modTime='" + modTime +
                ", size=" + size;
    }
}
