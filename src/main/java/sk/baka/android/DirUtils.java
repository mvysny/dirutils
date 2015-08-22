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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.baka.android.spi.AndroidFS;
import sk.baka.android.spi.DumbJavaFS;
import sk.baka.android.spi.FileSystemSpi;
import sk.baka.android.spi.Java7FS;

/**
 * @author mvy
 */
public class DirUtils {
    private DirUtils() {
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
        get().mkdir(directory);
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
     * @throws IOException if directory delete fails, for example because the directory is not empty.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void deleteNonRec(@NotNull String fileOrEmptyDirectory) throws IOException {
        deleteNonRec(new File(fileOrEmptyDirectory));
    }
    /**
     * Deletes given file or empty directory. Does nothing if there is no such file or directory with given name.
     * @param fileOrEmptyDirectory the file/directory to delete, not null. Must be an absolute path (must start with a slash).
     * @throws IOException if directory delete fails, for example because the directory is not empty.
     * @throws IllegalArgumentException if given path is not absolute.
     */
    public static void deleteNonRec(@NotNull File fileOrEmptyDirectory) throws IOException {
        if (!fileOrEmptyDirectory.isAbsolute()) {
            throw new IllegalArgumentException("Parameter fileOrEmptyDirectory: invalid value " + fileOrEmptyDirectory + ": must be an absolute path");
        }
        if (!fileOrEmptyDirectory.exists()) {
            return;
        }
        get().delete(fileOrEmptyDirectory);
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
        deleteNonRec(path);
    }

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
        deleteNonRec(target);
        if (!get().rename(source, target)) {
            copy(source, target);
            deleteNonRec(source.getAbsolutePath());
        }
    }

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
     * Quietly closes given closeable. Errors are logged.
     * @param s the closeable, may be null. Does nothing if s is null.
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
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteNonRecQuietly(@NotNull File fileOrDir) {
        try {
            deleteNonRec(fileOrDir);
        } catch (IOException ex) {
            log.error("Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}. Does NOT delete non-empty directory.
     * @param fileOrDir the file or directory to delete, not null.
     */
    @Contract("null -> fail")
    public static void deleteNonRecQuietly(@NotNull String fileOrDir) {
        deleteNonRecQuietly(new File(fileOrDir));
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
            log.error("Failed to delete " + fileOrDir, ex);
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

    private static final Logger log = LoggerFactory.getLogger(DirUtils.class);

    public static Integer getMod(@NotNull File file) throws IOException {
        return getMod(file.getAbsolutePath());
    }

    @Nullable
    private static FileSystemSpi SPI;
    @NotNull
    private static FileSystemSpi get() {
        if (SPI == null) {
            if (AndroidFS.isAvail()) {
                SPI = new AndroidFS();
            } else if (Java7FS.isAvail()) {
                SPI = new Java7FS();
            } else {
                // fallback
                SPI = new DumbJavaFS();
            }
            log.info("DirUtils: using SPI " + SPI.getClass().getSimpleName());
        }
        return SPI;
    }

    public static Integer getMod(@NotNull String file) throws IOException {
        return get().getMod(file);
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
        final StringBuilder sb = new StringBuilder(16);
        sb.append('0');
        sb.append(Integer.toOctalString(mod & 07777));
        sb.append(':');
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
