package sk.baka.android

import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.EnumSource
import sk.baka.android.spi.FileSystemSpi
import sk.baka.android.spi.Java7FS
import java.io.*
import java.nio.file.Files
import java.nio.file.LinkOption
import kotlin.reflect.KClass
import kotlin.test.assertFailsWith
import kotlin.test.expect

/**
 * Fails unless [block] throws a [type] whose message contains [message].
 */
fun <T : Throwable> expectThrows(type: KClass<T>, message: String = "", block: () -> Unit): T {
    val e = assertFailsWith(type, block = block)
    expect(true, "expected a message containing '$message', got '${e.message}'") { e.message.orEmpty().contains(message) }
    return e
}

fun File.expectDirectory() {
    expect(true, "$this is not a directory") { isDirectory }
}

fun File.expectFile() {
    expect(true, "$this is not a file") { isFile }
}

fun File.expectNotExists() {
    expect(false, "$this exists") { exists() }
}

/**
 * Runs [block] with this directory's permissions narrowed, restoring them afterwards. Aborts the test when
 * the narrowing has no effect, e.g. when the tests run as root.
 */
fun File.withoutPermission(read: Boolean = false, write: Boolean = false, block: () -> Unit) {
    try {
        if (read) setReadable(false)
        if (write) setWritable(false)
        if ((read && canRead()) || (write && canWrite())) abort<Nothing>("can't narrow the permissions of $this")
        block()
    } finally {
        setReadable(true)
        setWritable(true)
    }
}

/**
 * A temp dir on a different file store than [of], so that a rename between them can't be atomic; null if the
 * machine has none.
 */
fun createTempDirOnOtherMount(of: File): File? {
    val shm = File("/dev/shm")
    if (!shm.isDirectory || !shm.canWrite()) return null
    if (Files.getFileStore(shm.toPath()) == Files.getFileStore(of.toPath())) return null
    return Files.createTempDirectory(shm.toPath(), "dirutils").toFile()
}

/**
 * Makes this a symlink pointing to [target], which may not exist.
 */
fun File.symlinkTo(target: File): File {
    Files.createSymbolicLink(toPath(), target.toPath())
    return this
}

/**
 * Unlike [expectNotExists], also catches a dangling symlink, which [File.exists] denies.
 */
fun File.expectNoEntry() {
    expect(false, "$this still exists") { Files.exists(toPath(), LinkOption.NOFOLLOW_LINKS) }
}

class TrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
    var closed = false
    override fun close() { closed = true }
}

class TrackingOutputStream : ByteArrayOutputStream() {
    var closed = false
    override fun close() { closed = true }
}

@ParameterizedClass
@EnumSource(FS::class)
class DirUtilsTest(private val fs: FS) {
    @TempDir
    lateinit var tempdir: File

    @BeforeEach
    fun installFS() {
        fs.install()
    }

    @Nested
    inner class RelativePathsAreRejected {
        @Test
        fun mkdir() {
            expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.mkdir("foo") }
        }

        @Test
        fun mkdirs() {
            expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.mkdirs("foo/bar") }
        }

        @Test
        fun deleteNonRec() {
            expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.deleteNonRec("foo") }
        }

        @Test
        fun deleteRecursively() {
            expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.deleteRecursively("foo") }
        }
    }

    @Nested
    inner class Mkdir {
        @Test
        fun `creates the directory`() {
            val dir = File(tempdir, "foo")
            DirUtils.mkdir(dir.absolutePath)
            dir.expectDirectory()
        }

        @Test
        fun `does nothing when the directory exists`() {
            val dir = File(tempdir, "foo")
            DirUtils.mkdir(dir)
            DirUtils.mkdir(dir)
            dir.expectDirectory()
        }

        @Test
        fun `fails when a file with the same name exists`() {
            val file = File(tempdir, "foo")
            file.writeText("")
            expectThrows(IOException::class, "foo' points to a file") { DirUtils.mkdir(file) }
        }

        @Test
        fun `does not create parents`() {
            val dir = File(tempdir, "foo/bar")
            expectThrows(IOException::class, "foo' does not exist") { DirUtils.mkdir(dir) }
            dir.parentFile.expectNotExists()
        }

        @Test
        fun `fails when the parent is a file`() {
            File(tempdir, "foo").writeText("")
            expectThrows(IOException::class, "foo' is not a directory") { DirUtils.mkdir(File(tempdir, "foo/bar")) }
        }

        @Test
        fun `fails in a read-only directory`() {
            tempdir.withoutPermission(write = true) {
                expectThrows(IOException::class, "$tempdir' is not writable") { DirUtils.mkdir(File(tempdir, "foo")) }
            }
        }

        @Test
        fun `the backend accepts a directory that appeared meanwhile`() {
            val dir = File(tempdir, "foo")
            dir.mkdir()
            DirUtils.get().mkdir(dir)
            dir.expectDirectory()
        }

        @Test
        fun `the backend still fails on a file`() {
            val file = File(tempdir, "foo")
            file.writeText("")
            expectThrows(IOException::class, "foo' points to a file") { DirUtils.get().mkdir(file) }
        }
    }

    @Nested
    inner class Mkdirs {
        @Test
        fun `creates the directory with parents`() {
            val dir = File(tempdir, "foo/bar/baz")
            DirUtils.mkdirs(dir.absolutePath)
            dir.expectDirectory()
        }
    }

    @Nested
    inner class DeleteNonRec {
        @Test
        fun `deletes a file`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            DirUtils.deleteNonRec(file.absolutePath)
            file.expectNotExists()
        }

        @Test
        fun `fails in a read-only directory`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            tempdir.withoutPermission(write = true) {
                expectThrows(IOException::class, "Failed to delete '$file': '$tempdir' is not writable") { DirUtils.deleteNonRec(file) }
            }
            file.expectFile()
        }

        @Test
        fun `fails on a non-empty dir`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            expectThrows(IOException::class, "Failed to delete '$dir': '$dir' is not empty") { DirUtils.deleteNonRec(dir) }
            dir.expectDirectory()
        }

        @Test
        fun `deletes a dangling symlink`() {
            val link = File(tempdir, "foo").symlinkTo(File(tempdir, "nowhere"))
            DirUtils.deleteNonRec(link)
            link.expectNoEntry()
        }

        @Test
        fun `deletes a symlink to a dir, not the dir`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            val link = File(tempdir, "link").symlinkTo(dir)
            DirUtils.deleteNonRec(link)
            link.expectNoEntry()
            File(dir, "bar").expectDirectory()
        }
    }

    @Nested
    inner class DeleteRecursively {
        @Test
        fun `deletes a non-empty dir`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            File(dir, "baz.txt").writeText("baz")
            DirUtils.deleteRecursively(dir.absolutePath)
            dir.expectNotExists()
        }

        @Test
        fun `fails on a dir it can't list`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            dir.withoutPermission(read = true) {
                expectThrows(IOException::class, "Failed to delete") { DirUtils.deleteRecursively(dir) }
            }
            dir.expectDirectory()
        }

        @Test
        fun `deletes a symlink inside, not what it points to`() {
            val victim = File(tempdir, "victim")
            victim.mkdirp()
            File(victim, "keep.txt").writeText("keep")
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            File(dir, "link").symlinkTo(victim)
            DirUtils.deleteRecursively(dir)
            dir.expectNotExists()
            expect("keep") { File(victim, "keep.txt").readText() }
        }

        @Test
        fun `deletes a dangling symlink inside`() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            File(dir, "dangling").symlinkTo(File(tempdir, "nowhere"))
            DirUtils.deleteRecursively(dir)
            dir.expectNotExists()
        }

        @Test
        fun `deletes a symlink it's given, not what it points to`() {
            val victim = File(tempdir, "victim")
            victim.mkdirp()
            File(victim, "keep.txt").writeText("keep")
            val link = File(tempdir, "link").symlinkTo(victim)
            DirUtils.deleteRecursively(link)
            link.expectNoEntry()
            expect("keep") { File(victim, "keep.txt").readText() }
        }

        @Test
        fun `deletes a dir reached through a symlinked parent`() {
            val real = File(tempdir, "real")
            File(real, "foo/bar").mkdirp()
            val link = File(tempdir, "link").symlinkTo(real)
            DirUtils.deleteRecursively(File(link, "foo"))
            File(real, "foo").expectNotExists()
            real.expectDirectory()
        }
    }

    @Nested
    inner class Quietly {
        @Test
        fun `deleteNonRecQuietly deletes a file`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            DirUtils.deleteNonRecQuietly(file.absolutePath)
            file.expectNotExists()
        }

        @Test
        fun `deleteNonRecQuietly doesn't throw on a non-empty dir`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            DirUtils.deleteNonRecQuietly(dir)
            dir.expectDirectory()
        }

        @Test
        fun `deleteRecursivelyQuietly deletes a non-empty dir`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            DirUtils.deleteRecursivelyQuietly(dir.absolutePath)
            dir.expectNotExists()
        }

        @Test
        fun `deleteRecursivelyQuietly doesn't throw when the delete fails`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            tempdir.withoutPermission(write = true) {
                DirUtils.deleteRecursivelyQuietly(file)
            }
            file.expectFile()
        }
    }

    @Nested
    inner class Rename {
        @Test
        fun `renames a file`() {
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val target = File(tempdir, "bar")
            DirUtils.rename(source, target)
            source.expectNotExists()
            expect("foo") { target.readText() }
        }

        @Test
        fun `overwrites the target file`() {
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val target = File(tempdir, "bar")
            target.writeText("bar")
            DirUtils.rename(source, target)
            source.expectNotExists()
            expect("foo") { target.readText() }
        }

        @Test
        fun `does nothing when renaming to itself`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            DirUtils.rename(file, File(file.absolutePath))
            expect("foo") { file.readText() }
        }

        @Test
        fun `fails when the target is a non-empty dir`() {
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val target = File(tempdir, "bar")
            File(target, "baz").mkdirp()
            expectThrows(IOException::class, "Failed to delete") { DirUtils.rename(source, target) }
            source.expectFile()
        }

        @Test
        fun `fails when the source doesn't exist, leaving the target alone`() {
            val source = File(tempdir, "foo")
            val target = File(tempdir, "bar")
            target.writeText("bar")
            expectThrows(IOException::class, "foo' does not exist") { DirUtils.rename(source, target) }
            expect("bar") { target.readText() }
        }

        @Test
        fun `fails into a read-only directory`() {
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val targetDir = File(tempdir, "bar")
            targetDir.mkdirp()
            val target = File(targetDir, "baz")
            targetDir.withoutPermission(write = true) {
                expectThrows(IOException::class, "Failed to rename '$source' to '$target': '$targetDir' is not writable") {
                    DirUtils.rename(source, target)
                }
            }
            source.expectFile()
            target.expectNotExists()
        }

        @Test
        fun `the backend fails on a missing source rather than asking for a copy`() {
            expectThrows(IOException::class, "foo' does not exist") {
                DirUtils.get().rename(File(tempdir, "foo"), File(tempdir, "bar"))
            }
        }

        @Test
        fun `copies and deletes when the backend can't rename atomically`() {
            val backend = DirUtils.get()
            DirUtils.set(object : FileSystemSpi by backend {
                override fun rename(source: File, target: File): Boolean = false
            })
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val target = File(tempdir, "bar")
            DirUtils.rename(source, target)
            source.expectNotExists()
            expect("foo") { target.readText() }
        }

        @Test
        fun `moves across mount points`() {
            val otherDir = createTempDirOnOtherMount(tempdir) ?: abort("no writable /dev/shm on another file store")
            try {
                val source = File(tempdir, "foo")
                source.writeText("foo")
                val target = File(otherDir, "bar")
                DirUtils.rename(source, target)
                source.expectNotExists()
                expect("foo") { target.readText() }
            } finally {
                otherDir.deleteRecursively()
            }
        }
    }

    @Nested
    inner class Copy {
        @Test
        fun files() {
            val source = File(tempdir, "foo")
            source.writeText("foo")
            val target = File(tempdir, "bar")
            DirUtils.copy(source, target)
            expect("foo") { source.readText() }
            expect("foo") { target.readText() }
        }

        @Test
        fun `fails when the source file doesn't exist`() {
            val target = File(tempdir, "bar")
            expectThrows(FileNotFoundException::class) {
                DirUtils.copy(File(tempdir, "foo"), target)
            }
            target.expectNotExists()
        }

        @Test
        fun `closes the input stream only`() {
            val bytes = ByteArray(20000) { it.toByte() }
            val input = TrackingInputStream(bytes)
            val output = TrackingOutputStream()
            DirUtils.copy(input, output)
            expect(bytes.toList()) { output.toByteArray().toList() }
            expect(true) { input.closed }
            expect(false) { output.closed }
        }

        @Test
        fun `closes the streams as requested`() {
            val input = TrackingInputStream("foo".toByteArray())
            val output = TrackingOutputStream()
            DirUtils.copy(input, output, false, true)
            expect("foo") { output.toString() }
            expect(false) { input.closed }
            expect(true) { output.closed }
        }

        @Test
        fun `keeps reading after a zero-byte read`() {
            var zeroReadDone = false
            val input = object : ByteArrayInputStream("foo".toByteArray()) {
                override fun read(b: ByteArray): Int {
                    if (!zeroReadDone) { zeroReadDone = true; return 0 }
                    return super.read(b)
                }
            }
            val output = ByteArrayOutputStream()
            DirUtils.copy(input, output)
            expect("foo") { output.toString() }
        }

        @Test
        fun `a failing flush doesn't fail the copy`() {
            val output = object : ByteArrayOutputStream() {
                override fun flush() = throw IOException("simulated")
            }
            DirUtils.copy(ByteArrayInputStream("foo".toByteArray()), output)
            expect("foo") { output.toString() }
        }
    }

    @Nested
    inner class CloseQuietly {
        @Test
        fun `null`() {
            DirUtils.closeQuietly(null)
        }

        @Test
        fun closes() {
            val input = TrackingInputStream(byteArrayOf())
            DirUtils.closeQuietly(input)
            expect(true) { input.closed }
        }

        @Test
        fun `swallows the failure`() {
            DirUtils.closeQuietly { throw IOException("simulated") }
        }
    }

    @Nested
    inner class IsSymlink {
        @Test
        fun `a file isn't`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            expect(false) { DirUtils.get().isSymlink(file) }
        }

        @Test
        fun `a dir isn't`() {
            expect(false) { DirUtils.get().isSymlink(tempdir) }
        }

        @Test
        fun `a missing file isn't`() {
            expect(false) { DirUtils.get().isSymlink(File(tempdir, "foo")) }
        }

        @Test
        fun `the root isn't`() {
            expect(false) { DirUtils.get().isSymlink(File("/")) }
        }

        @Test
        fun `a symlink to a file is`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            expect(true) { DirUtils.get().isSymlink(File(tempdir, "link").symlinkTo(file)) }
        }

        @Test
        fun `a symlink to a dir is`() {
            expect(true) { DirUtils.get().isSymlink(File(tempdir, "link").symlinkTo(tempdir)) }
        }

        @Test
        fun `a dangling symlink is`() {
            expect(true) { DirUtils.get().isSymlink(File(tempdir, "link").symlinkTo(File(tempdir, "nowhere"))) }
        }

        @Test
        fun `a file under a symlinked dir isn't`() {
            val real = File(tempdir, "real")
            real.mkdirp()
            File(real, "foo").writeText("foo")
            val link = File(tempdir, "link").symlinkTo(real)
            expect(false) { DirUtils.get().isSymlink(File(link, "foo")) }
        }
    }

    @Nested
    inner class Spi {
        @Test
        fun `auto-detects Java7FS on a JVM`() {
            DirUtils.set(null)
            expect(true) { DirUtils.get() is Java7FS }
            expect(true) { DirUtils.isJava7Avail() }
        }
    }
}
