//> using scala "3.2.2"
//> using lib "com.lihaoyi::os-lib:0.9.1"
//> using file "../../src/main/scala/io/github/martinhh/sl/ProjectDefs.scala"
import scala.util.Properties

import io.github.martinhh.sl.ProjectDefs.*

val workDirPath = os.Path("target", os.pwd)
val binName = if (Properties.isWin) s"$BinaryName.exe" else BinaryName
val destPath = {
  workDirPath / binName
}
val scalaCLILauncher =
  if (Properties.isWin) "scala-cli.bat" else "scala-cli"

os.makeDir.all(workDirPath)
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
  "-c",
  "UTF-8",
  os.Path("src", os.pwd) / "test" / "resources" / "testcrates" / "2SongTestCrate.crate",
  workDirPath / "testoutput.m3u"
).call(cwd = os.pwd, stdout = os.Inherit)
  .out
  .text()
  .trim

val releaseDirPath = os.Path("executable", os.pwd) / BinaryName
os.makeDir.all(releaseDirPath)
os.move(destPath, releaseDirPath / binName)

// bundle license files with binary
os.copy.into(os.pwd / "LICENSE", releaseDirPath)
os.copy(os.pwd / "doc" / "BINARY_NOTICE.md", releaseDirPath / "NOTICE.md")
