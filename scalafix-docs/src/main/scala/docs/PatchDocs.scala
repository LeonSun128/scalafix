package scalafix.docs

import scala.meta.inputs.Input
import scala.meta.interactive._
import scala.meta.internal.symtab.GlobalSymbolTable
import scalafix.internal.v1.InternalSemanticDoc
import scalafix.internal.reflect.ClasspathOps
import scalafix.patch.Patch
import scalafix.v1._

object PatchDocs {
  implicit class XtensionPatch(p: Patch) {
    def output(implicit doc: SemanticDocument): String = {
      val (obtained, _) =
        Patch.semantic(Map(RuleName("patch") -> p), doc, suppress = false)
      obtained
    }
    def showDiff(context: Int = 0)(implicit doc: SemanticDocument): Unit = {
      printDiff(unifiedDiff(p.output, context))
    }
  }
  implicit class XtensionPatchs(p: Iterable[Patch]) {
    def showLints()(implicit doc: SemanticDocument): Unit = {
      val (_, diagnostics) = Patch.semantic(
        Map(RuleName("RuleName") -> p.asPatch),
        doc,
        suppress = false)
      diagnostics.foreach { diag =>
        println(diag.formattedMessage)
      }
    }
    def showDiff(context: Int = 0)(implicit doc: SemanticDocument): Unit = {
      p.asPatch.showDiff(context)
    }
    def showOutput()(implicit doc: SemanticDocument): Unit = {
      println(p.asPatch.output)
    }
  }
  def printDiff(diff: String): Unit = {
    val trimmed = diff.lines.drop(3).mkString("\n")
    val message =
      if (trimmed.isEmpty) "<no diff>"
      else trimmed
    println(message)
  }

  def unifiedDiff(obtained: String, context: Int)(
      implicit doc: SemanticDocument): String = {
    val in = Input.VirtualFile("before patch", doc.input.text)
    val out = Input.VirtualFile("after  patch", obtained)
    Patch.unifiedDiff(in, out, context)
  }
  lazy val compiler = InteractiveSemanticdb.newCompiler(List("-Ywarn-unused"))
  lazy val symtab = GlobalSymbolTable(ClasspathOps.thisClasspath)
  def fromString(code: String): SemanticDocument = {
    val filename = "Main.scala"
    println("```scala")
    println("// " + filename)
    println(code.trim)
    println("```")
    val textDocument = InteractiveSemanticdb.toTextDocument(compiler, code)
    val input = Input.VirtualFile(filename, code)
    val doc = SyntacticDocument.fromInput(input)
    val internal = new InternalSemanticDoc(
      doc,
      textDocument,
      symtab
    )
    new SemanticDocument(internal)
  }
}
