package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.ValueType
import org.jetbrains.plugins.scala.project.ProjectContext

/**
  * @author adkozlov
  */
trait DottyAndOrType extends DottyType with ValueType {
  implicit override def projectContext: ProjectContext = left.projectContext

  val left: ScType
  val right: ScType
}