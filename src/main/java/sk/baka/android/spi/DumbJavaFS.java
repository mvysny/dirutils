package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * Backend on plain {@link File}, which answers only {@code false}; the reasons are guessed by
 * {@link FailureReason}. Always available.
 * @author mvy
 */
public class DumbJavaFS implements FileSystemSpi {
    @Override
    public boolean rename(@NotNull File source, @NotNull File target) throws IOException {
        if (source.renameTo(target)) {
            return true;
        }
        final String why = FailureReason.ofRename(source, target);
        if (why != null) {
            throw new IOException("Failed to rename '" + source + "' to '" + target + "': " + why);
        }
        // nothing File can see explains it, so assume a different mount point
        return false;
    }

    @Override
    public void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        if (!fileOrEmptyDirectory.delete()) {
            throw new IOException("Failed to delete '" + fileOrEmptyDirectory + "': " + orUnknown(FailureReason.ofDelete(fileOrEmptyDirectory)));
        }
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        // mkdir, not mkdirs() - according to the contract, FileSystemSpi.mkdir() only creates one dir.
        if (!directory.mkdir() && !directory.isDirectory()) {
            throw new IOException("The directory '" + directory + "' couldn't be created: " + orUnknown(FailureReason.ofMkdir(directory)));
        }
    }

    @NotNull
    private static String orUnknown(@Nullable String reason) {
        return reason != null ? reason : "unknown reason";
    }
}
