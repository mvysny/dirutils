package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Uses new file methods added in Java 7 and Android API level 26 (Android Oreo 8.0.0).
 * @author mvy
 */
public class Java7FS implements FileSystemSpi {
    private static final boolean AVAIL;
    static {
        boolean avail = true;
        try {
            Class.forName("java.nio.file.Files");
        } catch (ClassNotFoundException ex) {
            avail = false;
        }
        AVAIL = avail;
    }
    public static boolean isAvail() {
        return AVAIL;
    }

    @Override
    public Integer getMod(@NotNull String file) throws IOException {
        return null;
    }

    @Override
    public boolean rename(@NotNull File source, @NotNull File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException ex) {
            return false;
        }
    }

    @Override
    public void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        try {
            Files.delete(fileOrEmptyDirectory.toPath());
        } catch (DirectoryNotEmptyException ex) {
            throw new IOException("Failed to delete '" + fileOrEmptyDirectory + "' because it's not empty", ex);
        } catch (IOException ex) {
            throw new IOException("Failed to delete '" + fileOrEmptyDirectory + "': " + ex.getMessage(), ex);
        }
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        try {
            Files.createDirectory(directory.toPath());
        } catch (FileAlreadyExistsException ex) {
            throw new IOException("'" + directory + "' points to a file", ex);
        } catch (IOException ex) {
            throw new IOException("The directory '" + directory + "' couldn't be created: " + (directory.getParentFile().canWrite() ? directory.getParent() + " not writable" : "unknown"), ex);
        }
    }
}
