package com.rgm.file

import java.nio.file.{Path =>JPath, _}
import org.scalatest.{FlatSpec, Suite, BeforeAndAfterEach}
import scala.collection.mutable.ListBuffer
import scala.util.{Try, Random}
import java.net.URI
import java.util


class PathSetSpec extends FlatSpec with FileSetupTeardown {

  behavior of "PathSet"

  val allMatcher = PathMatcher(""".*""".r)

  override def createFS(p: Path) : (Array[JPath],Array[JPath]) =
  {
    var fls = new Array[JPath](0)
    var dirs = new Array[JPath](0)
    (dirs,fls)
  }
  def buildTmpFileTree = {
    val src = Path(srcGlobal)
    val dir1 = src.createTempDir("dir1_")
    val dir2 = src.createTempDir("dir2_")
    val dir3 = dir1.createTempDir("dir3_")
    val dir4 = dir2.createTempDir("dir4_")

    dir1.createTempFile("file_1_",".tmp")
    dir1.createTempFile("file_2_",".tmp")
    dir3.createTempFile("file_3_",".tmp")
    dir4.createTempFile("file_4_", ".tmp")
    src.createTempFile("file_5_",".tmp")
  }
  it should "1. PathSet should find the current state of the file system" in {
    val pathSet = PathSet(Path(srcGlobal)) * allMatcher
    val foo = Path(srcGlobal).createTempFile("foo", ".tmp")
    Path(srcGlobal).createTempFile("bar", ".tmp")
    Path(srcGlobal).createTempFile("baz", ".scala")
    var numTmps = 0
    pathSet.foreach((p:Path) => numTmps+=1)
    assert(numTmps == 3)
    foo.delete()
    numTmps = 0
    pathSet.foreach(p => numTmps += 1)

    assert(numTmps == 3)
    flagGlobal = true
  }


  it should "2. test should search at exactly the given depth" in {
    //building testing tree
    val src = Path(srcGlobal)
    val dir1 = src.createTempDir("dir1_")
    val dir2 = src.createTempDir("dir2_")
    val dir3 = dir1.createTempDir("dir3_")
    val dir4 = dir2.createTempDir("dir4_")

    dir1.createTempFile("file_1_",".tmp")
    dir1.createTempFile("file_2_",".tmp")
    dir3.createTempFile("file_3_",".tmp")
    dir4.createTempFile("file_4_", ".tmp")
    src.createTempFile("file_5_",".tmp")

    //val pathSet = PathSet(Path(srcGlobal), matcher, 3)
    var numTmps = 0
    (PathSet(Path(srcGlobal)) * allMatcher).foreach((p:Path) => numTmps+=1)
    //assert(numTmps==2)
    numTmps = 0
    (PathSet(Path(srcGlobal)) * allMatcher * allMatcher).foreach((p:Path) => numTmps+=1)
    //assert(numTmps==4)
    assert(true)
    (PathSet(Path(srcGlobal)) * allMatcher).foreach((p:Path) => {numTmps+=1; println("Found: " + p)})
    assert(numTmps==2)
    numTmps = 0
    println("\n\n")//REMOVE
    (PathSet(Path(srcGlobal)) * allMatcher * allMatcher).foreach((p:Path) => {numTmps+=1; println("Found: " + p)})//REMOVE the println
    assert(numTmps==4)
    flagGlobal = true
  }

  it should "3. PathSet should apply its filter to the elements it finds" in {
    val matcher = PathMatcher(".*.tmp".r)
    val pathSet = PathSet(Path(srcGlobal)) * matcher
    Path(srcGlobal).createTempFile("foo", ".tmp")
    Path(srcGlobal).createTempFile("bar", ".tmp")
    Path(srcGlobal).createTempFile("baz", ".scala")
    var numTmps = 0
    pathSet.foreach((p:Path) => numTmps+=1)
    assert(numTmps == 2)
    flagGlobal = true
  }

  it should "4. test *" in {
    buildTmpFileTree
    val pathSet = PathSet(Path(srcGlobal))
    (pathSet * """.*""".r).foreach((p: Path) => println("matches = " + p))
    assert(false)
    flagGlobal = true
  }

  it should "5. Does not match root on calls to children" in {
    val pathSet = PathSet(Path(srcGlobal).createTempDir("file_1_")) * allMatcher
    var numFound = 0
    pathSet.foreach((p: Path) => numFound += 1)
    assert(numFound == 0)
    flagGlobal = true
  }
}