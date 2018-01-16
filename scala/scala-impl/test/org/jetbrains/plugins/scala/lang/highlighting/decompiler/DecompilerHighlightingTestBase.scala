package org.jetbrains.plugins.scala.lang.highlighting.decompiler

import com.intellij.psi.PsiDocumentManager
import org.jetbrains.plugins.scala.annotator.{AnnotatorHolderMock, Error, Message, ScalaAnnotator}
import org.jetbrains.plugins.scala.base.{AssertMatches, ScalaFixtureTestCase}
import org.jetbrains.plugins.scala.debugger.{ScalaVersion, Scala_2_11}
import org.jetbrains.plugins.scala.decompiler.DecompilerTestBase
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScSuperReference

/**
  * @author Roman.Shein
  * @since 31.05.2016.
  */
abstract class DecompilerHighlightingTestBase extends ScalaFixtureTestCase with DecompilerTestBase with AssertMatches {

  override implicit val version: ScalaVersion = Scala_2_11

  override protected val includeReflectLibrary: Boolean = true

  override def basePath = s"${super.basePath}/highlighting/"

  override def doTest(fileName: String) = {
    assertNothing(getMessages(fileName, decompile(getClassFilePath(fileName))))
  }

  def getMessages(fileName: String, scalaFileText: String): List[Message] = {
    myFixture.configureByText(fileName.substring(0, fileName.lastIndexOf('.')) + ".scala", scalaFileText.replace("{ /* compiled code */ }", "???"))
    PsiDocumentManager.getInstance(getProject).commitAllDocuments()

    val mock = new AnnotatorHolderMock(getFile)
    val annotator = ScalaAnnotator.forProject

    getFile.depthFirst().foreach(annotator.annotate(_, mock))
    val supers = getFile.depthFirst().collect{case psi: ScSuperReference => psi.staticSuperPsi}.collect{case Some(x) => x}.toList.groupBy(_.getText)
    var errorMessages = mock.annotations.filter {
      case Error(_, null) | Error(null, _) => false
      case Error(_, a) => true
      case _ => false
    }
    for ((refText, values) <- supers) {
      val (currentMessages, otherMessages) = errorMessages.partition{
        case Error(element, s) if s == s"Cannot resolve symbol $element" && refText == element => true
        case _ => false
      }
      errorMessages = otherMessages ++ currentMessages.drop(values.size * 2) //TODO annotator provides errors twice
    }
    errorMessages
  }
}
