package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.TypeVisitor
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class DottyAndType(override val left: ScType, override val right: ScType) extends DottyAndOrType with api.AndType

object DottyAndType {
  def apply: Seq[ScType] => ScType = _.reduce(DottyAndType(_, _))
}
