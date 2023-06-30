import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.File;
import exception.*;
import exception.FileNotFoundException;
import model.FileStorageModel;
import model.FileWrapper;

import java.awt.datatransfer.FlavorEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class DriveImplementation implements SpecificationInterface{
    private String osSeparator = java.io.File.separator;
    private Drive service;
    private FileStorageModel fileStorage = new FileStorageModel();
    private String storagesID;

    static {
        StorageManager.registerStorage(new DriveImplementation());
    }

    /**
     * Application name.
     */
    private static final String APPLICATION_NAME = "My project";

    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /**
     * Global instance of the JSON factory.
     */
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials at
     * ~/.credentials/calendar-java-quickstart
     */
    private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return an authorized Credential object.
     * @throws IOException
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in = DriveImplementation.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
                clientSecrets, SCOPES).setAccessType("offline").build();
        Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
        return credential;
    }

    /**
     * Build and return an authorized Calendar client service.
     *
     * @return an authorized Calendar client service
     * @throws IOException
     */
    public static Drive getDriveService() throws IOException {
        Credential credential = authorize();
        return new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    @Override
    public void createStorage(String path, long size, String[] configuration) {
        this.service = null;
        try {
            this.service = getDriveService();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
        FileList list = null;
        try {
            list = service.files().list()
                    .setPageSize(10)
                    .setFields("nextPageToken, files(id, name, createdTime, modifiedTime, size)")
                    .execute();
        } catch (IOException e){
            throw new RuntimeException(e);
        }
        List<File> files = list.getFiles();
        if(files == null || files.isEmpty()) {
            System.out.println("No files found");
        } else {
            for (File f : files) {
                if(f.getName().equals("Storages")) {
                    storagesID = f.getId();
                    initStorage(path, size, configuration);
                    break;
                }
            }
        }
        if(storagesID == null){
            File fileMetadata = new File();
            fileMetadata.setName("Storages");
            fileMetadata.setMimeType("application/vnd.google-apps.folder");
            try {
                File f= service.files().create(fileMetadata)
                        .setFields("id,createdTime,size")
                        .execute();
                storagesID = f.getId();
            }catch (IOException e){
                System.out.println("ne radi");
            }
            initStorage(path, size, configuration);
        }
    }
    public void initStorage(String path, long size, String[] configuration) {
        long maxSize = 0;
        int maxFilesInDir = 0;
        List<String> forbiddenExtensions = new ArrayList<>();
        boolean flag = false;
        if(configuration != null) {
            for (int i = 0; i < configuration.length; i++) {
                if ((configuration[i].charAt(0)) == '.') {
                    forbiddenExtensions.add(configuration[i]);
                } else {
                    maxFilesInDir = Integer.parseInt( configuration[i]);
                }
            }
            maxSize = size;
        }

        FileList result = listFilesFromDirectoryPom(storagesID);
        if(result != null) {
            List<File> list = result.getFiles();
            for (File f : list) {
                if (f.getName().equals(path)) {
                    flag = true;
                }
            }
        }
        // Ako nismo imali storages folder u kom ce se nalaziti nasi "storidzi" posle njega kreiramo i storage koji zelimo u njemu.
        if(!flag) {
            File newStorageMetadata = new File();
            newStorageMetadata.setName(path);
            newStorageMetadata.setParents(Collections.singletonList(storagesID));
            newStorageMetadata.setMimeType("application/vnd.google-apps.folder");
            File file;
            try {
                file = service.files().create(newStorageMetadata)
                        .setFields("id,mimeType,parents,createdTime, modifiedTime,size")
                        .execute();
                fileStorage = new FileStorageModel.StorageBuilder().withRootPath(file.getId()).withMaxSize(maxSize).withExtensionRestrictions(forbiddenExtensions).withNumberOfFilesOrDirs(maxFilesInDir).build();
                fileStorage.setCurrentPath(file.getId());
                fileStorage.setRootPath(file.getId());
                writeToConfig(fileStorage);
            } catch (Exception e) {
                System.out.println("ne radi");
            }
        } else {
            // Ukoliko vec postoji zelimo da ucitamo u fileStorage njegove podatke smestamo u fileStorageModel
            fileStorage.setCurrentPath(getIdByName(path));
            fileStorage.setRootPath(getIdByName(path));
            List<String> exts = new ArrayList<>();
            FileList fileList = listFilesFromDirectoryPom(fileStorage.getRootPath());
            List<File> list = fileList.getFiles();
            String dataConfigID = null;
            for(File f : list) {
                if(f.getName().equals("data.config")) {
                    dataConfigID = f.getId();
                }
            }
            //String dataConfigID = getIdByName("data.config");
            String[] line;
            try {
                OutputStream outputStream = new ByteArrayOutputStream();
                service.files().get(dataConfigID)
                        .executeMediaAndDownloadTo(outputStream);
                String s = outputStream.toString();
                //System.out.println(s);
                line = s.split("\\R");
                maxSize = Long.parseLong(line[0]);
                //System.out.println(line[0]); // OVDE
                fileStorage.setMaxSize(maxSize);
                String[] extensions = line[1].split(" ");
                for(String ext : extensions) {
                    //System.out.println(ext);
                    exts.add(ext);
                }
                String[] numbIdAndNumber = line[2].split(",");
                HashMap<String, Integer> map = new HashMap<>();
                for(String str : numbIdAndNumber) {
                    String[] oneLine = str.split(";");
                    map.put(oneLine[0], Integer.parseInt(oneLine[1]));
                }
                fileStorage.setExtensionRestrictions(exts);
                fileStorage.setNumberOfFilesOrDirs(map);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void createDirectory(String dirName, Integer... numberOfFilesOrDirs)  throws AlreadyExistsException, MaxNumberOfFilesException, StorageMaxSizeException {
        File fileMetaData=new File();
        boolean flag = false;
        FileList list = listFilesFromDirectoryPom(fileStorage.getCurrentPath());
        List<File> files = list.getFiles();
        for (File f : files ) {
            if(f.getName().equals(dirName)){
                flag = true;
                //System.out.println("Directory already exists");
                throw new AlreadyExistsException();
            }
        }
        if(!flag) {
            if((fileStorage.getMaxSize() - 4096) >= 0) {
                fileStorage.setMaxSize(fileStorage.getMaxSize() - 4096);
                if((fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1) >= 0) {
                    fileStorage.getNumberOfFilesOrDirs().put(fileStorage.getCurrentPath(), fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1);
                    fileMetaData.setName(dirName);
                    fileMetaData.setMimeType("application/vnd.google-apps.folder");
                    fileMetaData.setParents(Collections.singletonList(fileStorage.getCurrentPath()));
                    try {
                        File f = service.files().create(fileMetaData)
                                .setFields("id,mimeType,parents,createdTime,modifiedTime,size")
                                .execute();
                        if(numberOfFilesOrDirs == null) {
                            fileStorage.getNumberOfFilesOrDirs().put(f.getId(), 1000);
                        } else {
                            fileStorage.getNumberOfFilesOrDirs().put(f.getId(), numberOfFilesOrDirs[0]);
                        }
                        writeToConfig(fileStorage);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else throw new MaxNumberOfFilesException();
            } else throw new StorageMaxSizeException();
        }

    }

    @Override
    public void createFile(String fileName) throws AlreadyExistsException, MaxNumberOfFilesException, ForbiddenExtException {
        List<String> extensions = fileStorage.getExtensionRestrictions();
        boolean flag = false;
        FileList list = listFilesFromDirectoryPom(fileStorage.getCurrentPath());
        List<File> files = list.getFiles();
        for (File s : files ) {
            if(s.getName().equals(fileName)){
                flag = true;
            }
        }
        if(flag) {
            //System.out.println("File with given name already exists");
            throw new AlreadyExistsException();
        }
        if((fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1) < 0) {
            //System.out.println("Maximum number of files is reached");
            //flag = true;
            throw new MaxNumberOfFilesException();
        }
        for(int i = 0; i < extensions.size(); i++) {
            if(fileName.endsWith(extensions.get(i))) {
                //flag = true;
                //System.out.println("This extension is forbidden in current storage");
                throw new ForbiddenExtException();
            }
        }
        String folderId = fileStorage.getCurrentPath();
        File fileMetadata = new File();
        fileMetadata.setName(fileName);
        //fileMetadata.setMimeType("application/vnd.google-apps.file");
        fileMetadata.setParents(Collections.singletonList(folderId));
        try {
            service.files().create(fileMetadata).setFields("id, parents, createdTime, modifiedTime,size").execute();
            fileStorage.getNumberOfFilesOrDirs().put(fileStorage.getCurrentPath(), fileStorage.getNumberOfFilesOrDirs().get(fileStorage.getCurrentPath()) - 1);
            writeToConfig(fileStorage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteFileOrDir(String fileOrDirName) throws FileNotFoundException {
        FileList fileList = listFilesFromDirectoryPom(fileStorage.getCurrentPath());
        List<File> list = fileList.getFiles();
        boolean flag = false;
        for(File f : list) {
            if(f.getName().equals(fileOrDirName)) {
                flag = true;
                try {
                    service.files().delete(f.getId()).execute();
                    writeToConfig(fileStorage);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
        if(!flag) throw new FileNotFoundException();
    }

    @Override
    public void moveFile(String fileName, String path) {
        String fileID = getIdByName(fileName);
        String newFolderID = getIdByName(path);
        File file = null;
        try {
            file = service.files().get(fileID).setFields("parents, createdTime, modifiedTime, size").execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        StringBuilder previousParents = new StringBuilder();
        for(String parent : file.getParents()){
            previousParents.append(parent);
            previousParents.append(',');
        }
        try{
            file = service.files().update(fileID, null)
                    .setAddParents(newFolderID)
                    .setRemoveParents(previousParents.toString())
                    .setFields("id, parents, createdTime, modifiedTime, size")
                    .execute();
        }catch (Exception e){
            e.printStackTrace();
        }
        file.setName(fileName);
        //System.out.println("Uspesno premesten fajl.");
    }

    @Override
    public boolean downloadFileOrDirectory(String fileName) { // IZMENITI JER NECE DA RADI
        String fileId = getIdByName(fileName);
        File f = null;
        try {
            f = service.files().get(fileId).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        java.io.File file = new java.io.File(System.getProperty("user.home") + osSeparator + fileName);
        try {
            OutputStream outputStream = new FileOutputStream(file);
            String mimeType = f.getMimeType();

            if(mimeType.endsWith("document")) {
                service.files().export(fileId, "application/pdf").executeMediaAndDownloadTo(outputStream);
            } else {
                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
            }
            outputStream.flush();
            outputStream.close();
            
        } catch (IOException e) {
        }
        return true;
    }

    @Override
    public void renameFileOrDirectory(String path, String newName) { 
        String id = getIdByName(path);
        try {
            File file = new File();
            file.setName(newName);

            Drive.Files.Update patchRequest = service.files().update(id, file);
            patchRequest.setFields("name");
            patchRequest.execute();
        } catch (IOException e) {
            //System.out.println("An error occurred: " + e);
            e.printStackTrace();
        }

    }
    @Override
    public List<FileWrapper> listFilesFromDirectory(String... extensions) throws DirEmptyException{
        List<FileWrapper> list = new ArrayList<>();
        FileList fileList = listFilesFromDirectoryPom(fileStorage.getCurrentPath());
        if(fileList != null) {
            List<File> files = fileList.getFiles();
            for (File file : files) {
                if (extensions.length != 0) {
                    for (String extension : extensions) {
                        if (file.getName().endsWith(extension)) {
                            FileWrapper fw = new FileWrapper(file.getName(), file.getId(), file.getCreatedTime().toString(), file.getModifiedTime().toString(), file.getSize());
                            list.add(fw);
                        }
                    }
                } else { // Ako nemamo prosledjene ekstenzije koje zelimo da se ispisu
                    FileWrapper fw = createFW(file);
                    list.add(fw);
                }
            }
        } else throw new DirEmptyException();
        return list;
    }

    @Override
    public List<FileWrapper> listAllFilesFromAllDirs(String path) {
        File f = null;
        List<FileWrapper> ans = new ArrayList<>();
        try {
            f = service.files().get(fileStorage.getRootPath()).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Queue<File> q = new ArrayDeque<>();
        q.add(f);
        while (!q.isEmpty()) {
            File curr = q.poll();
            FileList fileList = listFilesFromDirectoryPom(curr.getId());
            if(fileList != null) {
                List<File> list = fileList.getFiles();
                for (File ff : list) {
                    if(ff.getMimeType().endsWith("folder")) {
                        q.add(ff);
                    }
                    FileWrapper fw = createFW(ff);
                    ans.add(fw);
                }
            }
        }
        return ans;
    }

    @Override
    public List<FileWrapper> listFilesForExtension(String extension) {
        File f = null;
        List<FileWrapper> ans = new ArrayList<>();
        try {
            f = service.files().get(fileStorage.getRootPath()).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Queue<File> q = new ArrayDeque<>();
        q.add(f);
        while (!q.isEmpty()) {
            File curr = q.poll();
            FileList fileList = listFilesFromDirectoryPom(curr.getId());
            if(fileList != null) {
                List<File> list = fileList.getFiles();
                for (File ff : list) {
                    if(ff.getMimeType().endsWith("folder")) {
                        q.add(ff);
                    }
                    if(ff.getName().endsWith(extension)) {
                        FileWrapper fw = createFW(ff);
                        ans.add(fw);
                    }
                }
            }
        }
        return ans;
    }

    @Override
    public List<FileWrapper> listFilesWithSubstring(String substring) {
        File f = null;
        List<FileWrapper> ans = new ArrayList<>();
        try {
            f = service.files().get(fileStorage.getRootPath()).execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Queue<File> q = new ArrayDeque<>();
        q.add(f);
        while (!q.isEmpty()) {
            File curr = q.poll();
            FileList fileList = listFilesFromDirectoryPom(curr.getId());
            if(fileList != null) {
                List<File> list = fileList.getFiles();
                for (File ff : list) {
                    if (ff.getMimeType().endsWith("folder")) {
                        q.add(ff);
                    }
                    if(ff.getName().contains(substring)) {
                        FileWrapper fw = createFW(ff);
                        ans.add(fw);
                    }
                }
            }
        }
        return ans;
    }

    @Override
    public boolean dirContains(String path, String... listOfFiles) throws FileNotFoundException, FileIsNotADirectoryException{
        FileList fileList = listFilesFromDirectoryPom(fileStorage.getRootPath());
        List<File> list = fileList.getFiles();
        String directoryId = null;
        for(File f : list) {
            if(f.getName().equals(path)) {
                directoryId = f.getId();
            }
        }
        if(directoryId != null) {
            if(listOfFiles.length == 1) {
                FileList filesFromDir = listFilesFromDirectoryPom(directoryId);
                List<File> files = filesFromDir.getFiles();
                for(File f : files) {
                    if(f.getName().equals(listOfFiles[0])) {
                        return true;
                    }
                }
            } else if(listOfFiles.length > 1) {
                int brojac = listOfFiles.length;
                FileList filesFromDir = listFilesFromDirectoryPom(directoryId);
                List<File> files = filesFromDir.getFiles();
                List<String> checker = Arrays.asList(listOfFiles);
                for(File f : files) {
                    if (checker.contains(f.getName())) {
                        brojac--;
                    }
                }
                if(brojac == 0) {
                    return true;
                } else {
                    return false;
                }
            } else throw new FileNotFoundException();
        } else throw new FileIsNotADirectoryException();
        return false;
    }

    @Override
    public String findParentDirectory(String fileName) throws FileNotFoundException{
        FileList lista = listFilesFromDrive();
        List<File> fajlovi = lista.getFiles();
        String parentID = null;
        for(File f : fajlovi) {
            if(f.getName().equals(fileName)) {
                List<String> parents = f.getParents();
                parentID = parents.get(0);
            }
        }
        for(File ff : fajlovi) {
            if(ff.getId().equals(parentID)) {
                return ff.getName();
            }
        }
        throw new FileNotFoundException();
    }

    @Override
    public List<FileWrapper> sortFilesAndDirs(String order, String... criteria) {
        List<File> listOfFiles = new ArrayList<>();
        List <FileWrapper> listOfNames = new ArrayList<>();

        FileList result = null;

        if (order.equals("asc")){
            if (criteria.equals("date")){
                result = listFilesFromDriveSort("createdTime");
            } else result = listFilesFromDriveSort("name");


        } else if (order.equals("desc")) {
            result = listFilesFromDriveSort("createdTime desc");
        } else result = listFilesFromDriveSort("name desc");


        List<File> files = result.getFiles();
        if (files != null || !files.isEmpty()) {
            for (File file : files) {
                FileWrapper fw = createFW(file);
                listOfNames.add(fw);
            }
        }
        return listOfNames;
    }

    @Override
    public List<FileWrapper> returnModFiles(String date) {
        List<FileWrapper> ans = new ArrayList<>();
        List<FileWrapper> listaWrappera = new ArrayList<>();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            Date dateFromString = sdf.parse(date);
            File f = null;
            try {
                f = service.files().get(fileStorage.getRootPath()).execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Queue<File> q = new ArrayDeque<>();
            q.add(f);
            while (!q.isEmpty()) {
                File curr = q.poll();
                FileList fileList = listFilesFromDirectoryPom(curr.getId());
                if(fileList != null) {
                    List<File> list = fileList.getFiles();
                    for (File ff : list) {
                        if(ff.getMimeType().endsWith("folder")) {
                            q.add(ff);
                        }
                        FileWrapper fw = createFW(ff);
                        listaWrappera.add(fw);
                    }
                }
            }
            for (FileWrapper file : listaWrappera) {
                String currDateString = sdf.format(file.getModTime());
                Date currDate = sdf.parse(currDateString);
                if(currDate.compareTo(dateFromString) > 0) {
                    ans.add(file);
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        return ans;
    }

    @Override
    public void forward(String fileName) throws FileIsNotADirectoryException, DirectoryNotFoundException {
        FileList dirFiles = listFilesFromDirectoryPom(fileStorage.getCurrentPath());
        List<File> files = dirFiles.getFiles();
        boolean flag = false;
        for(File f : files) {
            if(f.getName().equals(fileName)) {
                flag = true;
                if(f.getMimeType().endsWith("folder")) {
                    fileStorage.setCurrentPath(f.getId());
                    break;
                } else throw new FileIsNotADirectoryException();
            }
        }
        if(!flag) throw new DirectoryNotFoundException();
    }

    @Override
    public void backward() throws MoveOutOfStorageException{
        if(!fileStorage.getCurrentPath().contentEquals(fileStorage.getRootPath())){
            String parentId= getParentId();
            fileStorage.setCurrentPath(parentId);
        }
        else throw new MoveOutOfStorageException();
    }

    @Override
    public boolean uploadFile(String name) {
        File metaData = new File();
        metaData.setName(name);
        metaData.setParents(Collections.singletonList(fileStorage.getCurrentPath()));

        java.io.File file = new java.io.File(System.getProperty("user.home") + osSeparator + name);
        FileContent mediaContent = new FileContent("text/plain", file);
        try {
            service.files().create(metaData,mediaContent).setFields("id, parents").execute();
            return  true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public FileList listFilesFromDirectoryPom(String directoryID){
        FileList lista = null;

        try {
            lista = service.files().list()
                    .setQ("'" + directoryID + "' in parents")
                    .setFields("nextPageToken, files(id, mimeType, name, parents, createdTime, modifiedTime, size)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lista;
    }
    public FileList listFilesFromDrive() {
        FileList lista = null;

        try {
            lista = service.files().list()
                    .setPageSize(50)
                    .setFields("nextPageToken, files(id, name, parents, createdTime, modifiedTime, size)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lista;
    }
    public String getIdByName(String filename){
        String fileId = "";
        FileList list = listFilesFromDrive();
        List<File> fajlovi = list.getFiles();
        for (File f : fajlovi) {
            try {
                if (f.getName().equals(filename)){
                    fileId=f.getId();
                }
            }catch (Exception e) {
                System.out.println("File with given name doesn't exist");
            }
        }
        return fileId;
    }
    public void writeToConfig(FileStorageModel fileStorageModel) {
        FileList fileList = listFilesFromDirectoryPom(fileStorageModel.getRootPath());
        if(fileList != null) {
            List<File> files = fileList.getFiles();
            for (File f : files) {
                if (f.getName().equals("data.config")) {
                    try {
                        service.files().delete(f.getId()).execute();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        java.io.File config = new java.io.File("config");
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(config));
            bw.write(Long.toString(fileStorageModel.getMaxSize()));
            bw.newLine();
            for(int i = 0; i < fileStorageModel.getExtensionRestrictions().size(); i++){
                bw.write(fileStorageModel.getExtensionRestrictions().get(i));
            }
            bw.newLine();
            HashMap<String, Integer> mapa = fileStorageModel.getNumberOfFilesOrDirs();
            for(Map.Entry<String, Integer> entry : mapa.entrySet()) {
                bw.write(entry.getKey());
                bw.write(";");
                bw.write(entry.getValue().toString());
                bw.write(",");
            }
            bw.newLine();
            bw.close();
            File metadata = new File();
            FileContent fileContent = new FileContent("text/plain", config);
            metadata.setName("data.config");
            metadata.setParents(Collections.singletonList(fileStorage.getRootPath()));
            try{
                service.files().create(metadata, fileContent)
                        .setFields("id, parents, createdTime, modifiedTime, size")
                        .execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public String getParentId(){
        String parentId=null;
        FileList lista = listFilesFromDrive();

        List<File> fajlovi = lista.getFiles();
        if (fajlovi != null || !fajlovi.isEmpty()) {
            for (File qq : fajlovi) {
                try {
                    if (qq.getId().contains(fileStorage.getCurrentPath()) ){

                        List<String> parents = qq.getParents();
                        parentId=parents.get(0);
                    } else if (qq.getId().contains(fileStorage.getRootPath()))
                        parentId=fileStorage.getRootPath();
                }catch (Exception e){
                }
            }
        }
        return parentId;
    }
    public FileList listFilesFromDriveSort(String order){
        FileList lista = null;

        try {
            lista = service.files().list().setQ("'" + fileStorage.getCurrentPath()+"' in parents").setOrderBy(order)
                    .setPageSize(50)
                    .setFields("nextPageToken, files(id, name, parents, mimeType, createdTime, modifiedTime, size)")
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lista;
    }

    public FileWrapper createFW(File file) {
        FileWrapper fw = null;
        if(file.getSize() == null) {
            fw = new FileWrapper(file.getName(), file.getId(), file.getCreatedTime().toString(), file.getModifiedTime().toString(), 0);
        } else {
            fw = new FileWrapper(file.getName(), file.getId(), file.getCreatedTime().toString(), file.getModifiedTime().toString(), file.getSize());
        }
        return fw;
    }
}
