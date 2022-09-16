package sk.baka.android;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
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
    public static void deleteNonRecQuietly(@NotNull String fileOrDir) {
        deleteNonRecQuietly(new File(fileOrDir));
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}.
     * @param fileOrDir the file or directory to delete, not null.
     */
    public static void deleteRecursivelyQuietly(@NotNull File fileOrDir) {
        try {
            deleteRecursively(fileOrDir);
        } catch (IOException ex) {
            log.error("Failed to delete " + fileOrDir, ex);
        }
    }

    /**
     * Deletes given file or dictionary. Does not throw {@link IOException}.
     * @param fileOrDir the file or directory to delete, not null.
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
     * Allows you to force a particular SPI, bypassing the detection routine.
     * @param fileSystemSpi the SPI to use, null to enable the auto-detection routine.
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

    public static boolean isJava7Avail() {
        // had to move the function here. On Android 4.4 we cannot call Java7FS.isAvail()
        // since that would throw VerifyError when Android tries to load the Java7FS class.
        return JAVA7_AVAIL;
    }


    // visible for testing
    @NotNull
    static FileSystemSpi get() {
        if (SPI == null) {
            if (FORCE_SPI != null) {
                SPI = FORCE_SPI;
            } else if (isJava7Avail()) {
                SPI = new Java7FS();
            } else {
                // fallback
                SPI = new DumbJavaFS();
            }
            log.info("DirUtils: using SPI " + SPI.getClass().getSimpleName());
        }
        return SPI;
    }
}
