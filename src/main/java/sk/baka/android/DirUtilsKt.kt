package sk.baka.android

import java.io.File
import java.io.IOException

/**
 * Creates the directory and all of necessary parent directories. Does nothing if the directory already exists. Must be called on an absolute File, to
 * avoid relative file mistakes.
 *
 * Fails if there already is a file with given name, or if the directory couldn't be created. On failure, some directories might have been created.
 *
 * As opposed to dumb [File.mkdirs] this method throws [IOException] with informative message if the function fails.
 *
 * @throws IOException if directory create fails.
 * @throws IllegalArgumentException if given path is not absolute.
 */
@Throws(IOException::class)
fun File.mkdirp() = DirUtils.mkdirs(this)

/**
 * Deletes the file or empty directory.  The file must be [absolute][File.absolute]. Does nothing if there is no such file or directory with given name.
 *
 * @throws IOException if directory delete fails, for example because the directory is not empty.
 * @throws IllegalArgumentException if given path is not absolute.
 */
@Throws(IOException::class)
fun File.rmf() = DirUtils.deleteNonRec(this)

/**
 * Deletes this directory, including subdirectories. The file must be [absolute][File.absolute].
 * @throws IOException if the directory delete fails.
 * @throws IllegalArgumentException if given path is not absolute.
 */
@Throws(IOException::class)
fun File.rmrf() = DirUtils.deleteRecursively(this)

/**
 * Deletes given file or dictionary. Does not throw [IOException]. The file must be [absolute][File.absolute].
 */
fun File.rmrfq() = DirUtils.deleteRecursivelyQuietly(this)

/**
 * Returns length in bytes of given file or directory.
 * @receiver dir the directory to list. A directory length is set to be 4kb + lengths of all its children.
 * @return the directory length, 0 if the directory/file does not exist.
 */
fun File.calculateLengthRecursively(): Long = when {
    !exists() -> 0
    isFile -> length()
    isDirectory -> {
        val files = listFiles() ?: arrayOf()
        4096 + files.sumOf { it.calculateLengthRecursively() }
    }
    else -> 0
}
