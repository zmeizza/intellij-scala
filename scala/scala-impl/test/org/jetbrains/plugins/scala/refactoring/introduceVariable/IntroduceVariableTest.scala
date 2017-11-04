package org.jetbrains.plugins.scala.refactoring.introduceVariable

import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.scala.SlowTests
import org.jetbrains.plugins.scala.base.libraryLoaders.{JdkLoader, ScalaLibraryLoader}
import org.jetbrains.plugins.scala.debugger.{ScalaSdkOwner, ScalaVersion, Scala_2_10}
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.AllTests

/**
  * Nikolay.Tropin
  * 25-Sep-17
  */
@RunWith(classOf[AllTests])
@Category(Array(classOf[SlowTests]))
class IntroduceVariableTest extends AbstractIntroduceVariableTestBase("introduceVariable", "data")
  with ScalaSdkOwner {

  override implicit val version: ScalaVersion = Scala_2_10

  override def project: Project = myProject

  override implicit def module: Module = ModuleManager.getInstance(myProject).getModules()(0)

  override protected def librariesLoaders = Seq(JdkLoader(), ScalaLibraryLoader())
}

object IntroduceVariableTest {
  def suite = new IntroduceVariableTest
}
