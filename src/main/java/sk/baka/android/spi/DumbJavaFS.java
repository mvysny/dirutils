package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Dumb FS which fails to provide informative message. Always available.
 * @author mvy
 */
public class DumbJavaFS implements FileSystemSpi {
    @Override
    public boolean rename(@NotNull File source, @NotNull File target) throws IOException {
        return source.renameTo(target);
    }

    @Override
    public void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        if (!fileOrEmptyDirectory.delete()) {
            throw new IOException("Failed to delete " + fileOrEmptyDirectory + " for unknown reason");
        }
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        if (directory.exists() && !directory.isDirectory()) {
            throw new IOException("'" + directory + "' points to a file");
        }
        // mkdir, not mkdirs() - according to the contract, FileSystemSpi.mkdir() only creates one dir.
        if (!directory.exists() && !directory.mkdir()) {
            throw new IOException("The directory '" + directory + "' couldn't be created: " + (directory.getParentFile().canWrite() ? directory.getParent() + " not writable" : "unknown"));
        }
    }
}
