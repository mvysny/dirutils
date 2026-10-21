package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sk.baka.android.DirUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Backend on plain {@link File}, which answers only {@code false}; the reasons are guessed by
 * {@link FailureReason}. Always available.
 * @author mvy
 */
public class DumbJavaFS implements FileSystemSpi {
    @Override
    public void rename(@NotNull File source, @NotNull File target) throws IOException {
        if (source.renameTo(target)) {
            return;
        }
        final String failed = "Failed to rename '" + source + "' to '" + target + "': ";
        final String why = FailureReason.ofRename(source, target);
        if (why != null) {
            throw new IOException(failed + why);
        }
        // nothing File can see explains it, so assume another mount point, and copy as Files.move() does
        if (isSymlink(source)) {
            throw new IOException(failed + "'" + source + "' is a symlink, which File can't create on another mount point");
        }
        if (source.isDirectory()) {
            final String[] children = source.list();
            if (children == null) {
                throw new IOException(failed + "'" + source + "' can't be listed");
            }
            if (children.length > 0) {
                throw new IOException(failed + FailureReason.ofMoveAcrossMounts(source));
            }
        }
        DirUtils.copy(source, target);
        target.setLastModified(source.lastModified());
        try {
            delete(source);
        } catch (IOException ex) {
            // undo the copy, as Files.move() does, rather than leave the file in both places
            target.delete();
            throw ex;
        }
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

    /**
     * {@link File} follows symlinks everywhere, so a symlink is told by its canonical path differing from its path
     * under the canonical parent; a dangling one, which {@link File#exists()} denies, by its parent still listing it.
     *
     * <p>JDK 8–11 cache canonical paths for ~30 s ({@code sun.io.useCanonCaches}), clearing them only on a
     * {@link File#delete()} or {@link File#renameTo}: a symlink created or removed any other way meanwhile can be
     * misreported. Android 5.0 and 7.0 showed no such cache on the emulator.
     */
    @Override
    public boolean isSymlink(@NotNull File file) throws IOException {
        final File parent = file.getParentFile();
        final String name = file.getName();
        if (parent == null || name.equals(".") || name.equals("..")) {
            return false;
        }
        if (!file.exists()) {
            final String[] siblings = parent.list();
            return siblings != null && Arrays.asList(siblings).contains(name);
        }
        final File inCanonicalParent = new File(parent.getCanonicalFile(), name);
        return !inCanonicalParent.getCanonicalFile().equals(inCanonicalParent);
    }

    @NotNull
    private static String orUnknown(@Nullable String reason) {
        return reason != null ? reason : "unknown reason";
    }
}
