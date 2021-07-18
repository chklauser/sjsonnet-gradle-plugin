package io.github.chklauser.sjsonnet.gradle

import org.slf4j.{Logger, LoggerFactory}

import java.io.File
import java.nio.file.{FileVisitOption, Files, Path}
import java.nio.file.attribute.FileAttribute
import java.util.Comparator

class TemporaryDirectory(val prefix: String) extends AutoCloseable {
  val asPath: Path = Files.createTempDirectory(prefix)
  val asFile: File = asPath.toFile
  override def close(): Unit = try {
    if(Files.notExists(asPath)){
      return
    }
    Files.walk(asPath)
        .sorted(Comparator.reverseOrder():Comparator[Path])
        .forEach(f => Files.deleteIfExists(f))
    Files.deleteIfExists(asPath)
  } catch {
    case e: Exception => TemporaryDirectory.log.warn("Failed to delete temporary directory {}", Array(asPath, e):_*)
  }
}

object TemporaryDirectory {
  private val log: Logger = LoggerFactory.getLogger(classOf[TemporaryDirectory])
}
