package jestfs.util

import java.io.{Reader, File, PrintWriter}
import java.nio.file.{Files, StandardCopyOption}
import jestfs.*
import jestfs.error.*
import jestfs.util.BaseUtils.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.*
import scala.concurrent.duration.*
import scala.io.Source
import scala.sys.process.*
import io.circe.*, io.circe.syntax.*, io.circe.parser.*

/** file utilities */
object SystemUtils {
  // encoding
  private val ENC = "utf8"

  /** file reader */
  def fileReader(filename: String): Reader =
    Source.fromFile(filename, ENC).bufferedReader

  /** file trees with filename */
  def walkTree(filename: String, sort: Boolean = false): Iterable[File] =
    if (!sort)
      walkTree(File(filename))
    else
      walkTree(File(filename)).toSeq.sortWith(_.getPath < _.getPath).toIterable

  /** file trees with file */
  def walkTree(file: File): Iterable[File] =
    Seq(file) ++ new Iterable[File] {
      def iterator: Iterator[File] =
        if (file.isDirectory) file.listFiles.sorted.iterator
        else Iterator.empty
    }.flatMap(walkTree)

  /** extension filter */
  def extFilter(ext: String): (String | File) => Boolean = _ match {
    case name: String => name.endsWith(s".$ext")
    case file: File   => file.getName.endsWith(s".$ext")
  }
  lazy val algoFilter = extFilter("algo")
  lazy val irFilter = extFilter("ir")
  lazy val cfgFilter = extFilter("cfg")
  lazy val jsFilter = extFilter("js")

  /** print writer */
  def getPrintWriter(filename: String): PrintWriter =
    val file = File(filename)
    val parent = file.getParent
    if (parent != null) mkdir(parent)
    PrintWriter(file)

  /** dump given data to a file */
  def dumpFile(data: Any, filename: String): Unit =
    val nf = getPrintWriter(filename)
    nf.print(data)
    nf.close()

  /** dump given data into a file and show message */
  def dumpFile(name: Option[String], data: Any, filename: String): Unit =
    dumpFile(data, filename)
    name.map(name => println(s"- Dumped $name into $filename."))

  /** dump given data into a file and show message */
  def dumpFile(name: String, data: Any, filename: String): Unit =
    dumpFile(Some(name), data, filename)

  /** dump rows as tsv */
  def dumpRows(data: Seq[Vector[String]], filename: String): Unit =
    dumpFile(
      data.map(_.mkString("\t")).mkString(LINE_SEP),
      filename,
    )

  /** dump given data collection into a directory and show message */
  def dumpDir[T](
    name: Option[String],
    iterable: Iterable[T],
    dirname: String,
    getName: T => String,
    getData: T => Any,
    remove: Boolean,
  ): Unit =
    if (remove) rmdir(dirname)
    mkdir(dirname)
    for (x <- iterable) dumpFile(getData(x), s"$dirname/${getName(x)}")
    name.map(name => println(s"- Dumped $name into $dirname."))

  /** dump given data collection into a directory and show message */
  def dumpDir[T](
    name: String,
    iterable: Iterable[T],
    dirname: String,
    getName: T => String,
    getData: T => Any = (x: T) => x,
    remove: Boolean = false,
  ): Unit = dumpDir(Some(name), iterable, dirname, getName, getData, remove)

  /** dump given data in a JSON format */
  def dumpJson[T](
    data: T,
    filename: String,
    space: Boolean = true,
  )(using encoder: Encoder[T]): Unit =
    val json = data.asJson
    dumpFile(if (!space) json.noSpaces else json.spaces2, filename)

  /** dump given data in a JSON format and show message */
  def dumpJson[T](
    name: Option[String],
    data: T,
    filename: String,
    space: Boolean,
  )(using encoder: Encoder[T]): Unit =
    dumpJson(data, filename, space)
    name.map(name =>
      println(s"- Dumped $name into $filename in a JSON format."),
    )

  /** dump given data in a JSON format and show message */
  def dumpJson[T](
    name: String,
    data: T,
    filename: String,
    space: Boolean,
  )(using encoder: Encoder[T]): Unit =
    dumpJson(Some(name), data, filename, space)

  /** get first filename */
  def getFirstFilename(cmdConfig: CommandConfig, msg: String): String =
    cmdConfig.targets.headOption.getOrElse(throw NoFileError(msg))

  /** read file */
  def readFile(filename: String): String =
    val source = Source.fromFile(filename, ENC)
    val str = source.mkString
    source.close
    str

  /** read JSON */
  def readJson[T](filename: String)(implicit decoder: Decoder[T]): T =
    parse(readFile(filename)) match {
      case Left(err) => throw err
      case Right(json) =>
        json.as[T] match {
          case Left(err) => throw err
          case Right(v)  => v
        }
    }

  /** read HTML */
  def readHtml(filename: String): org.jsoup.nodes.Document =
    import HtmlUtils.*
    readFile(filename).toHtml

  /** delete files */
  def deleteFile(filename: String): Unit = File(filename).delete

  /** change extension */
  def changeExt(from: String, to: String): String => String =
    filename => filename.substring(0, filename.length - from.length) + to

  /** get name without extension */
  def removedExt(filename: String): String =
    filename.split('.').dropRight(1).mkString(".")

  /** get extension */
  def getExt(filename: String): String =
    filename.split('.').last

  /** get absolute path */
  def getAbsPath(filename: String): String =
    File(filename).getAbsolutePath

  /** renamed filename */
  def renameFile(from: String, to: String): Unit =
    File(from).renameTo(File(to))

  /** copy file */
  def copyFile(from: String, to: String): Unit = Files.copy(
    File(from).toPath,
    File(to).toPath,
    StandardCopyOption.REPLACE_EXISTING,
  )

  /** create symbolic link */
  def createSymlink(
    link: String,
    target: String,
    overwrite: Boolean = false,
  ): Unit = {
    if (overwrite)
      deleteFile(link)
    Files.createSymbolicLink(
      File(link).toPath,
      File(target).toPath,
    )
  }

  /** create directories */
  def mkdir(name: String, remove: Boolean = false): Unit =
    if (remove) rmdir(name)
    File(name).mkdirs

  /** clean directories */
  def cleanDir(name: String) = for (file <- walkTree(name)) file.delete

  /** remove directories */
  def rmdir(name: String): Unit = {
    def deleteRecursively(f: File): Boolean =
      if (f.isDirectory) f.listFiles match {
        case files: Array[File] => files.foreach(deleteRecursively)
        case null               =>
      }
      f.delete()
    deleteRecursively(File(name))
  }

  /** list directory */
  def listFiles(name: String): List[File] = listFiles(File(name))
  def listFiles(dir: File): List[File] =
    Option(dir.listFiles)
      .map(_.toList)
      .getOrElse(List())
      .filter(!_.getName.startsWith("."))

  /** file existence check */
  def exists(name: String): Boolean = File(name).exists

  /** check whether a shell command is normally terminated */
  def isNormalExit(str: String): Boolean = optional(executeCmd(str)).isDefined

  /** execute shell command with given dir, default to CUR_DIR */
  def executeCmd(cmdStr: String, dir: String = CUR_DIR): String =
    var cmd = s"$cmdStr 2> /dev/null"
    var directory = File(dir)
    var process = Process(Seq("sh", "-c", cmd), directory)
    process.!!

  /** concurrently execute a list of functions */
  def concurrent[T](
    fs: Iterable[() => T],
    duration: Duration = Duration.Inf,
  ): Iterable[T] = Await
    .result(
      Future.sequence(fs.map(f => Future(Try(f())))),
      duration,
    )
    .map(_.get)
}
