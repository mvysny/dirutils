package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * Basic operations every FS must implement. {@link sk.baka.android.DirUtils} uses this as its backend, and
 * hands in absolute paths only.
 *
 * <p>Every failure throws an {@link IOException} naming the culprit and why, e.g.
 * <pre>The directory '/data/foo/bar' couldn't be created: '/data/foo' is not writable</pre>
 * @author mvy
 */
public interface FileSystemSpi {
    /**
     * Renames the file, atomically.
     *
     * @param source exists.
     * @param target does not exist.
     * @return false ONLY IF target resides on a different mount point, so the caller has to copy.
     * @throws IOException on any other failure.
     */
    boolean rename(@NotNull File source, @NotNull File target) throws IOException;

    /**
     * Deletes given file or empty directory.
     *
     * @param fileOrEmptyDirectory exists.
     * @throws IOException if the delete fails, e.g. because the directory is not empty.
     */
    void delete(@NotNull File fileOrEmptyDirectory) throws IOException;

    /**
     * Creates given directory; does nothing if it already is one, so a concurrent {@code mkdirs()} of the same
     * tree doesn't fail. Does not create the parent directory.
     *
     * @param directory may exist, and may point to a file.
     * @throws IOException if the directory couldn't be created, e.g. because a file of that name exists.
     */
    void mkdir(@NotNull File directory) throws IOException;

    /**
     * Tells whether {@code file} itself is a symlink, which recursive operations delete rather than follow, like
     * {@code rm -rf} does.
     *
     * @param file may not exist.
     * @return true for a dangling symlink too.
     * @throws IOException if the path can't be resolved.
     */
    boolean isSymlink(@NotNull File file) throws IOException;
}
