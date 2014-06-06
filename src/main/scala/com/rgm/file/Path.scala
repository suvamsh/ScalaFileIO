package com.rgm.file

import java.io.{File => JFile, IOException}
import java.net.{URI, URL}
import java.nio.file.{Path => JPath, _}
import scala.language.implicitConversions
import scala.collection.JavaConverters._
import java.nio.file.attribute._



object Path {
  def apply(jpath: JPath): Path = new Path(jpath)
  def apply(jfile: JFile): Path = new Path(jfile.toPath)

  // it might be better if these methods took an implicit FileSystem rather than relying on java.nio.file..Paths
  // (which assumes the default FileSystem)
  def apply(uri: URI): Path = new Path(Paths.get(uri))
  def apply(path: String): Path = new Path(Paths.get(path))

  implicit def fromJPath(jpath: JPath): Path = apply(jpath)
  implicit def fromJFile(jfile: JFile): Path = apply(jfile)
  implicit def fromString(path: String): Path = apply(path)

  implicit def toFinder(path: Path): PathFinder = ???

  /**
   * Enumeration of the Access modes possible for accessing files
   */
  /* CHECK ACCESS METHOD USES THIS, HOW? I DON'T KNOW YET :(*/

  /*object AccessModes {
    sealed trait AccessMode
    case object Execute extends AccessMode
    case object Read extends AccessMode
    case object Write extends AccessMode
    def values:Set[AccessMode] = Set(Execute, Read, Write)
  }*/
}

final class Path(val jpath: JPath) extends Equals with Ordered[Path] {

  if (jpath == null) throw new NullPointerException("cannot wrap a null path")

  override def toString: String = jpath.toString

  override def hashCode: Int = jpath.hashCode

  override def equals(other: Any): Boolean = other match {
    case that: Path => this.jpath equals that.jpath
    case _ => false
  }

  def canEqual(that: Any): Boolean = that.isInstanceOf[Path]

  def compare(that: Path): Int = jpath.compareTo(that.jpath)

  //--------------------------------------------------------------------------------------------------------------------

  def fileSystem: FileSystem = FileSystem(jpath.getFileSystem)

  def path: String = toString

  def name: String = simpleName + extension

  //last most segment without extension
  def simpleName: String = if(segments.last.toString.count(_ == '.') == 1 ) segments.last.toString.split('.')(0) else ""


  def extension: Option[String] = name.lastIndexWhere (_ == '.') match {
    case idx if idx != -1 => Some(name.drop(idx))
    case _ => None
  }

  def withExtension(extension: Option[String]): Path =
  {
    if(extension.isEmpty)
      path
    else if(segments.last.toString.charAt(0).equals('.'))
      segments.init.mkString("/").concat("/").concat(segments.last.toString.concat(extension.get.toString))
    else
      segments.init.mkString("/").concat("/").concat(segments.last.toString.split('.').init.mkString(".").concat(extension.get.toString))
  }

  // segments should probably include the root segment, if any (like scalax but unlike Java NIO)
  def segments: Seq[Path] = if (isAbsolute) path.concat("/").split("/").map(_.concat("/")).map(Path(_)) else path.split("/").map(_.concat("/")).map(Path(_))


  // just like segments
  def segmentIterator: Iterator[Path] =  segments.iterator

  // again, should count the root segment, if any
  def segmentCount: Int = segments.size

  def root: Option[Path] = Option(jpath.getRoot).map(Path(_))

  // there is a good argument for having this method always return an unwrapped (non-null) Path. For example:
  //   Path("a").parent == Path("") or Path(".")
  //   Path("").parent == Path("..")
  //   Path("..").parent == Path("../..")
  //   Path("/").parent == Path("/")
  // is this sensible, or does it present serious problems? it would certainly be convenient.
<<<<<<< HEAD
  def parent: Option[Path] = Option(Path(jpath).resolve("..").normalize)
  /*{
    if(path.equals(""))
      Option(Path(".."))
    else if(path.equalsIgnoreCase("/"))
      Option(Path("/"))
    else if (segmentIterator.forall(k => k.toString.equals("..")))
      Option(Path(segments.mkString("/").concat("/..")))
    else if ((path.count(_ == '/') == 0 || path.count(_ == '/') == 1) && segmentCount == 1)
      Option(Path("."))
    else
      Option(jpath.normalize.getParent).map(Path(_))
  }*/
=======
  // this breaks on ".." vs "../.." and "." and becomes inconsistent...

  def parent: Option[Path] = if (jpath.getParent == null) Option(null) else Option(Path(jpath.getParent))
>>>>>>> 655994bde07f2bde81d89bd05f9e5b949dcdb742

  def subpath(begin: Int, end: Int): Path = Path(jpath.subpath(begin, end))

  def startsWith(other: Path): Boolean = jpath.startsWith(other.jpath)

  def startsWith(other: String): Boolean = jpath.startsWith(fileSystem.path(other).jpath)

  def endsWith(other: Path): Boolean = jpath.endsWith(other)

  def endsWith(other: String): Boolean = jpath.endsWith(fileSystem.path(other))

  def isAbsolute: Boolean = jpath.isAbsolute

  //this can't be syntactic function, needs to search filesystem for absolute path
  def toAbsolute: Path = Path(jpath.toAbsolutePath)

  // should behave mostly like JPath.normalize but should correctly handle the empty path
  // (N.B. the scalax implementation is wrong too--the operation should be idempotent).
  def normalize: Path =
  {
    if(jpath.toString.equals("")) {
      return ""
    }
    else
      Path(jpath.normalize)
  }

  def toRealPath(options: LinkOption*): Path = Path(jpath.toRealPath(options : _*))

  def toURI: URI = jpath.toUri

  def toURL: URL = toURI.toURL

  def jfile: JFile = jpath.toFile

  // should behave like JPath.relativize (the scalax implementation is backwards and broken)
  def relativize(other: Path): Path = jpath.relativize(other.jpath)

  def relativize(other: String): Path = jpath.relativize(fileSystem.path(other))

  def relativeTo(base: Path): Path = base.relativize(this)

  def relativeTo(base: String): Path = jpath.relativize(fileSystem.path(base))

  // should behave like JPath.resolve except when `other` is an absolute path, in which case in should behave as if
  // `other` were actually a relative path (i.e. `other.relativeTo(other.root.get)`)
  // this is so that `path1 / path2` behaves exactly like `Path(path1.path + "/" + path2.path)`
  def resolve(other: Path): Path =
  {
    if(other.isAbsolute)
      Path(path + "/" + other.relativeTo(other.root.get))
    else
      Path(jpath.resolve(other.jpath))
  }

  // as above
  def resolve(other: String): Path = resolve(Path(other))

  def / (other: Path): Path = resolve(other)

  def / (other: String): Path = resolve(other)

  // should behave like JPath.resolveSibling except that `other` should always be treated as a relative path
  // (for consistency with resolve). the NIO implementation also seems dubious when `this` is the root or when `this`
  // has no parent and `other` is on a different file system
  def sibling(other: Path): Path = {
    val sibl : Path = if (other.isAbsolute) Path(other.path.substring(1)) else other
    if (this.equals(Path("/")))
      fileSystem.path("/.").jpath.resolveSibling(sibl.path)
    else
      jpath.resolveSibling(sibl.path)
  }

    def sibling(other: String): Path = sibling(Path(other))


  //--------------------------------------------------------------------------------------------------------------------

  def exists: Boolean = Files.exists(jpath)

  def exists(options: LinkOption*): Boolean = Files.exists(jpath.toRealPath(options :_*))

  def nonExistent: Boolean = Files.notExists(jpath) // not equivalent to `!exists`

  def nonExistent(options: LinkOption*): Boolean = Files.notExists(jpath.toRealPath(options : _*))

  def isSame(other: Path): Boolean = normalize == other.normalize

  def size: Option[Long] = Option(Files.size(jpath.toAbsolutePath))

  def isDirectory: Boolean = Files.isDirectory(jpath.toAbsolutePath)

  def isFile: Boolean = Files.isRegularFile(jpath.toAbsolutePath) // == isRegularFile

  def isSymLink: Boolean = Files.isSymbolicLink(jpath.toAbsolutePath)

  def isHidden: Boolean = Files.isHidden(jpath.toAbsolutePath)

  def isReadable: Boolean = Files.isReadable(jpath.toAbsolutePath)

  def isWritable: Boolean = Files.isWritable(jpath.toAbsolutePath)

  def isExecutable: Boolean = Files.isExecutable(jpath.toAbsolutePath)

  // etc.

  //createTempFile
  def createTempFile(prefix: String, suffix: String /*deleteOnExit: Boolean NOT SURE IF NEEDED*/) : Path = Files.createTempFile(jpath,prefix, suffix)


  //createTempDir
  def createTempDir(prefix: String /*FILE ATTRIBUTES???*/ ) : Path = Files.createTempDirectory(jpath, prefix)


  //checkAccess -> canWrite, canRead, canExecute
  /* TOO MANY ERRORS WILL FIX LATER*/
  def checkAccess(modes: AccessMode*): Boolean = {
    modes.toString forall {
      case 'x'  => jfile.canExecute()
      case 'r'     => jfile.canRead()
      case 'w'    => jfile.canWrite()
    }
  }

  def access_=(accessModes:Iterable[AccessMode]) = {
    //if (nonExistent) fail("Path %s does not exist".format(path))
    jfile.setReadable(accessModes exists {_=='r'})
    jfile.setWritable(accessModes exists {_=='w'})
    jfile.setExecutable(accessModes exists {_=='x'})
  }
  //lastModified
  def lastModified : FileTime = Files.getLastModifiedTime(jpath)

  //access_
  def access_(perms: Set[PosixFilePermission]) : Path = Files.setPosixFilePermissions(jpath, perms.asJava)

  //createFile
  def createFile : Path = Files.createFile(jpath)

  //createDirectory
  def createDirectory : Path = Files.createDirectory(jpath)

  //deleteIfExists
  def deleteIfExists : Boolean = Files.deleteIfExists(jpath)

  //delete
  def delete : Unit = Files.delete(jpath)

  //deleteRecursively
  def deleteRecursively : Boolean =
  {
    //first check if it's a dir or file
    if(exists && isDirectory)
    {
      Files.walkFileTree(jpath,
        new SimpleFileVisitor[JPath]
        {
          //@throws(classOf[IOException])
          override def postVisitDirectory(dir: JPath, e: IOException) : FileVisitResult =
          {
            if(e == None)
            {
              throw e
            }
            else
            {
              Files.delete(dir)
              FileVisitResult.CONTINUE
            }
          }

          override def visitFile(file: JPath,attrs: BasicFileAttributes ) : FileVisitResult = {Files.delete(file);FileVisitResult.CONTINUE }
        }
      )
      true
    }
    else if(exists)
    {
      try {
        delete
        true
      } catch {
        case e: IOException => false
      }
    }
    else
      false
  }

  //copyTo(source, target) /*BROKEN*/
  def copyTo(target: Path) : Path = Files.copy(jpath, target.jpath)

  //moveFile /*BROKEN*/
  def moveFile(target: Path) : Unit = Files.move(jpath, target.jpath)

  //moveDirectory  /*BROKEN*/
  def moveDirectory(target: Path) : Unit =
  {
    if(exists && isDirectory)
    {
      Files.walkFileTree(jpath,
        new SimpleFileVisitor[JPath]
        {
          //@throws(classOf[IOException])
          override def preVisitDirectory(dir: JPath, attrs: BasicFileAttributes) : FileVisitResult =
          {
            Files.createDirectories(target.jpath)
            FileVisitResult.CONTINUE
          }

          override def visitFile(file: JPath,attrs: BasicFileAttributes ) : FileVisitResult = {Files.copy(file, target.jpath); FileVisitResult.CONTINUE}
        })
    }
  }
}