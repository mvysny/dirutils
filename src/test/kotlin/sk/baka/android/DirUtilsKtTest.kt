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
        group("rmrf") {
            test("empty dir") {
                val dir = File(tempdir, "foo")
                dir.mkdirp()
                dir.rmrf()
                expect(false) { dir.exists() }
            }
            test("empty file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                file.rmrf()
                expect(false) { file.exists() }
            }
            test("do nothing on non-existent file/dir") {
                val file = File(tempdir, "foo")
                file.rmrf()
                expect(false) { file.exists() }
            }
            test("deletes non-empty dir as well") {
                val dir = File(tempdir, "foo/bar")
                dir.mkdirp()
                dir.parentFile.rmrf()
                expect(false) { dir.parentFile.exists() }
            }
        }
        group("rmrfq") {
            test("empty dir") {
                val dir = File(tempdir, "foo")
                dir.mkdirp()
                dir.rmrfq()
                expect(false) { dir.exists() }
            }
            test("empty file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                file.rmrfq()
                expect(false) { file.exists() }
            }
            test("do nothing on non-existent file/dir") {
                val file = File(tempdir, "foo")
                file.rmrfq()
                expect(false) { file.exists() }
            }
            test("deletes non-empty dir as well") {
                val dir = File(tempdir, "foo/bar")
                dir.mkdirp()
                dir.parentFile.rmrfq()
                expect(false) { dir.parentFile.exists() }
            }
        }
        group("calculateLengthRecursively") {
            test("non-existing") {
                val dir = File(tempdir, "foo")
                expect(false) { dir.exists() }
                expect(0) { dir.calculateLengthRecursively() }
            }
            test("file") {
                val file = File(tempdir, "foo")
                file.writeText("foo")
                expect(3) { file.calculateLengthRecursively() }
            }
            test("empty dir") {
                val file = File(tempdir, "foo")
                file.mkdirp()
                expect(4096) { file.calculateLengthRecursively() }
            }
            test("nested empty dirs") {
                val dir = File(tempdir, "foo/bar/baz")
                dir.mkdirp()
                expect(12288) { File(tempdir, "foo").calculateLengthRecursively() }
            }
            test("nested dirs with a file") {
                val dir = File(tempdir, "foo/bar/baz")
                dir.mkdirp()
                File(dir, "foo.txt").writeText("foo!")
                expect(12292) { File(tempdir, "foo").calculateLengthRecursively() }
            }
        }
    }
})
