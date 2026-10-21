package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;

/**
 * Uses new file methods added in Java 7 and Android API level 26 (Android Oreo 8.0.0). Instantiate only after
 * {@link sk.baka.android.DirUtils#isJava7Avail()} said yes.
 * @author mvy
 */
public class Java7FS implements FileSystemSpi {
    @Override
    public boolean rename(@NotNull File source, @NotNull File target) throws IOException {
        try {
            Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (AtomicMoveNotSupportedException ex) {
            return false;
        } catch (IOException ex) {
            throw new IOException("Failed to rename '" + source + "' to '" + target + "': " + reason(FailureReason.ofRename(source, target), ex), ex);
        }
    }

    @Override
    public void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        try {
            Files.delete(fileOrEmptyDirectory.toPath());
        } catch (IOException ex) {
            throw new IOException("Failed to delete '" + fileOrEmptyDirectory + "': " + reason(FailureReason.ofDelete(fileOrEmptyDirectory), ex), ex);
        }
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        try {
            Files.createDirectory(directory.toPath());
        } catch (IOException ex) {
            if (ex instanceof FileAlreadyExistsException && directory.isDirectory()) {
                return;
            }
            throw new IOException("The directory '" + directory + "' couldn't be created: " + reason(FailureReason.ofMkdir(directory), ex), ex);
        }
    }

    @Override
    public boolean isSymlink(@NotNull File file) {
        return Files.isSymbolicLink(file.toPath());
    }

    /**
     * Prefers {@code guessed}, which names the culprit; NIO names only the paths it was given, and puts the reason
     * of the commonest failures solely into the exception's type.
     */
    @NotNull
    private static String reason(@Nullable String guessed, @NotNull IOException ex) {
        if (guessed != null) {
            return guessed;
        }
        if (!(ex instanceof FileSystemException)) {
            return ex.toString();
        }
        final String reason = ((FileSystemException) ex).getReason();
        if (reason != null) {
            return reason;
        } else if (ex instanceof AccessDeniedException) {
            return "permission denied";
        } else if (ex instanceof NoSuchFileException) {
            return "no such file or directory";
        } else if (ex instanceof DirectoryNotEmptyException) {
            return "directory not empty";
        } else if (ex instanceof FileAlreadyExistsException) {
            return "already exists";
        }
        return ex.toString();
    }
}
