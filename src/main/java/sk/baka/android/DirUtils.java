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
        check(INSTANCE.mkdirInt(directory.getAbsolutePath()), "create directory '" + directory + "'", directory);
        final int mod = getMod(directory);
        if (isSticky(mod)) {
            // you don't want to create sticky directory on SDCard:
            // if you create a child directory, Android will immediately chown it to root
            // that means that only a root can delete it
            // that means that your application can no longer delete this directory nor the child directory
            // Avoid sticky dirs at all costs.
            throw new IOException("I have just created a sticky directory!!! " + formatMod(mod) + ": " + directory);
        }
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
        check(INSTANCE.deleteInt(fileOrEmptyDirectory.getAbsolutePath()), "delete '" + fileOrEmptyDirectory + "'", fileOrEmptyDirectory);
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

    private static void check(int errnum, String message, @Nullable File fileForSticky) throws IOException {
        if (errnum != 0) {
            throw new IOException("Failed to " + message + ": #" + errnum + " " + INSTANCE.strerror(errnum)
            + (fileForSticky == null ? "" : " (" + getParentStickyness(fileForSticky) + ")"));
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
            check(errno, "rename '" + source + "' to '" + target + "'", target);
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

    private native int getmod(String file);

    public static int getMod(@NotNull File file) throws IOException {
        return getMod(file.getAbsolutePath());
    }

    public static int getMod(@NotNull String file) throws IOException {
        int result = INSTANCE.getmod(file);
        if ((result & 0x80000000) != 0) {
            result = result & (~0x80000000);
            check(result, "get mod of '" + file + "'", null);
            throw new RuntimeException("unexpected for " + file);
        }
        return result;
    }

    private static String getParentStickyness(@NotNull File file) {
        final File parent = file.getAbsoluteFile().getParentFile();
        try {
            final int mod = getMod(parent);
            final boolean sticky = isSticky(mod);
            return sticky ? parent + " has sticky bit set! " + formatMod(mod) : parent.getName() + ":" + formatMod(mod);
        } catch (IOException ex) {
            Log.e(TAG, "Failed to get mod of " + parent, ex);
            return "failed to get mod of " + parent + ": " + ex;
        }
    }

    private static final int S_IFMT = 00170000;
    private static final int S_IFDIR = 0040000;
    public static boolean S_ISDIR(int mod) {
        return (((mod) & S_IFMT) == S_IFDIR);
    }
    public static boolean isSticky(int mod) {
        return (mod & STICKY) != 0;
    }
    private static final int STICKY = 01000;
    private static final int S_IRUSR = 00400;
    private static final int S_IWUSR = 00200;
    private static final int S_IXUSR = 00100;
    private static final int S_IRGRP = 00040;
    private static final int S_IWGRP = 00020;
    private static final int S_IXGRP = 00010;
    private static final int S_IROTH = 00004;
    private static final int S_IWOTH = 00002;
    private static final int S_IXOTH = 00001;
    @NotNull
    public static String formatMod(int mod) {
        final StringBuilder sb = new StringBuilder(10);
        sb.append((S_ISDIR(mod)) ? "d" : "-");
        sb.append((mod & S_IRUSR)!=0 ? "r" : "-");
        sb.append((mod & S_IWUSR)!=0 ? "w" : "-");
        sb.append((mod & S_IXUSR)!=0 ? "x" : "-");
        sb.append((mod & S_IRGRP)!=0 ? "r" : "-");
        sb.append((mod & S_IWGRP)!=0 ? "w" : "-");
        sb.append((mod & S_IXGRP)!=0 ? "x" : "-");
        sb.append((mod & S_IROTH)!=0 ? "r" : "-");
        sb.append((mod & S_IWOTH)!=0 ? "w" : "-");
        final boolean sticky = isSticky(mod);
        final boolean xoth = (mod & S_IXOTH)!=0;
        sb.append(sticky ? (xoth ? 't' : 'T') : (xoth ? "x" : "-"));
        return sb.toString();
    }
}
