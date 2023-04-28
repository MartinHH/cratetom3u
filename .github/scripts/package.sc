//> using scala "3.2.2"
//> using lib "com.lihaoyi::os-lib:0.9.1"
//> using file "../../src/main/scala/io/github/martinhh/sl/ProjectDefs.scala"
import scala.util.Properties

import io.github.martinhh.sl.ProjectDefs.*

val platformString: String = {
  if (Properties.isWin) "windows"
  else if (Properties.isLinux) "linux"
  else if (Properties.isMac) "mac"
  else "unknown"
}
val targetDirPath = os.Path("artifacts", os.pwd) / platformString
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
).call(cwd = os.pwd)
  .out
  .text()
  .trim
