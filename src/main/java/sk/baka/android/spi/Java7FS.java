package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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
        Files.delete(fileOrEmptyDirectory.toPath());
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        Files.createDirectory(directory.toPath());
    }
}
