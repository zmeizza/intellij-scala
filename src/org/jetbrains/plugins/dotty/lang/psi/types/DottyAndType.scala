package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.TypeVisitor
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class DottyAndType(override val left: ScType, override val right: ScType) extends DottyAndOrType {
  override def visitType[T](visitor: TypeVisitor[T]): T = visitor match {
    case v: DottyAndTypeVisitor[T] => v.visitAndType(this)
    case _ => visitor.notSupported(this)
  }
}

object DottyAndType {
  def apply: Seq[ScType] => ScType = _.reduce(DottyAndType(_, _))
}

trait DottyAndTypeVisitor[T] extends api.TypeVisitor[T] {
  def visitAndType(tp: DottyAndType): T = default(tp)
}

