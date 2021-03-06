package com.rgm.file


import java.nio.file.{Path =>JPath, _}
import org.scalatest.{FlatSpec, Suite, BeforeAndAfterEach}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Random}
import java.net.URI
import java.util
import java.io.{IOException, FileOutputStream, PrintStream}
import java.nio.file.attribute.PosixFilePermission._
import java.nio.file.attribute.PosixFilePermission


class FileIOSpec extends FlatSpec with FileSetupTeardown {

  behavior of "File System"

  //copyTo test
  it should "1. copy file to target location correctly" in {
    for(i <- filsGlobal.toList) {
      val tmp = Path(i)
      val trgt = Path(FileSystems.getDefault.getPath(targetGlobal + i.toString.split("/").last))
      try {
        tmp.copyTo(trgt)
      }
      catch {
        case nsfe: NoSuchFileException => assert(false)
      }
    }
    for(x <- filsGlobal.toList) {
      val tmp = new Path(FileSystems.getDefault.getPath(targetGlobal + x.toString.split("/").last))
      assert(tmp.exists() && tmp.isFile() && (tmp.size().get == 0))
    }
    flagGlobal = true
  }

  //moveTo test
  it should "2. moveFile to target location correctly" in {
    for(i <- filsGlobal.toList) {
      val tmp = new Path(i)
      try {
        tmp.moveFile(Path(targetGlobal + i.toString.split("/").last))
      }
      catch {
        case nsfe: NoSuchFileException => assert(false)
      }
    }
    for(x <- filsGlobal.toList) {
      val tmp = new Path(FileSystems.getDefault.getPath(targetGlobal + x.toString.split("/").last))
      val tmp2 = new Path(x)
      assert(tmp.exists() && tmp.isFile() && tmp2.nonExistent() && (tmp.size().get == 0))
    }
    flagGlobal = true
  }

  //moveDirectory test
  it should "3. move directory to target location correctly" in {
    for(i <- dirsGlobal.toList) {
      val tmp = new Path(i)
      try {
        tmp.moveDirectory(Path(targetGlobal + i.toString.split("/").last))
      }
      catch {
        case nsfe: NoSuchFileException => assert(false)
      }
    }
    for(x <- dirsGlobal.toList) {
      val tmp = new Path(FileSystems.getDefault.getPath(targetGlobal + x.toString.split("/").last))
      val tmp2 = new Path(x)
      assert(tmp.exists() && tmp.isDirectory() && tmp2.nonExistent())
    }
    flagGlobal = true
  }


  //deleteRecursively test
  it should "4. recursively delete the 'src' directory where the file tree is constructed" in {
    val p = new Path(FileSystems.getDefault.getPath(srcGlobal))
    p.deleteRecursively()
    assert(p.nonExistent())
    flagGlobal = true
  }

  //createTempFile test
  it should "5. create temp file in target and check its existence" in {
    val p = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    assert(p.exists())
    flagGlobal = true
  }

  //createTempDir test
  it should "6. create temp dir in target and check its existence" in {
    val p = Path.createTempDirectory(Path(FileSystems.getDefault.getPath(targetGlobal)), "test")
    assert(p.exists())
    flagGlobal = true
  }

  //delete test
  it should "7. create a temp file then delete it and check its existence" in {
    val p = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    p.delete()
    assert(p.nonExistent())
    flagGlobal = true
  }

  //deleteIfExists test
  it should "8. delete a file if it exists else fail" in {
    val p = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    p.delete()
    assert(p.nonExistent())
    flagGlobal = true
  }

  //createDirectory
  it should "9. create a directory and check its existence" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test")).createDirectory(false)
    assert(p.exists())
    flagGlobal = true
  }

  //createFile
  it should "10. create a file and check its existence" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(p.exists())
    flagGlobal = true
  }

  //isSame test
  it should "11. check if the file is the same" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(p.isSame(p))
    flagGlobal = true
  }

  //size test
  it should "12. ensure temp file size is 0" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(p.size().get === 0)
    flagGlobal = true
  }

  //isReadable test
  it should "13. create a temp file and check if it is readable-> true" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(p.isReadable())
    flagGlobal = true
  }

  //isWritable test
  it should "14. create a temp file and check if it is writable-> true" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(p.isWritable())
    flagGlobal = true
  }

  //isExecutable test
  it should "15. create a temp file and check if it is executable-> false" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    assert(!p.isExecutable)
    flagGlobal = true
  }

  //isSymbolicLink test
  it should "16. creates a SymLink using NIO and ensures it is a symbolic link" in {
    val p = new Path(FileSystems.getDefault.getPath(targetGlobal + "tmp.link"))
    val q = new Path(FileSystems.getDefault.getPath(targetGlobal + "test.tmp")).createFile(false)
    Files.createSymbolicLink(p.jpath,q.jpath)
    assert(p.isSymLink())
    flagGlobal = true
  }

  //checkAccess test=create tmp file(only read & write access) -> ensure READ/WRITE and no EXECUTE
  it should "17. creates a tmp file and checks permissions" in {
    val p = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    assert(p.checkAccess(AccessMode.READ) && p.checkAccess(AccessMode.WRITE) && !p.checkAccess(AccessMode.EXECUTE))
    flagGlobal = true
  }

  //access sets access modes for the given path
  it should "18. set the correct posix access modes" in {
    val assigningPerms = Set(GROUP_EXECUTE, GROUP_READ, OWNER_READ, OTHERS_WRITE)
    val complementPerms = Set(GROUP_WRITE, OWNER_WRITE, OWNER_EXECUTE, OTHERS_EXECUTE, OTHERS_READ)

    val tmp = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    tmp.posixFilePerm_=(assigningPerms)
    val actualPerms = tmp.posixFilePerm()
    for (perm <- actualPerms)
      assert(assigningPerms contains perm)
    for (perm <- actualPerms)
      assert(!(complementPerms contains perm))
    flagGlobal = true
  }

  it should "19. not resolve symbolic links in toRealPath iff NOFOLLOW_LINKS option is used " in {
    val p = Path(FileSystems.getDefault.getPath(targetGlobal + "tmp.link"))
    val q = Path(FileSystems.getDefault.getPath(targetGlobal + "testDir/")).createDirectory(false)
    Files.createSymbolicLink(p.jpath, q.jpath)
    val pChild = p.resolve("targetFile")
    val qChild = q.resolve("targetFile").createFile(false)
    val shouldFail = Try(pChild.toRealPath(LinkOption.NOFOLLOW_LINKS))
    val shouldSucceed = Try(pChild.toRealPath())
    assert(shouldFail.get.toString == pChild.path)
    assert(shouldSucceed.get.toString == qChild.toRealPath().toString)
    flagGlobal = true
  }

  it should "20. copy then copy with replace" in {
    for(i <- filsGlobal.toList) {
      val tmp = new Path(i)
      try {
        tmp.copyTo(Path(targetGlobal + i.toString.split("/").last))
      }
      catch {
        case nsfe: NoSuchFileException => assert(false)
      }
    }

    for(i <- filsGlobal.toList) {
      val tmp = new Path(i)
      try {
        tmp.copyTo(Path(targetGlobal + i.toString.split("/").last), StandardCopyOption.REPLACE_EXISTING)
      }
      catch {
        case nsfe: NoSuchFileException => assert(false)
      }
    }

    for(x <- filsGlobal.toList) {
      val tmp = new Path(FileSystems.getDefault.getPath(targetGlobal + x.toString.split("/").last))
      val tmp2 = new Path(x)
      assert(tmp.exists() && tmp.isFile() && tmp2.exists() && (tmp.size().get == 0))
    }
    flagGlobal = true
  }

  it should "21. Handle zip files" in {
    val zipFile = Paths.get("src/test/resources/dir1.zip")
    Path(zipFile).deleteIfExists()
    val uri = URI.create("jar:file:" + zipFile.toUri.getPath)
    val env:  util.Map[String, String] = new util.HashMap[String, String]()
    env.put("create", "true")
    val zipSystem = FileSystem(FileSystems.newFileSystem(uri, env))
    val p = Path("/")(zipSystem)

    //test create and copy files zip-> unix
    val pth = Path.createTempFile(p, "test",".tmp")
    Path("src/test/resources/tmpCopy").deleteIfExists()
    pth.copyTo(Path("src/test/resources/tmpCopy"))
    assert(pth.exists() && Path("src/test/resources/tmpCopy").exists())

    //test create /tmpDir/file.tmp -> move to unix fileSystem
    val d = Path.createTempDirectory(p, "tmpDir")
    d.exists()
    val dst = Path("/tmpDir")(zipSystem)
    d.moveDirectory(dst)
    assert(dst.exists())
    zipSystem.close
    flagGlobal = true
  }

  it should "22. check file io stream" in {
    val p = Path((new java.io.File( "." ).getCanonicalPath).toString + "/src/test/resources/file.tmp")///Users/sshivaprasad/Documents/src/zpath/src/main/scala/com/rgm/file/Path.scala")
    p.deleteIfExists()
    val ps = new java.io.PrintStream(p.outputStream())
    ps.print("String")
    ps.close()
    assert(p.inputStream().read() == 83)
    flagGlobal = true
  }

  it should "23. ensure size called on nonExistent file returns None and NOT Some(0)" in {
    val p = Path("/foo/bar/tmp.txt")
    assert(p.size() == None)
    flagGlobal = true
  }

  it should "24. ensure parent directories are created when createParents=true in createDirectory" in {
    val tmp1 = Path(targetGlobal + "tmp_dir/tmp_dir2")
    val tmp2 = Path(targetGlobal + "tmp_dir")
    val p = Path(targetGlobal + "tmp_dir/tmp_dir2/tmp_dir3")
    p.createDirectory(true)
    assert(tmp1.exists() && tmp2.exists())
    flagGlobal = true
  }

  it should "25. fail if it exists in createDirectory" in {
    val p = Path(targetGlobal + "tmp_dir")
    p.createDirectory(false)
    val tmp = p
    try {
      tmp.createDirectory(false,true)
    } catch {
      case e: IOException => assert(true)
      case _: Throwable => assert(false)
    }
    flagGlobal = true
  }

  it should "24. ensure parent directories are created when createParents=true in createFile" in {
    val tmp1 = Path(targetGlobal + "tmp_dir/tmp_dir2")
    val tmp2 = Path(targetGlobal + "tmp_dir")
    val p = Path(targetGlobal + "tmp_dir/tmp_dir2/tmp_dir3.txt")
    p.createFile(true)
    assert(tmp1.exists() && tmp2.exists())
    flagGlobal = true
  }

  it should "25. fail if it exists in createFile" in {
    val p = Path(targetGlobal + "tmp_dir.txt")
    p.createFile(false)
    val tmp = p
    try {
      tmp.createFile(false,true)
    } catch {
      case e: IOException => assert(true)
      case _: Throwable => assert(false)
    }
    flagGlobal = true
  }

  it should "26. check combinations of access modes" in {

    val tmp = Path.createTempFile(Path(FileSystems.getDefault.getPath(targetGlobal)), "test", ".tmp")
    val actualPerms = tmp.posixFilePerm()
    var trueModes = Set[AccessMode]()
    val allModes = Seq(AccessMode.EXECUTE, AccessMode.READ, AccessMode.WRITE)
    for (mode <- allModes) {
      println(mode + " is set? = " + tmp.checkAccess(mode))
      if (tmp.checkAccess(mode))
        trueModes += mode

    }
    for (mode1 <- allModes)
      for (mode2 <- allModes)
        assert(tmp.checkAccess(mode1, mode2) == (trueModes.contains(mode1) && trueModes.contains(mode2)))
    flagGlobal = true

  }

}
