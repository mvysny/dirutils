package sk.baka.android.spi;

import android.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.baka.android.DirUtils;

import java.io.File;
import java.io.IOException;

/**
 * Android FS.
 * @author mvy
 */
public class AndroidFS implements FileSystemSpi {
    private static final Logger log = LoggerFactory.getLogger(AndroidFS.class);
    private static final boolean AVAIL;
    static {
        boolean avail = true;
        try {
            // auto-detect android availability.
            AndroidFS.class.getClassLoader().loadClass("android.app.Activity");
        } catch (ClassNotFoundException ex) {
            avail = false;
        }
        AVAIL = avail;
        if (AVAIL) {
            System.loadLibrary("dirutils");
        }
    }

    public static boolean isAvail() {
        return AVAIL;
    }

    private native int deleteInt(String path);

    private native int mkdirInt(String directory);

    private native String strerror(int errnum);

    private native int getmod(String file);

    /**
     * http://linux.die.net/man/2/rename
     * @param oldpath
     * @param newpath
     * @return errno or 0 on success
     */
    private native int rename(String oldpath, String newpath);

    private void check(int errnum, String message, @Nullable File fileForSticky) throws IOException {
        if (errnum != 0) {
            throw new IOException("Failed to " + message + ": #" + errnum + " " + strerror(errnum)
                    + (fileForSticky == null ? "" : " (" + getParentStickyness(fileForSticky) + ")"));
        }
    }

    @Override
    public Integer getMod(@NotNull String file) throws IOException {
        int result = getmod(file);
        if ((result & 0x80000000) != 0) {
            result = result & (~0x80000000);
            check(result, "get mod of '" + file + "'", null);
            throw new RuntimeException("unexpected for " + file);
        }
        return result;
    }

    /**
     * Cross-device link
     * <p></p>
     * Rename: oldpath and newpath are not on the same mounted file system. (Linux permits a file system to be mounted at
     * multiple points, but rename() does not work across different mount points, even if the same file system is mounted on both.)
     */
    public static final int EXDEV = 18;

    @Override
    public boolean rename(@NotNull File source, @NotNull File target) throws IOException {
        final int errno = rename(source.getAbsolutePath(), target.getAbsolutePath());
        if (errno == EXDEV) {
            // different mount points, we have to perform a copy
            return false;
        } else {
            check(errno, "rename '" + source + "' to '" + target + "'", target);
            return true;
        }
    }

    @Override
    public void delete(@NotNull File fileOrEmptyDirectory) throws IOException {
        check(deleteInt(fileOrEmptyDirectory.getAbsolutePath()), "delete '" + fileOrEmptyDirectory + "'", fileOrEmptyDirectory);
    }

    @Override
    public void mkdir(@NotNull File directory) throws IOException {
        check(mkdirInt(directory.getAbsolutePath()), "create directory '" + directory + "'", directory);
        final Integer mod = getMod(directory.getAbsolutePath());
        if (mod != null && DirUtils.isSticky(mod)) {
            // you don't want to create sticky directory on SDCard:
            // if you create a child directory, Android will immediately chown it to root
            // that means that only a root can delete it
            // that means that your application can no longer delete this directory nor the child directory
            // Avoid sticky dirs at all costs.
            throw new IOException("I have just created a sticky directory!!! " + DirUtils.formatMod(mod) + ": " + directory);
        }
    }

    private String getParentStickyness(@NotNull File file) {
        final File parent = file.getAbsoluteFile().getParentFile();
        try {
            final Integer mod = getMod(parent.getAbsolutePath());
            if (mod == null) {
                return parent.getName() + ":mod:?";
            }
            final boolean sticky = DirUtils.isSticky(mod);
            return sticky ? parent + " has sticky bit set! " + DirUtils.formatMod(mod) + " - please see Aedict FAQ for more, and also https://code.google.com/p/android/issues/detail?id=173708" : parent.getName() + ":" + DirUtils.formatMod(mod);
        } catch (IOException ex) {
            log.error("Failed to get mod of " + parent, ex);
            return "failed to get mod of " + parent + ": " + ex;
        }
    }
}