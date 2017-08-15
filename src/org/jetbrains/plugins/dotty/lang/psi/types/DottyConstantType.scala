package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.TypeVisitor

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
trait DottyConstantType extends DottyType {
  override def visitType[T](visitor: TypeVisitor[T]): T = visitor match {
    case v: DottyConstantTypeVisitor[T] => v.visitConstantType(this)
    case _ => visitor.notSupported(this)
  }
}

trait DottyConstantTypeVisitor[T] extends TypeVisitor[T] {
  def visitConstantType(tp: DottyConstantType): T = default(tp)
}