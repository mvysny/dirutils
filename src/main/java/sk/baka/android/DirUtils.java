package sk.baka.android;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.util.Log;

/**
 * @author mvy
 */
public class DirUtils {
    static {
        System.loadLibrary("dirutils");
    }

    private native int mkdirInt(String directory);

    private native String strerror(int errnum);

    private DirUtils() {
    }

    private static final DirUtils INSTANCE = new DirUtils();

    /**
     * Creates given directory. Does nothing if the directory already exists. Does not create parent directory - use {@link #mkdirs(String)} for that.
     * <p></p>
     * Fails if there already is a file with given name, or if the directory couldn't be created.
     * <p></p>
     * As opposed to dumb {@link File#mkdir()} this method throws {@link IOException} with informative message if the function fails.
     * @param directory the directory to create, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void mkdir(@NotNull String directory) throws IOException {
        mkdir(new File(directory));
    }
    /**
     * Creates given directory. Does nothing if the directory already exists. Does not create parent directory - use {@link #mkdirs(String)} for that.
     * <p></p>
     * Fails if there already is a file with given name, or if the directory couldn't be created.
     * <p></p>
     * As opposed to dumb {@link File#mkdir()} this method throws {@link IOException} with informative message if the function fails.
     * @param directory the directory to create, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void mkdir(@NotNull File directory) throws IOException {
        if (!directory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter directory: invalid value " + directory + ": must be an absolute path");
        }
        if (directory.exists() && directory.isDirectory()) {
            return;
        }
        check(INSTANCE.mkdirInt(directory.getAbsolutePath()), "create directory '" + directory + "'");
    }

    /**
     * Creates given directory and all of necessary parent directories. Does nothing if the directory already exists.
     * <p></p>
     * Fails if there already is a file with given name, or if the directory couldn't be created. On failure, some directories might have been created.
     * <p></p>
     * As opposed to dumb {@link File#mkdirs()} this method throws {@link IOException} with informative message if the function fails.
     * @param directory the directory to create, not null. Must be an absolute directory (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void mkdirs(@NotNull String directory) throws IOException {
        mkdirs(new File(directory));
    }

    /**
     * Creates given directory and all of necessary parent directories. Does nothing if the directory already exists.
     * <p></p>
     * Fails if there already is a file with given name, or if the directory couldn't be created. On failure, some directories might have been created.
     * <p></p>
     * As opposed to dumb {@link File#mkdirs()} this method throws {@link IOException} with informative message if the function fails.
     * @param directory the directory to create, not null. Must be an absolute directory (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
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
     * Deletes given file or empty directory. Does nothing if there is no such file or directory with given name.
     * @param fileOrEmptyDirectory the file/directory to delete, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void delete(@NotNull String fileOrEmptyDirectory) throws IOException {
        delete(new File(fileOrEmptyDirectory));
    }
    /**
     * Deletes given file or empty directory. Does nothing if there is no such file or directory with given name.
     * @param fileOrEmptyDirectory the file/directory to delete, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if directory create fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        if (!fileOrEmptyDirectory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter fileOrEmptyDirectory: invalid value " + fileOrEmptyDirectory + ": must be an absolute path");
        }
        if (!fileOrEmptyDirectory.exists()) {
            return;
        }
        check(INSTANCE.deleteInt(fileOrEmptyDirectory.getAbsolutePath()), "delete '" + fileOrEmptyDirectory + "'");
    }

    /**
     * Deletes given directory, including subdirectories.
     * @param path the directory or file to delete, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if the directory delete fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void deleteRecursively(@NotNull String path) throws IOException {
        deleteRecursively(new File(path));
    }
    /**
     * Deletes given directory, including subdirectories.
     * @param path the directory or file to delete, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if the directory delete fails.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void deleteRecursively(@NotNull File path) throws IOException {
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Parameter path: invalid value " + path + ": must be an absolute path");
        }
        if (!path.exists()) {
            return;
        }
        if (path.isDirectory()) {
            final File[] files = path.listFiles();
            if (files != null) {
                for (final File child : files) {
                    deleteRecursively(child.getAbsolutePath());
                }
            }
        }
        delete(path);
    }

    private static void check(int errnum, String message) throws IOException {
        if (errnum != 0) {
            throw new IOException("Failed to " + message + ": #" + errnum + " " + INSTANCE.strerror(errnum));
        }
    }

    private native int deleteInt(String path);

    /**
     * Cross-device link
     * <p></p>
     * Rename: oldpath and newpath are not on the same mounted file system. (Linux permits a file system to be mounted at
     * multiple points, but rename() does not work across different mount points, even if the same file system is mounted on both.)
     */
    public static final int EXDEV = 18;

    /**
     * Renames the file, overwriting the target file.
     * <p></p>
     * If the file cannot be renamed atomically (because target resides on a different mount point), the file is copied, then deleted.
     * @param source source file, not null.
     * @param target target file, not null.
     * @throws IOException
     */
    public static void rename(@NotNull File source, @NotNull File target) throws IOException {
        if (source.equals(target)) {
            return;
        }
        delete(target);
        final int errno = INSTANCE.rename(source.getAbsolutePath(), target.getAbsolutePath());
        if (errno == EXDEV) {
            // different mount points, we have to perform a copy
            copy(source, target);
            delete(source.getAbsolutePath());
        } else {
            check(errno, "rename '" + source + "' to '" + target + "'");
        }
    }

    /**
     * http://linux.die.net/man/2/rename
     * @param oldpath
     * @param newpath
     * @return errno or 0 on success
     */
    private native int rename(String oldpath, String newpath);

    public static void copy(@NotNull File source, @NotNull File target) throws IOException {
        final FileOutputStream out = new FileOutputStream(target);
        try {
            copy(new FileInputStream(source), out);
        } finally {
            closeQuietly(out);
        }
    }

    /**
     * Copies input stream.
     * @param inputStream the input stream, always closed.
     * @param outputStream the output stream, never closed.
     */
    @Contract("null, _ -> fail")
    public static void copy(@NotNull InputStream inputStream, @NotNull OutputStream outputStream) throws IOException {
        copy(inputStream, outputStream, true, false);
    }

    private static final int BUFSIZE = 8192;

    /**
     * Copies input stream.
     * @param inputStream the input stream, closed as required.
     * @param outputStream the output stream, closed as required, always flushed.
     * @param closeIn if true, inputStream is closed.
     * @param closeOut if true, outputStream is closed.
     */
    @Contract("null, _ -> fail")
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
                Log.e(TAG, "Failed to flush output stream", ex);
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
     * Quietly closes given closeable. Errors are logged.
     * @param s the closeable, may be null. Does nothing if s is null.
     */
    public static void closeQuietly(@Nullable Closeable s) {
        if (s != null) {
            try {
                s.close();
            } catch (Throwable t) {
                Log.i(TAG, "Failed to close " + s, t);
            }
        }
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteQuietly(@NotNull File fileOrDir) {
        try {
            delete(fileOrDir);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteQuietly(@NotNull String fileOrDir) {
        deleteQuietly(new File(fileOrDir));
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteRecursivelyQuietly(@NotNull File fileOrDir) {
        try {
            deleteRecursively(fileOrDir);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteRecursivelyQuietly(@NotNull String fileOrDir) {
        deleteRecursivelyQuietly(new File(fileOrDir));
    }

    private static final String TAG = DirUtils.class.getSimpleName();
}
