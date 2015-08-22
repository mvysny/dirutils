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
    public Integer getMod(@NotNull String file) throws IOException {
        return null;
    }

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
        if (!directory.mkdir()) {
            throw new IOException("Failed to create " + directory + " for unknown reason");
        }
    }
}
