package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class DottyOrType(override val left: ScType, override val right: ScType) extends DottyAndOrType with api.OrType

object DottyOrType {
  def apply: Seq[ScType] => ScType = _.reduce(DottyOrType(_, _))
}