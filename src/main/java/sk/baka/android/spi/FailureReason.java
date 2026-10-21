package sk.baka.android.spi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

/**
 * Explains why a mkdir, delete or rename failed, from what {@link File} can still see. Each method returns a
 * reason naming its culprit, or {@code null} when nothing visible explains the failure:
 *
 * <pre>{@code
 * FailureReason.ofMkdir(new File("/data/foo/bar"));   // => "'/data/foo' is not writable"
 * }</pre>
 *
 * <h2>Implementation detail</h2>
 * Uses {@code java.io.File} only, so that {@link DumbJavaFS} can load on pre-Oreo Android.
 */
final class FailureReason {
    private FailureReason() {
    }

    @Nullable
    static String ofMkdir(@NotNull File directory) {
        if (directory.exists() && !directory.isDirectory()) {
            return "'" + directory + "' points to a file";
        }
        return ofParent(directory.getParentFile());
    }

    @Nullable
    static String ofDelete(@NotNull File fileOrEmptyDirectory) {
        if (!fileOrEmptyDirectory.exists()) {
            return "'" + fileOrEmptyDirectory + "' does not exist";
        }
        final String[] children = fileOrEmptyDirectory.list();
        if (children != null && children.length > 0) {
            return "'" + fileOrEmptyDirectory + "' is not empty";
        }
        return ofParent(fileOrEmptyDirectory.getParentFile());
    }

    @Nullable
    static String ofRename(@NotNull File source, @NotNull File target) {
        if (!source.exists()) {
            return "'" + source + "' does not exist";
        }
        final String targetParent = ofParent(target.getParentFile());
        return targetParent != null ? targetParent : ofParent(source.getParentFile());
    }

    /**
     * Why {@code directory} can't move to another mount point: like {@link java.nio.file.Files#move}, only an
     * empty one can.
     */
    @NotNull
    static String ofMoveAcrossMounts(@NotNull File directory) {
        return "'" + directory + "' is not empty, and a directory moves to another mount point only when empty";
    }

    /**
     * Why no entry can be created in, or removed from, {@code parent}.
     */
    @Nullable
    private static String ofParent(@Nullable File parent) {
        if (parent == null) {
            return null;
        }
        if (!parent.exists()) {
            return "'" + parent + "' does not exist";
        }
        if (!parent.isDirectory()) {
            return "'" + parent + "' is not a directory";
        }
        if (!parent.canWrite()) {
            return "'" + parent + "' is not writable";
        }
        return null;
    }
}
