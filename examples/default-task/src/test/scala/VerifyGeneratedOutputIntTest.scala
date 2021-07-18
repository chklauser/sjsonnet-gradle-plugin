import org.junit.jupiter.api.Test
import org.scalatest.Assertions

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path}

/**
 * This integration test verifies that the sjsonnet tasks
 * in this project have generated the correct output files.
 *
 * It requires that the tasks are defined as dependencies of the test task.
 */
class VerifyGeneratedOutputIntTest extends Assertions {
  private val basePath = Path.of("build/generated/resources")

  @Test
  def custom(): Unit = {
    assertTwoFileStructure("custom", 3, Seq("yes, please"))
  }

  @Test
  def main(): Unit = {
    assertTwoFileStructure("main", 5, Seq("Champagne Essence"))
  }

  @Test
  def mock(): Unit = {
    assertTwoFileStructure("mockData", 1, Seq("Maraschino Cherry"))
  }

  /** The files of the `main` source set should also be available as classpath resources. */
  @Test
  def mainOnClasspath(): Unit = {
    val ty = classOf[VerifyGeneratedOutputIntTest]
    assert(ty.getResource("/f5.json") != null)
    assert(ty.getResource("/subdirectory/f6.json") != null)
  }

  private def assertTwoFileStructure(sourceSetName: String, indexOffset: Int, contentChecks: Seq[String] = Seq(), extension: String = "json") = {
    val outDir = basePath.resolve(sourceSetName).resolve("jsonnet")
    val f1 = outDir.resolve(s"f${indexOffset}.$extension")
    val f2 = outDir.resolve(s"subdirectory/f${1 + indexOffset}.$extension")

    assert(Files.exists(f1), f1)
    assert(Files.exists(f2), f2)

    if (contentChecks.nonEmpty) {
      val content2 = Files.readString(f2, StandardCharsets.UTF_8)
      assert(contentChecks.forall(content2.contains(_)), contentChecks)
    }
  }
}
