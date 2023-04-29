//> using scala "3.2.2"
//> using lib "com.lihaoyi::os-lib:0.9.1"
//> using file "../../src/main/scala/io/github/martinhh/sl/ProjectDefs.scala"
import scala.util.Properties

import io.github.martinhh.sl.ProjectDefs.*

val targetDirPath = os.Path("artifacts", os.pwd) / BinaryName
val destPath = {
  val binName = if (Properties.isWin) s"$BinaryName.exe" else BinaryName
  targetDirPath / binName
}
val scalaCLILauncher =
  if (Properties.isWin) "scala-cli.bat" else "scala-cli"

os.makeDir.all(targetDirPath)
os.proc(
  scalaCLILauncher,
  "--power",
  "package",
  ".",
  "-o",
  destPath,
  "--version",
  Version,
  "--native-image"
).call(cwd = os.pwd, stdout = os.Inherit)
  .out
  .text()
  .trim

// test the generated executable with a test-.crate-file
os.proc(
  destPath,
  "-f",
  os.Path("src", os.pwd) / "test" / "resources" / "testcrates" / "2SongTestCrate.crate",
  targetDirPath / "testoutput.m3u"
).call(cwd = os.pwd, stdout = os.Inherit)
  .out
  .text()
  .trim
