package sk.baka.android;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.baka.android.spi.DumbJavaFS;
import sk.baka.android.spi.FileSystemSpi;
import sk.baka.android.spi.Java7FS;

/**
 * {@link File} operations that throw an {@link IOException} saying why, instead of returning {@code false}:
 *
 * <pre>{@code
 * File thumbnails = new File(context.getCacheDir(), "thumbnails");
 * DirUtils.mkdirs(thumbnails);             // like mkdir -p
 * DirUtils.rename(new File(thumbnails, "a.tmp"), new File(thumbnails, "a.png"));
 * DirUtils.deleteNonRec(thumbnails);       // IOException: Failed to delete '/data/.../thumbnails': '/data/.../thumbnails' is not empty
 * DirUtils.deleteRecursively(thumbnails);  // like rm -rf
 * }</pre>
 *
 * The mkdir, delete and rename methods take absolute paths only: a relative one throws
 * {@link IllegalArgumentException}, to catch resolving it against an unexpected working directory. The
 * {@code *Quietly} variants log the failure instead of throwing.
 *
 * <p>Each operation behaves like the {@link java.nio.file.Files} call its doc names, so on Android 8.0
 * (API 26)+ moving to {@code Files} changes nothing but the exception: DirUtils throws a plain
 * {@link IOException}, never {@code Files}' subclasses such as {@code NoSuchFileException}, which older
 * Android lacks.
 *
 * <p>The work is done by a {@link FileSystemSpi} backend, picked at runtime; {@link #set} forces one.
 * @author mvy
 */
public class DirUtils {
    private DirUtils() {
    }

    /**
     * Same as {@link #mkdir(File)}.
     * @param directory absolute.
     * @throws IOException if the directory couldn't be created.
     * @throws IllegalArgumentException if {@code directory} is relative.
     */
    public static void mkdir(@NotNull String directory) throws IOException {
        mkdir(new File(directory));
    }
    /**
     * Creates the directory; does nothing if it already is one. Does not create the parent, see
     * {@link #mkdirs(File)}.
     *
     * <p>Like {@link java.nio.file.Files#createDirectory}, except that one throws on an existing directory.
     * @param directory absolute.
     * @throws IOException if the directory couldn't be created, e.g. because a file of that name exists or the
     *   parent is missing.
     * @throws IllegalArgumentException if {@code directory} is relative.
     */
    public static void mkdir(@NotNull File directory) throws IOException {
        if (!directory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter directory: invalid value " + directory + ": must be an absolute path");
        }
        if (directory.exists() && directory.isDirectory()) {
            return;
        }
        get().mkdir(directory);
    }

    /**
     * Same as {@link #mkdirs(File)}.
     * @param directory absolute.
     * @throws IOException if a directory couldn't be created.
     * @throws IllegalArgumentException if {@code directory} is relative.
     */
    public static void mkdirs(@NotNull String directory) throws IOException {
        mkdirs(new File(directory));
    }

    /**
     * Creates the directory and any missing parents, like {@code mkdir -p} and
     * {@link java.nio.file.Files#createDirectories}; does nothing if it already is one.
     * @param directory absolute.
     * @throws IOException if a directory couldn't be created, e.g. because a file of that name exists; the parents
     *   created before it stay.
     * @throws IllegalArgumentException if {@code directory} is relative.
     */
    public static void mkdirs(@NotNull File directory) throws IOException {
        if (!directory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter directory: invalid value " + directory + ": must be an absolute path");
        }
        if (directory.exists()) {
            // check if it is a file
            mkdir(directory);
            return;
        }
        mkdirs(directory.getParentFile());
        mkdir(directory);
    }

    /**
     * Same as {@link #deleteNonRec(File)}.
     * @param fileOrEmptyDirectory absolute.
     * @throws IOException if the delete fails, e.g. because the directory is not empty.
     * @throws IllegalArgumentException if {@code fileOrEmptyDirectory} is relative.
     */
    public static void deleteNonRec(@NotNull String fileOrEmptyDirectory) throws IOException {
        deleteNonRec(new File(fileOrEmptyDirectory));
    }
    /**
     * Deletes the file or empty directory; does nothing if it doesn't exist. A symlink, even a dangling one, is
     * deleted itself, not its target. Like {@link java.nio.file.Files#deleteIfExists}.
     * @param fileOrEmptyDirectory absolute.
     * @throws IOException if the delete fails, e.g. because the directory is not empty.
     * @throws IllegalArgumentException if {@code fileOrEmptyDirectory} is relative.
     */
    public static void deleteNonRec(@NotNull File fileOrEmptyDirectory) throws IOException {
        if (!fileOrEmptyDirectory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter fileOrEmptyDirectory: invalid value " + fileOrEmptyDirectory + ": must be an absolute path");
        }
        // File.exists() follows the link, so it denies a dangling one
        if (!fileOrEmptyDirectory.exists() && !get().isSymlink(fileOrEmptyDirectory)) {
            return;
        }
        get().delete(fileOrEmptyDirectory);
    }

    /**
     * Same as {@link #deleteRecursively(File)}.
     * @param path absolute.
     * @throws IOException if an entry couldn't be deleted.
     * @throws IllegalArgumentException if {@code path} is relative.
     */
    public static void deleteRecursively(@NotNull String path) throws IOException {
        deleteRecursively(new File(path));
    }
    /**
     * Deletes the file, or the directory with everything in it, like {@code rm -rf}: a symlink is deleted, never
     * followed. Does nothing if it doesn't exist.
     * @param path absolute.
     * @throws IOException if an entry couldn't be deleted; the entries deleted before it stay deleted.
     * @throws IllegalArgumentException if {@code path} is relative.
     */
    public static void deleteRecursively(@NotNull File path) throws IOException {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Parameter path: invalid value " + path + ": must be an absolute path");
        }
        if (path.isDirectory() && !get().isSymlink(path)) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File child : files) {
                    deleteRecursively(child.getAbsolutePath());
                }
            }
        }
        deleteNonRec(path);
    }

    /**
     * Moves the file or directory, replacing {@code target}, like {@code mv} and
     * {@link java.nio.file.Files#move} with {@code REPLACE_EXISTING}. Within a mount point it is an atomic
     * rename. Across mount points it is copied with its modification time, then the source deleted: a symlink
     * moves as itself (before Android 8.0, not at all), a directory only when empty.
     *
     * <p>Not atomic: {@code target} is deleted first, so a move that fails afterwards leaves the source in place
     * and the target gone.
     * @param source must exist; may be a dangling symlink.
     * @param target absolute; may be an empty directory, never a non-empty one.
     * @throws IOException if the source is missing, or the target can't be deleted, or the move fails.
     * @throws IllegalArgumentException if {@code target} is relative.
     */
    public static void rename(@NotNull File source, @NotNull File target) throws IOException {
        // compared as entries, not by path: a second path to the source would be deleted below as the target
        if (entry(source).equals(entry(target))) {
            return;
        }
        // the target is deleted first, so check before destroying it for nothing
        if (!source.exists() && !get().isSymlink(source)) {
            throw new IOException("Failed to rename '" + source + "' to '" + target + "': '" + source + "' does not exist");
        }
        deleteNonRec(target);
        get().rename(source, target);
    }

    /**
     * Copies the file's bytes into {@code target}, like {@link java.nio.file.Files#copy(java.nio.file.Path,
     * java.nio.file.Path, java.nio.file.CopyOption...)} with {@code REPLACE_EXISTING}: a symlink is followed, a
     * directory is copied as an empty one, and no attributes are copied, such as permissions or the modification
     * time.
     * @param source must exist.
     * @param target replaced if it exists: a file, a symlink (not what it points to) or an empty directory,
     *   never a non-empty one.
     * @throws IOException if the source is missing, or the target can't be replaced or written; a half-written
     *   target is deleted.
     */
    public static void copy(@NotNull File source, @NotNull File target) throws IOException {
        // checked before the target is replaced, so that a missing source doesn't destroy it for nothing
        if (!source.exists()) {
            throw new FileNotFoundException("Failed to copy '" + source + "' to '" + target + "': '" + source + "' does not exist");
        }
        // the source is followed, the target isn't: copying a symlink onto what it points to is a no-op too
        if (source.getCanonicalFile().equals(entry(target))) {
            return;
        }
        final File absoluteTarget = target.getAbsoluteFile();
        if (absoluteTarget.exists() || get().isSymlink(absoluteTarget)) {
            get().delete(absoluteTarget);
        }
        if (source.isDirectory()) {
            get().mkdir(absoluteTarget);
            return;
        }
        final FileInputStream in = new FileInputStream(source);
        final FileOutputStream out;
        try {
            out = new FileOutputStream(absoluteTarget);
        } catch (IOException ex) {
            closeQuietly(in);
            throw ex;
        }
        boolean copied = false;
        try {
            copy(in, out);
            copied = true;
        } finally {
            closeQuietly(out);
            if (!copied) {
                absoluteTarget.delete();
            }
        }
    }

    /**
     * The directory entry {@code file} names: its parent's symlinks resolved, its own not. Two paths to one
     * entry compare equal, a symlink and what it points to don't.
     */
    @NotNull
    private static File entry(@NotNull File file) throws IOException {
        final File absolute = file.getAbsoluteFile();
        final File parent = absolute.getParentFile();
        final String name = absolute.getName();
        if (parent == null || name.equals(".") || name.equals("..")) {
            return absolute.getCanonicalFile();
        }
        return new File(parent.getCanonicalFile(), name);
    }

    /**
     * Copies everything left in {@code inputStream} to {@code outputStream}.
     * @param inputStream always closed, even on failure.
     * @param outputStream flushed, never closed.
     * @throws IOException if a read or a write fails.
     */
    public static void copy(@NotNull InputStream inputStream, @NotNull OutputStream outputStream) throws IOException {
        copy(inputStream, outputStream, true, false);
    }

    private static final int BUFSIZE = 8192;

    /**
     * Copies everything left in {@code inputStream} to {@code outputStream}.
     * @param inputStream read to its end.
     * @param outputStream flushed, even on failure.
     * @param closeIn whether to close {@code inputStream} afterwards, even on failure.
     * @param closeOut whether to close {@code outputStream} afterwards, even on failure.
     * @throws IOException if a read or a write fails.
     */
    public static void copy(@NotNull InputStream inputStream, @NotNull OutputStream outputStream, boolean closeIn, boolean closeOut) throws IOException {
        try {
            final byte[] buf = new byte[BUFSIZE];
            for (; ; ) {
                final int bytes = inputStream.read(buf);
                if (bytes < 0) {
                    break;
                } else if (bytes > 0) {
                    outputStream.write(buf, 0, bytes);
                }
            }
        } finally {
            try {
                outputStream.flush();
            } catch (Exception ex) {
                log.error("Failed to flush output stream", ex);
            }
            if (closeIn) {
                closeQuietly(inputStream);
            }
            if (closeOut) {
                closeQuietly(outputStream);
            }
        }
    }

    /**
     * Closes {@code s}, logging a failure instead of throwing it.
     * @param s nothing happens if null.
     */
    public static void closeQuietly(@Nullable Closeable s) {
        if (s != null) {
            try {
                s.close();
            } catch (Throwable t) {
                log.info("Failed to close " + s, t);
            }
        }
    }

    /**
     * Like {@link #deleteNonRec(File)}, but logs a failure instead of throwing it.
     * @param fileOrDir absolute.
     * @throws IllegalArgumentException if {@code fileOrDir} is relative; that one is not swallowed.
     */
    public static void deleteNonRecQuietly(@NotNull File fileOrDir) {
        try {
            deleteNonRec(fileOrDir);
        } catch (IOException ex) {
            log.error("Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Same as {@link #deleteNonRecQuietly(File)}.
     * @param fileOrDir absolute.
     * @throws IllegalArgumentException if {@code fileOrDir} is relative; that one is not swallowed.
     */
    public static void deleteNonRecQuietly(@NotNull String fileOrDir) {
        deleteNonRecQuietly(new File(fileOrDir));
    }

    /**
     * Like {@link #deleteRecursively(File)}, but logs a failure instead of throwing it.
     * @param fileOrDir absolute.
     * @throws IllegalArgumentException if {@code fileOrDir} is relative; that one is not swallowed.
     */
    public static void deleteRecursivelyQuietly(@NotNull File fileOrDir) {
        try {
            deleteRecursively(fileOrDir);
        } catch (IOException ex) {
            log.error("Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Same as {@link #deleteRecursivelyQuietly(File)}.
     * @param fileOrDir absolute.
     * @throws IllegalArgumentException if {@code fileOrDir} is relative; that one is not swallowed.
     */
    public static void deleteRecursivelyQuietly(@NotNull String fileOrDir) {
        deleteRecursivelyQuietly(new File(fileOrDir));
    }

    private static final Logger log = LoggerFactory.getLogger(DirUtils.class);

    @Nullable
    private static FileSystemSpi SPI;
    @Nullable
    private static volatile FileSystemSpi FORCE_SPI;

    /**
     * Forces a backend, e.g. {@code DirUtils.set(new DumbJavaFS())}, instead of the detected one:
     * {@link Java7FS} if {@link #isJava7Avail()}, else {@link DumbJavaFS}.
     * Call it before other threads use {@code DirUtils}, e.g. at startup or in a test: an
     * operation racing it may keep using, or even re-install, the previous backend.
     * @param fileSystemSpi the backend, or {@code null} to detect one again.
     */
    public static synchronized void set(@Nullable FileSystemSpi fileSystemSpi) {
        SPI = null;
        FORCE_SPI = fileSystemSpi;
    }

    private static final boolean JAVA7_AVAIL;
    static {
        boolean avail = true;
        try {
            Class.forName("java.nio.file.Files");
        } catch (ClassNotFoundException ex) {
            avail = false;
        }
        JAVA7_AVAIL = avail;
    }

    /**
     * Tells whether {@code java.nio.file} is there; only then may {@link Java7FS} be loaded.
     * @return true on any JVM, and on Android 8.0 (API 26)+.
     */
    public static boolean isJava7Avail() {
        // the probe lives here, not in Java7FS: on Android 4.4 merely loading Java7FS throws VerifyError.
        return JAVA7_AVAIL;
    }


    // visible for testing
    @NotNull
    static FileSystemSpi get() {
        // lock-free: SPI is read once into a local, since a racing set(), or a racy re-read of a
        // non-volatile field, may otherwise make the second read return null. Threads racing the
        // first call may each create a backend; harmless, the backends are stateless.
        FileSystemSpi spi = SPI;
        if (spi == null) {
            final FileSystemSpi forced = FORCE_SPI;
            if (forced != null) {
                spi = forced;
            } else if (isJava7Avail()) {
                spi = new Java7FS();
            } else {
                // fallback
                spi = new DumbJavaFS();
            }
            SPI = spi;
            log.info("DirUtils: using SPI " + spi.getClass().getSimpleName());
        }
        return spi;
    }
}
