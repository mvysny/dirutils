package sk.baka.android

import com.github.mvysny.dynatest.*
import sk.baka.android.spi.DumbJavaFS
import sk.baka.android.spi.Java7FS
import java.io.File
import java.io.IOException

fun DynaNodeGroup.withAllFS(block: DynaNodeGroup.() -> Unit) {
    group("DumbJavaFS") {
        DirUtils.set(DumbJavaFS())
        block()
    }
    group("Java7FS") {
        DirUtils.set(Java7FS())
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
                expectThrows(IOException::class) { dir.mkdirp() }
            }
        }
    }
})
