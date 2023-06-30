import exception.*;
import exception.FileNotFoundException;
import model.FileStorageModel;
import model.FileWrapper;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.apache.commons.io.FileUtils.deleteDirectory;

public class LocalImplementation implements SpecificationInterface{
    private FileStorageModel fileStorage = new FileStorageModel();
    private String osSeparator = File.separator;
    private String path = "";

    static {
        StorageManager.registerStorage(new LocalImplementation());
    }

    @Override
    public void createStorage(String path, long size, String[] configuration) {
        String storageName = path;
        long maxSize = 0;
        int maxFilesInDir = 0;
        List<String> forbiddenExtensions = new ArrayList<>();
        path = System.getProperty("user.home") + osSeparator + path;
        // Provera da li vec postoji fileStorage koji zelimo da napravimo
        File file = new File(System.getProperty("user.home"));
        String[] children = file.list();
        boolean exists = false;
        for(int i = 0; i < children.length; i++) {
            if(children[i].equals(storageName)) {
                exists = true;
            }
        }
        //Ukoliko ne postoji pravimo novi fileStorage
        if (!exists) {
            maxSize = size;
            if(configuration != null) {
                for(int i = 0; i < configuration.length; i++) {
                    if((configuration[i].charAt(0)) == '.') {
                        forbiddenExtensions.add(configuration[i]);
                    } else {
                        maxFilesInDir = Integer.parseInt(configuration[i]);
                    }
                }
            }
            fileStorage = new FileStorageModel.StorageBuilder().withRootPath(path).withMaxSize(maxSize).withExtensionRestrictions(forbiddenExtensions).withNumberOfFilesOrDirs(maxFilesInDir).build();
            fileStorage.setCurrentPath(path);

            File storageFile = new File(path);
            storageFile.mkdir();
            try {
                File f = new File(fileStorage.getCurrentPath() + osSeparator + "data.config");
                f.createNewFile();
                FileWriter fw = new FileWriter(f);
                BufferedWriter writer = new BufferedWriter(fw);
                writer.write(Long.toString(fileStorage.getMaxSize()));
                writer.newLine();
                for (int i = 0; i < forbiddenExtensions.size(); i++) {
                    writer.write(forbiddenExtensions.get(i) + ",");
                }
                writer.newLine();
                writer.write(fileStorage.getRootPath() + ";" + fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getRootPath()) + ","); // U u fajlu su parovi iz mape razdvojeni sa ; !!!
                writer.newLine();
                writer.close();
                //System.out.println("Storage successfuly created on path: " + fileStorage.getCurrentPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Ukoliko postoji zelimo da u fileStorageModel ucitamo konfiguraciju postojeceg storage-a
        } else {
            // U fileStorage upisujemo vrednosti od trenutno aktivnog storage-a
            fileStorage.setRootPath(path);
            fileStorage.setCurrentPath(path);

            try {
                File f = new File(path + osSeparator + "data.config");
                FileReader fr = new FileReader(f);
                BufferedReader reader = new BufferedReader(fr);
                fileStorage.setMaxSize(Long.parseLong(reader.readLine()));
                List<String> extensions = Arrays.asList(reader.readLine().split(",")); // Pravi listu zabranjenih ekstenzija
                fileStorage.setExtensionRestrictions(extensions);
                String[] pathsAndSizes = reader.readLine().split(","); // Splituje poslednji red u data.config fajlu na parove kljuc vrednost razdvojene sa dve tacke
                HashMap<String, Integer> map = new HashMap<>();
                for(int i = 0; i < pathsAndSizes.length; i++) {
                    String[] keyValue = new String[10000];
                    keyValue = pathsAndSizes[i].split(";");
                    map.put(keyValue[0], Integer.parseInt(keyValue[1]));
                }
                fileStorage.setNumberOfFilesOrDirs(map);
            } catch (Exception e) {
                //System.out.println("Nije uspesno ucitan data.config file iz storage-a");
                e.printStackTrace();
            }
        }
    }

    public void rewriteConfig() {
        try {
            File f = new File(fileStorage.getRootPath() + osSeparator + "data.config");
            f.delete();
            List<String> forbiddenExtensions = fileStorage.getExtensionRestrictions();
            File file = new File(fileStorage.getRootPath() + osSeparator + "data.config");
            file.createNewFile();
            FileWriter fw = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(fw);
            writer.write(Long.toString(fileStorage.getMaxSize()));
            writer.newLine();
            for (int i = 0; i < forbiddenExtensions.size(); i++) {
                writer.write(forbiddenExtensions.get(i) + ",");
            }
            writer.newLine();
            for(Map.Entry<String,Integer> mapElement : fileStorage.getNumberOfFilesOrDirs().entrySet()) {
                writer.write(mapElement.getKey() + ";" + mapElement.getValue() + ","); // U u fajlu su parovi iz mape razdvojeni sa ; !!!
            }
            writer.newLine();
            writer.close();
        } catch (Exception e) {
            //System.out.println("Nije uspesno ucitan data.config file iz storage-a");
            e.printStackTrace();
        }
    }

    @Override
    public void createDirectory(String dirName, Integer... numberOfFilesOrDirs) throws AlreadyExistsException, StorageMaxSizeException, MaxNumberOfFilesException {
        File f = new File(fileStorage.getCurrentPath());
        System.out.println(fileStorage.getCurrentPath());
        String[] children = f.list();
        boolean flag = false;
        for(int i = 0; i < children.length; i++) {
            if(children[i].equals(dirName)) {
                flag = true;
                //System.out.println("Directory with given name already exists");
                throw new AlreadyExistsException();
            }
        }
        if(!flag) {
            if((fileStorage.getMaxSize() - 4096) >= 0) {
                fileStorage.setMaxSize(fileStorage.getMaxSize() - 4096);
                System.out.println(fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()));
                if((fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1) >= 0) {
                    fileStorage.getNumberOfFilesOrDirs().put(fileStorage.getCurrentPath(), fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1);
                    File newDir = new File(fileStorage.getCurrentPath() + osSeparator + dirName);
                    fileStorage.getNumberOfFilesOrDirs().put(newDir.getPath(), numberOfFilesOrDirs[0]);
                    newDir.mkdir();
                    rewriteConfig();
                } else throw new MaxNumberOfFilesException();
            } else throw new StorageMaxSizeException();
        }
    }

    @Override
    public void createFile(String fileName) throws AlreadyExistsException, MaxNumberOfFilesException, ForbiddenExtException {
        List<String> extensions = fileStorage.getExtensionRestrictions();
        boolean flag = false;
        // Da li vec postoji fajl sa istim imenom u trenutnom direktorijumu
        flag = parentContains(fileName, fileStorage.getCurrentPath());
        if(flag) {
            //System.out.println("File with given name already exists");
            throw new AlreadyExistsException();
        }
        // Da li je ekstenzija fajla zabranjena u storage-u
        for(int i = 0; i < extensions.size(); i++) {
            if(fileName.endsWith(extensions.get(i))) {

                //System.out.println("This extension is forbidden in current storage");
                throw new ForbiddenExtException();
            }
        }
        // Da li je dostignut maksimum broja fajlova u trenutnom direktorijumu
        if((fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1) < 0) {
            //System.out.println("Maximum number of files is reached");
            throw new MaxNumberOfFilesException();
        }
        // Pravljenje novog fajla
        if(!flag) {
            try {
                File f = new File(fileStorage.getCurrentPath() + osSeparator + fileName);
                fileStorage.getNumberOfFilesOrDirs().put(fileStorage.getCurrentPath(), fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1);
                f.createNewFile();
                rewriteConfig();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void deleteFileOrDir(String fileOrDirName) throws FileNotFoundException {
        boolean flag = false;
        flag = parentContains(fileOrDirName, fileStorage.getCurrentPath());
        if(flag) {
            File f = new File(fileStorage.getCurrentPath() + osSeparator + fileOrDirName);
            /* Rekurzivno u dubinu proveravati da li postoji direktorijum
               Dokle god postoji uci u njega i iz file storage mape obrisati key i value za njegovu putanju
             */
            if(f.isDirectory()) {
                fileStorage.getNumberOfFilesOrDirs().put(f.getParentFile().getPath(), fileStorage.getNumberOfFilesOrDirs().get(f.getParentFile().getPath()) + 1);
                fileStorage.getNumberOfFilesOrDirs().remove(f.getPath());
                fileStorage.setMaxSize(fileStorage.getMaxSize() + 4096);
                deleteDirectory(f);
            }
            long fileSize = f.length();
            if(f.delete()) {
                fileStorage.setMaxSize(fileStorage.getMaxSize() + fileSize);
            } else throw new FileNotFoundException();
        } else throw new FileNotFoundException();
    }
    public void deleteDirectory(File file) {
        for(File subFile : file.listFiles()) {
            if(subFile.isDirectory()) {
                deleteDirectory(subFile);
                fileStorage.getNumberOfFilesOrDirs().remove(subFile.getPath());
            }
            long fileSize = subFile.length();
            if(subFile.delete()) {
                fileStorage.setMaxSize(fileStorage.getMaxSize() + fileSize);
            }
        }
    }

    @Override
    public void moveFile(String fileName, String path) {
        String newPath = fileStorage.getRootPath() + osSeparator + path;
        try {
            Files.move(Paths.get(fileStorage.getCurrentPath() + osSeparator + fileName), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {

        }
    }

    @Override
    public boolean downloadFileOrDirectory(String fileName) {
        File file = new File(fileStorage.getCurrentPath() + osSeparator + fileName);
        Path downloadDir = Paths.get(System.getProperty("user.home") + osSeparator + "StorageDownloads");
        if (!Files.exists(downloadDir)) {
            File newDir = new File(System.getProperty("user.home") + osSeparator + "StorageDownloads");
            newDir.mkdir();
        }
        if (!file.isDirectory()) {
            try {
                Files.copy(Paths.get(fileStorage.getCurrentPath() + osSeparator + fileName),
                        downloadDir.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }else {
            try {
                FileUtils.copyDirectory(file, new File(System.getProperty("user.home") +
                        osSeparator + "StorageDownloads" + osSeparator + fileName));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void renameFileOrDirectory(String path, String newName) {
        File f = new File(fileStorage.getCurrentPath() + osSeparator + path);
        File newFile = new File(fileStorage.getCurrentPath() + osSeparator + newName);
        f.renameTo(newFile);
    }

    @Override
    public List<FileWrapper> listFilesFromDirectory(String... extensions) throws DirEmptyException {
        List<FileWrapper> fileNames = new ArrayList<>();
        File f = new File(fileStorage.getCurrentPath());
        File[] files = f.listFiles();
        if (files.length != 0) {
            for(File file : files) {
                if (extensions.length != 0) {
                    for(String extension : extensions) {
                        if(file.getName().endsWith(extension)) {
                            try {
                                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                                FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                                fileNames.add(fw);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                } else { // Ako nemamo prosledjene ekstenzije koje zelimo da se ispisu
                    try {
                        BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                        fileNames.add(fw);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else throw new DirEmptyException();
        return fileNames;
    }

    @Override
    public List<FileWrapper> listAllFilesFromAllDirs(String path) {
        List<FileWrapper> allFiles = new ArrayList<>();
        if(path == null) {
            File f = new File(fileStorage.getCurrentPath());
            // BFS
            Queue<File> queue = new ArrayDeque<>();
            queue.add(f);
            while(!queue.isEmpty()) {
                File current = queue.poll();
                File[] listOfFilesAndDirectory = current.listFiles();
                if (listOfFilesAndDirectory != null) {
                    for (File file: listOfFilesAndDirectory) {
                        if (file.isDirectory()) {
                            queue.add(file);
                        } else {
                            try {
                                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                                FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                                allFiles.add(fw);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        } else {
            File f = new File(path);
            Queue<File> queue = new ArrayDeque<>();
            queue.add(f);
            while(!queue.isEmpty()) {
                File current = queue.poll();
                File[] listOfFilesAndDirectory = current.listFiles();
                if (listOfFilesAndDirectory != null) {
                    for (File file: listOfFilesAndDirectory) {
                        if (file.isDirectory()) {
                            queue.add(file);
                        } else {
                            try {
                                BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                                FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                                allFiles.add(fw);
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }
        }
        return allFiles;
    }

    @Override
    public List<FileWrapper> listFilesForExtension(String extension) {
        List<FileWrapper> files = new ArrayList<>();
        Queue<File> queue = new ArrayDeque<>();
        queue.add(new File(fileStorage.getRootPath()));
        while (!queue.isEmpty()) {
            File current = queue.poll();
            File[] listOfFilesAndDirectory = current.listFiles();
            if (listOfFilesAndDirectory != null) {
                for (File file: listOfFilesAndDirectory) {
                    if(file.getName().endsWith(extension)) {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                            files.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (file.isDirectory()) {
                        queue.add(file);
                    }
                }
            }
        }
        return files;
    }

    @Override
    public List<FileWrapper> listFilesWithSubstring(String substring) {
        List<FileWrapper> files = new ArrayList<>();
        Queue<File> queue = new ArrayDeque<>();
        queue.add(new File(fileStorage.getRootPath()));
        while (!queue.isEmpty()) {
            File current = queue.poll();
            File[] listOfFilesAndDirectory = current.listFiles();
            if (listOfFilesAndDirectory != null) {
                for (File file: listOfFilesAndDirectory) {
                    if(file.getName().contains(substring)) {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                            files.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (file.isDirectory()) {
                        queue.add(file);
                    }
                }
            }
        }
        return files;
    }

    @Override
    public boolean dirContains(String path, String... substring) throws FileNotFoundException, FileIsNotADirectoryException {
        File f = new File(fileStorage.getRootPath() + osSeparator + path);
        List<String> children = Arrays.asList(f.list());
        if (children != null) throw new FileIsNotADirectoryException();
        if(substring.length == 1) {
            for (String s : children) {
                if (s.equals(substring))
                    return true;
            }
        } else if(substring.length > 1) {
            List<String> files = new ArrayList<>();
            for(String sub : substring) {
                if(children.contains(sub)) {
                    continue;
                } else {
                    files.add(sub);
                }
            }
            if(files.isEmpty()) {
                return true;
            } else {
                return false;
            }
        } else throw new FileNotFoundException();
        return false;
    }

    @Override
    public String findParentDirectory(String fileName) throws FileNotFoundException{
        Queue<File> queue = new ArrayDeque<>();
        queue.add(new File(fileStorage.getRootPath()));
        while (!queue.isEmpty()) {
            File current = queue.poll();
            File[] listOfFilesAndDirectory = current.listFiles();
            if (listOfFilesAndDirectory != null) {
                for (File file: listOfFilesAndDirectory) {
                    if(file.getName().equals(fileName)) {
                        return current.getPath();
                    }
                    if (file.isDirectory()) {
                        queue.add(file);
                    }
                }
            }
        }
        throw new FileNotFoundException();
    }

    @Override
    public List<FileWrapper> sortFilesAndDirs(String order, String... criteria) {
        List <FileWrapper> listOfNames=new ArrayList<>();
        File file = new File(fileStorage.getCurrentPath());
        File[] list = file.listFiles();
        if (criteria.length == 0) {
            if (order.equals("asc")) {
                Arrays.sort(list);
                for (File f: list){
                    try {
                        BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                        FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                        listOfNames.add(fw);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            else if (order.equals("desc")) {
                Arrays.sort(list, Collections.reverseOrder());
                for (File f: list){
                    try {
                        BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                        FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                        listOfNames.add(fw);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        } else {
            if (criteria[0].equals("date")) {
                if (order.equals("asc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::lastModified));
                    for (File f: list){
                        try {
                            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                            listOfNames.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                else if (order.equals("desc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::lastModified).reversed());
                    for (File f: list){
                        try {
                            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                            listOfNames.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            } else if (criteria[0].equals("size")) {
                if (order.equals("asc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::length));
                    for (File f: list){
                        try {
                            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                            listOfNames.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
                else if (order.equals("desc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::length).reversed());
                    for (File f: list){
                        try {
                            BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(f.getName(), f.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), f.length());
                            listOfNames.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return listOfNames;
    }

    @Override
    public List<FileWrapper> returnModFiles(String date) {
        List<FileWrapper> lista = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            Date dateFromString = sdf.parse(date);
        File f = new File(fileStorage.getCurrentPath());
        Queue<File> queue = new ArrayDeque<>();
        queue.add(f);
        while (!queue.isEmpty()) {
            File current = queue.poll();
            File[] listOfFilesAndDirectory = current.listFiles();
            if (listOfFilesAndDirectory != null) {
                for (File file: listOfFilesAndDirectory) {
                    String currDateString = sdf.format(file.lastModified());
                    Date currDate = sdf.parse(currDateString);
                    if(currDate.compareTo(dateFromString) > 0) {
                        try {
                            BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                            FileWrapper fw = new FileWrapper(file.getName(), file.getPath(), attr.creationTime().toString(), attr.lastModifiedTime().toString(), file.length());
                            lista.add(fw);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return lista;
    }

    @Override
    public void forward(String fileName) throws FileIsNotADirectoryException, DirectoryNotFoundException {
        boolean flag = parentContains(fileName, fileStorage.getCurrentPath());
        if(flag) {
            File f = new File(fileStorage.getCurrentPath() + osSeparator + fileName);
            if(f.isDirectory()) {
                fileStorage.setCurrentPath(f.getPath());
                System.out.println(fileStorage.getCurrentPath());
            } else throw new FileIsNotADirectoryException();
        } else throw new DirectoryNotFoundException();
    }

    @Override
    public void backward() throws MoveOutOfStorageException {
        if(!fileStorage.getCurrentPath().equals(fileStorage.getRootPath())) {
            fileStorage.setCurrentPath(fileStorage.getCurrentPath().substring(0, fileStorage.getCurrentPath().lastIndexOf(osSeparator)));
            System.out.println(fileStorage.getCurrentPath());
        } else throw new MoveOutOfStorageException();
    }

    @Override
    public boolean uploadFile(String name) {
        return true;
    }
    public boolean parentContains(String name, String path) {
        File parent = new File(path);
        String[] children = parent.list();
        for (int i = 0; i < children.length; i++) {
            if(children[i].equals(name)) {
                return true;
            }
        }
        return false;
    }
}
