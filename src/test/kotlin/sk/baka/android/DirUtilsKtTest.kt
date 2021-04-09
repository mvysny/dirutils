package sk.baka.android

import com.github.mvysny.dynatest.*
import sk.baka.android.spi.DumbJavaFS
import sk.baka.android.spi.Java7FS
import java.io.File
import java.io.IOException
import kotlin.test.expect

fun DynaNodeGroup.withAllFS(block: DynaNodeGroup.() -> Unit) {
    group("DumbJavaFS") {
        beforeEach {
            DirUtils.set(DumbJavaFS())
            DirUtils.get() as DumbJavaFS
        }
        block()
    }
    group("Java7FS") {
        beforeEach {
            DirUtils.set(Java7FS())
            DirUtils.get() as Java7FS
        }
        block()
    }
}

class DirUtilsKtTest : DynaTest({
    withAllFS {
        val tempdir: File by withTempDir("tempdir")
        group("mkdirp") {
            test("simple") {
                val dir = File(tempdir, "foo")
                dir.mkdirp()
                dir.expectDirectory()
            }
            test("multiple dirs") {
                val dir = File(tempdir, "foo/bar/baz")
                dir.mkdirp()
                dir.expectDirectory()
            }
            test("fails when a file with the same name exists") {
                val dir = File(tempdir, "foo")
                dir.writeText("")
                dir.expectFile()
                expectThrows(IOException::class, "foo' points to a file") {
                    dir.mkdirp()
                }
            }
        }
        group("rmf") {
            test("empty dir") {
                val dir = File(tempdir, "foo")
                dir.mkdirp()
                dir.rmf()
                expect(false) { dir.exists() }
            }
            test("empty file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                file.rmf()
                expect(false) { file.exists() }
            }
            test("do nothing on non-existent file/dir") {
                val file = File(tempdir, "foo")
                file.rmf()
                expect(false) { file.exists() }
            }
            test("fail on non-empty dir") {
                val dir = File(tempdir, "foo/bar")
                dir.mkdirp()
                expectThrows(IOException::class, "Failed to delete") {
                    File(tempdir, "foo").rmf()
                }
            }
        }
    }
})
