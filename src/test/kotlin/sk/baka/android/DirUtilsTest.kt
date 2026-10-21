package sk.baka.android

import com.github.mvysny.dynatest.*
import sk.baka.android.spi.FileSystemSpi
import sk.baka.android.spi.Java7FS
import java.io.*
import java.nio.file.Files
import kotlin.test.expect

/**
 * Runs [block] with this directory's permissions narrowed, restoring them afterwards. Skips [block] when the
 * narrowing has no effect, e.g. when the tests run as root.
 */
fun File.withoutPermission(read: Boolean = false, write: Boolean = false, block: () -> Unit) {
    try {
        if (read) setReadable(false)
        if (write) setWritable(false)
        if ((read && canRead()) || (write && canWrite())) return
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

class TrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
    var closed = false
    override fun close() { closed = true }
}

class TrackingOutputStream : ByteArrayOutputStream() {
    var closed = false
    override fun close() { closed = true }
}

class DirUtilsTest : DynaTest({
    withAllFS {
        val tempdir: File by withTempDir("tempdir")

        group("relative paths are rejected") {
            test("mkdir") {
                expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.mkdir("foo") }
            }
            test("mkdirs") {
                expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.mkdirs("foo/bar") }
            }
            test("deleteNonRec") {
                expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.deleteNonRec("foo") }
            }
            test("deleteRecursively") {
                expectThrows(IllegalArgumentException::class, "must be an absolute path") { DirUtils.deleteRecursively("foo") }
            }
        }

        group("mkdir") {
            test("creates the directory") {
                val dir = File(tempdir, "foo")
                DirUtils.mkdir(dir.absolutePath)
                dir.expectDirectory()
            }
            test("does nothing when the directory exists") {
                val dir = File(tempdir, "foo")
                DirUtils.mkdir(dir)
                DirUtils.mkdir(dir)
                dir.expectDirectory()
            }
            test("fails when a file with the same name exists") {
                val file = File(tempdir, "foo")
                file.writeText("")
                expectThrows(IOException::class, "foo' points to a file") { DirUtils.mkdir(file) }
            }
            test("does not create parents") {
                val dir = File(tempdir, "foo/bar")
                expectThrows(IOException::class, "foo' does not exist") { DirUtils.mkdir(dir) }
                dir.parentFile.expectNotExists()
            }
            test("fails when the parent is a file") {
                File(tempdir, "foo").writeText("")
                expectThrows(IOException::class, "foo' is not a directory") { DirUtils.mkdir(File(tempdir, "foo/bar")) }
            }
            test("fails in a read-only directory") {
                tempdir.withoutPermission(write = true) {
                    expectThrows(IOException::class, "$tempdir' is not writable") { DirUtils.mkdir(File(tempdir, "foo")) }
                }
            }
            test("the backend accepts a directory that appeared meanwhile") {
                val dir = File(tempdir, "foo")
                dir.mkdir()
                DirUtils.get().mkdir(dir)
                dir.expectDirectory()
            }
            test("the backend still fails on a file") {
                val file = File(tempdir, "foo")
                file.writeText("")
                expectThrows(IOException::class, "foo' points to a file") { DirUtils.get().mkdir(file) }
            }
        }

        group("mkdirs") {
            test("creates the directory with parents") {
                val dir = File(tempdir, "foo/bar/baz")
                DirUtils.mkdirs(dir.absolutePath)
                dir.expectDirectory()
            }
        }

        group("deleteNonRec") {
            test("deletes a file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                DirUtils.deleteNonRec(file.absolutePath)
                file.expectNotExists()
            }
            test("fails in a read-only directory") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                tempdir.withoutPermission(write = true) {
                    expectThrows(IOException::class, "Failed to delete '$file': '$tempdir' is not writable") { DirUtils.deleteNonRec(file) }
                }
                file.expectFile()
            }
            test("fails on a non-empty dir") {
                val dir = File(tempdir, "foo")
                File(dir, "bar").mkdirp()
                expectThrows(IOException::class, "Failed to delete '$dir': '$dir' is not empty") { DirUtils.deleteNonRec(dir) }
                dir.expectDirectory()
            }
        }

        group("deleteRecursively") {
            test("deletes a non-empty dir") {
                val dir = File(tempdir, "foo")
                File(dir, "bar").mkdirp()
                File(dir, "baz.txt").writeText("baz")
                DirUtils.deleteRecursively(dir.absolutePath)
                dir.expectNotExists()
            }
            test("fails on a dir it can't list") {
                val dir = File(tempdir, "foo")
                File(dir, "bar").mkdirp()
                dir.withoutPermission(read = true) {
                    expectThrows(IOException::class, "Failed to delete") { DirUtils.deleteRecursively(dir) }
                }
                dir.expectDirectory()
            }
        }

        group("quietly") {
            test("deleteNonRecQuietly deletes a file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                DirUtils.deleteNonRecQuietly(file.absolutePath)
                file.expectNotExists()
            }
            test("deleteNonRecQuietly doesn't throw on a non-empty dir") {
                val dir = File(tempdir, "foo")
                File(dir, "bar").mkdirp()
                DirUtils.deleteNonRecQuietly(dir)
                dir.expectDirectory()
            }
            test("deleteRecursivelyQuietly deletes a non-empty dir") {
                val dir = File(tempdir, "foo")
                File(dir, "bar").mkdirp()
                DirUtils.deleteRecursivelyQuietly(dir.absolutePath)
                dir.expectNotExists()
            }
            test("deleteRecursivelyQuietly doesn't throw when the delete fails") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                tempdir.withoutPermission(write = true) {
                    DirUtils.deleteRecursivelyQuietly(file)
                }
                file.expectFile()
            }
        }

        group("rename") {
            test("renames a file") {
                val source = File(tempdir, "foo")
                source.writeText("foo")
                val target = File(tempdir, "bar")
                DirUtils.rename(source, target)
                source.expectNotExists()
                expect("foo") { target.readText() }
            }
            test("overwrites the target file") {
                val source = File(tempdir, "foo")
                source.writeText("foo")
                val target = File(tempdir, "bar")
                target.writeText("bar")
                DirUtils.rename(source, target)
                source.expectNotExists()
                expect("foo") { target.readText() }
            }
            test("does nothing when renaming to itself") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                DirUtils.rename(file, File(file.absolutePath))
                expect("foo") { file.readText() }
            }
            test("fails when the target is a non-empty dir") {
                val source = File(tempdir, "foo")
                source.writeText("foo")
                val target = File(tempdir, "bar")
                File(target, "baz").mkdirp()
                expectThrows(IOException::class, "Failed to delete") { DirUtils.rename(source, target) }
                source.expectFile()
            }
            test("fails when the source doesn't exist, leaving the target alone") {
                val source = File(tempdir, "foo")
                val target = File(tempdir, "bar")
                target.writeText("bar")
                expectThrows(IOException::class, "foo' does not exist") { DirUtils.rename(source, target) }
                expect("bar") { target.readText() }
            }
            test("fails into a read-only directory") {
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
            test("the backend fails on a missing source rather than asking for a copy") {
                expectThrows(IOException::class, "foo' does not exist") {
                    DirUtils.get().rename(File(tempdir, "foo"), File(tempdir, "bar"))
                }
            }
            test("copies and deletes when the backend can't rename atomically") {
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
            test("moves across mount points") {
                val otherDir = createTempDirOnOtherMount(tempdir) ?: return@test
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

        group("copy") {
            test("files") {
                val source = File(tempdir, "foo")
                source.writeText("foo")
                val target = File(tempdir, "bar")
                DirUtils.copy(source, target)
                expect("foo") { source.readText() }
                expect("foo") { target.readText() }
            }
            test("fails when the source file doesn't exist") {
                val target = File(tempdir, "bar")
                expectThrows(FileNotFoundException::class) {
                    DirUtils.copy(File(tempdir, "foo"), target)
                }
                target.expectNotExists()
            }
            test("closes the input stream only") {
                val bytes = ByteArray(20000) { it.toByte() }
                val input = TrackingInputStream(bytes)
                val output = TrackingOutputStream()
                DirUtils.copy(input, output)
                expect(bytes.toList()) { output.toByteArray().toList() }
                expect(true) { input.closed }
                expect(false) { output.closed }
            }
            test("closes the streams as requested") {
                val input = TrackingInputStream("foo".toByteArray())
                val output = TrackingOutputStream()
                DirUtils.copy(input, output, false, true)
                expect("foo") { output.toString() }
                expect(false) { input.closed }
                expect(true) { output.closed }
            }
            test("keeps reading after a zero-byte read") {
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
            test("a failing flush doesn't fail the copy") {
                val output = object : ByteArrayOutputStream() {
                    override fun flush() = throw IOException("simulated")
                }
                DirUtils.copy(ByteArrayInputStream("foo".toByteArray()), output)
                expect("foo") { output.toString() }
            }
        }

        group("closeQuietly") {
            test("null") {
                DirUtils.closeQuietly(null)
            }
            test("closes") {
                val input = TrackingInputStream(byteArrayOf())
                DirUtils.closeQuietly(input)
                expect(true) { input.closed }
            }
            test("swallows the failure") {
                DirUtils.closeQuietly { throw IOException("simulated") }
            }
        }

        group("SPI") {
            test("auto-detects Java7FS on a JVM") {
                DirUtils.set(null)
                expect(true) { DirUtils.get() is Java7FS }
                expect(true) { DirUtils.isJava7Avail() }
            }
        }
    }
})
