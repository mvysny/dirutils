package sk.baka.android

import java.io.File
import java.io.IOException

/**
 * Creates this directory and any missing parents, like `mkdir -p`; does nothing if it already is one.
 *
 * @throws IOException if a directory couldn't be created, e.g. because a file of that name exists; the parents
 *   created before it stay.
 * @throws IllegalArgumentException if this file is relative.
 */
@Throws(IOException::class)
fun File.mkdirp() = DirUtils.mkdirs(this)

/**
 * Deletes this file or empty directory; does nothing if it doesn't exist. A symlink, even a dangling one, is
 * deleted itself, not its target.
 *
 * @throws IOException if the delete fails, e.g. because the directory is not empty.
 * @throws IllegalArgumentException if this file is relative.
 */
@Throws(IOException::class)
fun File.rmf() = DirUtils.deleteNonRec(this)

/**
 * Deletes this file, or this directory with everything in it, like `rm -rf`: a symlink is deleted, never
 * followed. Does nothing if it doesn't exist.
 *
 * @throws IOException if an entry couldn't be deleted; the entries deleted before it stay deleted.
 * @throws IllegalArgumentException if this file is relative.
 */
@Throws(IOException::class)
fun File.rmrf() = DirUtils.deleteRecursively(this)

/**
 * Like [rmrf], but logs a failure instead of throwing it.
 *
 * @throws IllegalArgumentException if this file is relative; that one is not swallowed.
 */
fun File.rmrfq() = DirUtils.deleteRecursivelyQuietly(this)

/**
 * Returns the size in bytes of this file, or of this directory with everything in it; 0 if it doesn't exist.
 * A directory counts 4096 on top of its children; a symlink counts 0 and is not followed.
 *
 * @throws IOException if a symlink can't be told apart from a file, see [sk.baka.android.spi.FileSystemSpi.isSymlink].
 */
@Throws(IOException::class)
fun File.calculateLengthRecursively(): Long = when {
    DirUtils.get().isSymlink(this) -> 0
    isFile -> length()
    isDirectory -> {
        val files = listFiles() ?: arrayOf()
        4096 + files.sumOf { it.calculateLengthRecursively() }
    }
    else -> 0
}
