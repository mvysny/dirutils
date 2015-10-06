package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Basic operations every FS must implement. {@link sk.baka.android.DirUtils} uses this as its backend.
 * @author mvy
 */
public interface FileSystemSpi {
    /**
     * Returns the rwxrwxrwx mod for given file.
     *
     * @param file the file, absolute, not null.
     * @return mod or null if the file system does not provide such information.
     */
    Integer getMod(@NotNull String file) throws IOException;

    /**
     * Renames the file, overwriting the target file.
     * <p></p>
     * If the file cannot be renamed atomically (because target resides on a different mount point), return false.
     *
     * @param source source file, not null.
     * @param target target file, not null. Does not exist.
     * @return true on success, false ONLY IF target resides on a different mount point and needs to be copied.
     * @throws IOException on any I/O error
     */
    boolean rename(@NotNull File source, @NotNull File target) throws IOException;

    /**
     * Deletes given file or empty directory.
     *
     * @param fileOrEmptyDirectory the file/directory to delete, not null. Will be an absolute path (must start with a slash).
     * Will exist.
     * @throws IOException if the delete fails because given file does not exist, is not an empty dir, etc etc.
     */
    void delete(@NotNull File fileOrEmptyDirectory) throws IOException;
    /**
     * Creates given directory. Fails if the directory already exists. Does not create parent directory.
     * <p></p>
     * Fails if there already is a file with given name, or if the directory couldn't be created.
     * <p></p>
     * As opposed to dumb {@link File#mkdir()} this method throws {@link IOException} with informative message if the function fails.
     * @param directory the directory to create, not null. Must be an absolute path (must start with a slash). May exist, and may point to a file.
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    void mkdir(@NotNull File directory) throws IOException;
}