import exception.*;
import model.FileWrapper;

import java.util.List;

public interface SpecificationInterface {

    /**
     * Creates storage on given path with chosen configuration of storage
     * Configuration of storage represents size, forbidden extensions and number of files/directories in storage
     * @param path
     * @param size
     * @param configuration
     */

    void createStorage(String path,long size, String[] configuration);

    /**
     * Creates directory on forwarded path with given name
     *
     * @param dirName
     * @param numberOfFilesOrDirs
     * @throws AlreadyExistsException
     * @throws MaxNumberOfFilesException
     * @throws StorageMaxSizeException
     */
    void createDirectory(String dirName, Integer ... numberOfFilesOrDirs) throws AlreadyExistsException, MaxNumberOfFilesException, StorageMaxSizeException;

    /**
     * Creates a new file with given filename in current directory
     *
     * @param fileName
     * @throws AlreadyExistsException
     * @throws MaxNumberOfFilesException
     * @throws ForbiddenExtException
     */
    void createFile(String fileName) throws AlreadyExistsException, MaxNumberOfFilesException, ForbiddenExtException;

    /**
     * Deletes file or directory with passed name in directory
     *
     * @param fileOrDirName
     * @throws FileNotFoundException
     */
    void deleteFileOrDir(String fileOrDirName) throws FileNotFoundException;

    /**
     * Moves selected file to a folder with given path
     *
     * @param fileName
     * @param path
     */
    void moveFile(String fileName, String path);

    /**
     * Downloads selected file to a given path
     *
     * @param fileName
     * @return Returns true if file/directory is successfully downloaded otherwise it returns false
     */
    boolean downloadFileOrDirectory(String fileName);

    /**
     * Renames file or directory on given path
     * New name is passed as a second argument in method
     *
     * @param path
     * @param newName
     */
    void renameFileOrDirectory(String path, String newName);

    /**
     * Returns list (of names) of files and directories saved on current path.
     * If extension exists, then method returns files with wanted extension.
     *
     * @param extensions
     * @return List that contains names of files in chosen directory
     * @throws DirEmptyException
     */
    List<FileWrapper> listFilesFromDirectory(String ... extensions) throws DirEmptyException; //vraca se naziv i meta podaci

    /**
     * Returns all files from all directories from chosen directory
     *
     * @param path
     * @return List that contains all files
     */
    List<FileWrapper> listAllFilesFromAllDirs(String path); //vraca sve fajlove

    /**
     * Returns list of files that have given extension.
     *
     * @param extension
     * @return
     */
    List<FileWrapper> listFilesForExtension(String extension);

    /**
     * Returns list of files that begin, contain or end with given substring
     *
     * @param substring
     * @return
     */
    List<FileWrapper> listFilesWithSubstring(String substring);

    /**
     * Returns true or false if directory with given path contains file or list of files
     *
     * @param path
     * @param substring
     * @return
     * @throws FileNotFoundException
     * @throws FileIsNotADirectoryException
     */
    boolean dirContains(String path, String ... substring) throws FileNotFoundException, FileIsNotADirectoryException;

    /**
     * Returns name of directory which contains file with given file name.
     *
     * @param fileName
     * @return
     * @throws FileNotFoundException
     */
    String findParentDirectory(String fileName) throws FileNotFoundException;

    /**
     * Sorts files and directories in given order by chosen criteria.
     *
     * @param order
     * @param criteria
     * @return
     */
    List<FileWrapper> sortFilesAndDirs(String order, String ... criteria);

    /**
     * Returns modified files in directory passed by its path.
     *
     * @param date
     * @return
     */
    List<FileWrapper> returnModFiles(String date);

    /**
     * Opens directory with given file name that is inside of current directory
     *
     * @param fileName
     * @throws FileIsNotADirectoryException
     * @throws DirectoryNotFoundException
     */
    void forward(String fileName) throws FileIsNotADirectoryException, DirectoryNotFoundException;

    /**
     * Goes to parent dir.
     *
     * @throws MoveOutOfStorageException
     */
    void backward() throws MoveOutOfStorageException;

    /**
     * Uploads file from user directory on system
     *
     * @param name
     * @return
     */
    boolean uploadFile(String name);
}
