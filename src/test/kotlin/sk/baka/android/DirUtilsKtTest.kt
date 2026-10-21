package sk.baka.android

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedClass
import org.junit.jupiter.params.provider.EnumSource
import sk.baka.android.spi.FileSystemSpi
import java.io.File
import java.io.IOException
import kotlin.test.expect

/**
 * The [FileSystemSpi] backends. Every suite is a `@ParameterizedClass` over this enum, so that it runs once per
 * backend.
 */
enum class FS(private val create: () -> FileSystemSpi) {
    DumbJavaFS({ sk.baka.android.spi.DumbJavaFS() }),
    Java7FS({ sk.baka.android.spi.Java7FS() });

    fun install() {
        DirUtils.set(create())
    }
}

@ParameterizedClass
@EnumSource(FS::class)
class DirUtilsKtTest(private val fs: FS) {
    @TempDir
    lateinit var tempdir: File

    @BeforeEach
    fun installFS() {
        fs.install()
    }

    @Nested
    inner class Mkdirp {
        @Test
        fun simple() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            dir.expectDirectory()
        }

        @Test
        fun `multiple dirs`() {
            val dir = File(tempdir, "foo/bar/baz")
            dir.mkdirp()
            dir.expectDirectory()
        }

        @Test
        fun `fails when a file with the same name exists`() {
            val dir = File(tempdir, "foo")
            dir.writeText("")
            dir.expectFile()
            expectThrows(IOException::class, "foo' points to a file") {
                dir.mkdirp()
            }
        }
    }

    @Nested
    inner class Rmf {
        @Test
        fun `empty dir`() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            dir.rmf()
            expect(false) { dir.exists() }
        }

        @Test
        fun `empty file`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            file.rmf()
            expect(false) { file.exists() }
        }

        @Test
        fun `do nothing on non-existent file or dir`() {
            val file = File(tempdir, "foo")
            file.rmf()
            expect(false) { file.exists() }
        }

        @Test
        fun `fail on non-empty dir`() {
            val dir = File(tempdir, "foo/bar")
            dir.mkdirp()
            expectThrows(IOException::class, "Failed to delete") {
                File(tempdir, "foo").rmf()
            }
        }
    }

    @Nested
    inner class Rmrf {
        @Test
        fun `empty dir`() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            dir.rmrf()
            expect(false) { dir.exists() }
        }

        @Test
        fun `empty file`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            file.rmrf()
            expect(false) { file.exists() }
        }

        @Test
        fun `do nothing on non-existent file or dir`() {
            val file = File(tempdir, "foo")
            file.rmrf()
            expect(false) { file.exists() }
        }

        @Test
        fun `deletes non-empty dir as well`() {
            val dir = File(tempdir, "foo/bar")
            dir.mkdirp()
            dir.parentFile.rmrf()
            expect(false) { dir.parentFile.exists() }
        }
    }

    @Nested
    inner class Rmrfq {
        @Test
        fun `empty dir`() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            dir.rmrfq()
            expect(false) { dir.exists() }
        }

        @Test
        fun `empty file`() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            file.rmrfq()
            expect(false) { file.exists() }
        }

        @Test
        fun `do nothing on non-existent file or dir`() {
            val file = File(tempdir, "foo")
            file.rmrfq()
            expect(false) { file.exists() }
        }

        @Test
        fun `deletes non-empty dir as well`() {
            val dir = File(tempdir, "foo/bar")
            dir.mkdirp()
            dir.parentFile.rmrfq()
            expect(false) { dir.parentFile.exists() }
        }
    }

    @Nested
    inner class CalculateLengthRecursively {
        @Test
        fun `non-existing`() {
            val dir = File(tempdir, "foo")
            expect(false) { dir.exists() }
            expect(0) { dir.calculateLengthRecursively() }
        }

        @Test
        fun file() {
            val file = File(tempdir, "foo")
            file.writeText("foo")
            expect(3) { file.calculateLengthRecursively() }
        }

        @Test
        fun `empty dir`() {
            val file = File(tempdir, "foo")
            file.mkdirp()
            expect(4096) { file.calculateLengthRecursively() }
        }

        @Test
        fun `nested empty dirs`() {
            val dir = File(tempdir, "foo/bar/baz")
            dir.mkdirp()
            expect(12288) { File(tempdir, "foo").calculateLengthRecursively() }
        }

        @Test
        fun `nested dirs with a file`() {
            val dir = File(tempdir, "foo/bar/baz")
            dir.mkdirp()
            File(dir, "foo.txt").writeText("foo!")
            expect(12292) { File(tempdir, "foo").calculateLengthRecursively() }
        }

        @Test
        fun `dir it can't list`() {
            val dir = File(tempdir, "foo")
            File(dir, "bar").mkdirp()
            dir.withoutPermission(read = true) {
                expect(4096) { dir.calculateLengthRecursively() }
            }
        }

        @Test
        fun `doesn't follow a symlink`() {
            val victim = File(tempdir, "victim")
            victim.mkdirp()
            File(victim, "big.txt").writeText("x".repeat(10000))
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            File(dir, "foo.txt").writeText("foo")
            File(dir, "link").symlinkTo(victim)
            File(dir, "filelink").symlinkTo(File(victim, "big.txt"))
            expect(4099) { dir.calculateLengthRecursively() }
        }

        @Test
        fun `survives a symlink loop`() {
            val dir = File(tempdir, "foo")
            dir.mkdirp()
            File(dir, "loop").symlinkTo(dir)
            expect(4096) { dir.calculateLengthRecursively() }
        }

        @Test
        fun `neither file nor dir`() {
            val devNull = File("/dev/null")
            assumeTrue(devNull.exists(), "no /dev/null")
            expect(0) { devNull.calculateLengthRecursively() }
        }
    }
}
