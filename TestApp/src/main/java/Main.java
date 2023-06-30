import exception.*;
import model.FileWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws ClassNotFoundException {
        //Class.forName("DriveImplementation");
        //Class.forName("LocalImplementation");
        String currentPath = "";
        Scanner input = new Scanner(System.in);
        String commandLine;
        SpecificationInterface local;
        System.out.println("Izaberite koju implementaciju zelite da koristite: ");
        commandLine = input.nextLine();
        String[] par = commandLine.split(" ");
        if(par[0].equals("local")) {
            Class.forName("LocalImplementation");
        } else if (par[0].equals("drive")) {
            Class.forName("DriveImplementation");
        }
        System.out.println("Unesite komandu:");
        while (true) {
            local = StorageManager.getStorage();
            commandLine = input.nextLine();
            String[] parameters = commandLine.split(" ");
            if (parameters[0].equals("initStorage")) {
                if (parameters.length == 1) {
                    System.out.println("Error: You must enter a path.");
                    continue;
                } else if (parameters.length == 2) {
                    local.createStorage(parameters[1], 0, null);
                } else if(parameters.length >= 3) {
                    String[] config  = new String[parameters.length - 3];;
                    for(int i = 3; i < parameters.length; i++) {
                        config[i-3] = parameters[i];
                    }
                    local.createStorage(parameters[1], Long.parseLong(parameters[2]), config);
                }
                System.out.println("Storage uspesno inicijalizovan");
            } else if(parameters[0].equals("mkdir")) {
                if(parameters.length == 1) {
                    System.out.println("Error: You must enter name of directory");
                    continue;
                } else if(parameters.length == 2) {
                    try {
                        local.createDirectory(parameters[1], null);
                    } catch (MaxNumberOfFilesException mE) {
                        System.out.println(mE.getMessage());
                    } catch (StorageMaxSizeException sE) {
                        System.out.println(sE.getMessage());
                    } catch (AlreadyExistsException aE) {
                        System.out.println(aE.getMessage());
                    }
                } else if(parameters.length == 3){
                    try {
                        local.createDirectory(parameters[1], Integer.valueOf(parameters[2]));
                    } catch (MaxNumberOfFilesException mE) {
                        System.out.println(mE.getMessage());
                    } catch (StorageMaxSizeException sE) {
                        System.out.println(sE.getMessage());
                    } catch (AlreadyExistsException aE) {
                        System.out.println(aE.getMessage());
                    }
                }
                System.out.println("Direktorijum kreiran");
            } else if(parameters[0].equals("touch")) {
                try {
                    local.createFile(parameters[1]);
                } catch (AlreadyExistsException e) {
                    System.out.println(e.getMessage());
                } catch (MaxNumberOfFilesException e) {
                    System.out.println(e.getMessage());
                } catch (ForbiddenExtException e) {
                    System.out.println(e.getMessage());
                }
                System.out.println("Fajl kreiran");
            } else if(parameters[0].equals("cd")) {
                if(parameters.length == 1) {
                    System.out.println("You must enter second parameter");
                } else if(parameters[1].equals("..")) {
                    try {
                        local.backward();
                    } catch (MoveOutOfStorageException e) {
                        System.out.println(e.getMessage());
                        continue;
                    }
                    System.out.println("Uspesno ste se vratili korak unazad");
                } else {
                    try {
                        local.forward(parameters[1]);
                    } catch (FileIsNotADirectoryException e) {
                        System.out.println(e.getMessage());
                        continue;
                    } catch (DirectoryNotFoundException e) {
                        System.out.println(e.getMessage());
                        continue;
                    }
                    System.out.println("Uspesno ste usli u direktorijum");
                }
            } else if(parameters[0].equals("rm")) {
                try {
                    local.deleteFileOrDir(parameters[1]);
                } catch (FileNotFoundException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
                System.out.println("Uspesno obrisano");
            } else if(parameters[0].equals("rename")) {
                local.renameFileOrDirectory(parameters[1], parameters[2]);
                System.out.println("Novi naziv fajla: " + parameters[2]);
            } else if(parameters[0].equals("move")) {
                local.moveFile(parameters[1], parameters[2]);
                System.out.println("Fajl premesten");
            } else if(parameters[0].equals("ls")) {
                List<FileWrapper> list=new ArrayList<>();
                if (parameters.length > 1) {
                    for (int i = 1; i < parameters.length; i++) {
                        try {
                            list.addAll(local.listFilesFromDirectory(parameters[i]));
                        } catch (DirEmptyException e) {
                            System.out.println(e.getMessage());
                            continue;
                        }
                    }
                } else {
                    try {
                        list = local.listFilesFromDirectory();
                    } catch (DirEmptyException e) {
                        System.out.println(e.getMessage());
                    }
                }
                for (FileWrapper l: list){
                    System.out.println(l.toString());
                }
                System.out.println("Unesite sledecu komandu: ");
            } else if(parameters[0].equals("download")) {
                boolean flag = local.downloadFileOrDirectory(parameters[1]);
                if(flag == true) {
                    System.out.println("File successfully downloaded");
                } else {
                    System.out.println("File couldn't be downloaded");
                }
            } else if(parameters[0].equals("findParent")) {
                String parent = null;
                try {
                    parent = local.findParentDirectory(parameters[1]);
                } catch (FileNotFoundException e) {
                    System.out.println(e.getMessage());
                }
                System.out.println(parent);
            } else if(parameters[0].equals("listSub")) {
                List<FileWrapper> list = new ArrayList<>();
                list = local.listFilesWithSubstring(parameters[1]);
                for (FileWrapper l: list){
                    System.out.println(l);
                }
            } else if(parameters[0].equals("dirContains")) {
                if(parameters.length == 3) {
                    try {
                        System.out.println(local.dirContains(parameters[1], parameters[2]));
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    } catch (FileIsNotADirectoryException e) {
                        System.out.println(e.getMessage());
                    }
                } else if(parameters.length > 3) {
                    String[] lof = new String[parameters.length - 2];
                    for (int i = 2; i < (parameters.length); i++) {
                        String p = parameters[i];
                        lof[i - 2] = p;
                    }
                    try {
                        System.out.println(local.dirContains(parameters[1], lof));
                    } catch (FileNotFoundException e) {
                        System.out.println(e.getMessage());
                    } catch (FileIsNotADirectoryException e) {
                        System.out.println(e.getMessage());
                    }
                }
            } else if(parameters[0].equals("sort")) {
                try {
                    List<FileWrapper> listOfFiles;
                    if (parameters.length == 1)
                        System.out.println("Error: You must enter asc or desc.");
                    if (parameters.length == 2) {
                        listOfFiles=local.sortFilesAndDirs(parameters[1]);
                    } else {
                        listOfFiles=local.sortFilesAndDirs(parameters[1], parameters[2]);
                    }
                    for (FileWrapper f: listOfFiles){
                        System.out.println(f);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    //System.out.println("Too many or too few arguments.");
                }
            } else if(parameters[0].equals("findExt")) {
                List<FileWrapper> filesWithExt = local.listFilesForExtension(parameters[1]);
                for (FileWrapper l: filesWithExt){
                    System.out.println(l);
                }
            } else if(parameters[0].equals("lsAll")) {
                List<FileWrapper> allFiles = local.listAllFilesFromAllDirs(null);
                for (FileWrapper l: allFiles){
                    System.out.println(l);
                }
            } else if(parameters[0].equals("lastMod")) {
                List<FileWrapper> allFiles = local.returnModFiles(parameters[1]);
                for (FileWrapper l: allFiles){
                    System.out.println(l);
                }
            } else if (parameters[0].equals("exit")) {
                break;
            }
        }
    }
}
