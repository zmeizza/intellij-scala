package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.TypeVisitor
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class DottyOrType(override val left: ScType, override val right: ScType) extends DottyAndOrType {
  override def visitType[T](visitor: TypeVisitor[T]): T = visitor match {
    case v: DottyOrTypeVisitor[T] => v.visitOrType(this)
    case _ => visitor.notSupported(this)
  }
}

object DottyOrType {
  def apply: Seq[ScType] => ScType = _.reduce(DottyOrType(_, _))
}

trait DottyOrTypeVisitor[T] extends api.TypeVisitor[T] {
  def visitOrType(tp: DottyOrType): T = default(tp)
}
