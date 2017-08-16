package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api
import org.jetbrains.plugins.scala.lang.psi.types.api.ValueType
import org.jetbrains.plugins.scala.project.ProjectContext

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */

// is value type?
class DottyNoType(implicit val projectContext: ProjectContext) extends DottyType with ValueType with api.NoType {
  override def isFinalType = true

  override def equals(other: Any): Boolean = other.isInstanceOf[DottyNoType]

  override def hashCode(): Int = DottyNoType.hashCode()
}

object DottyNoType {
  def apply()(implicit projectContext: ProjectContext) = new DottyNoType()
  def unapply(t: DottyNoType): Boolean = true
}