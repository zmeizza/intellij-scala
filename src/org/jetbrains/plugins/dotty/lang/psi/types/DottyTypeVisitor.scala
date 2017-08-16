package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.{AndType, ConstantType, NoType, OrType, RefinedType}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * @author adkozlov
  */
trait GenDottyTypeVisitor[T] extends api.TypeVisitor[T] {
  override def visitOrType(tp: OrType): T = default(tp)

  override def visitAndType(tp: AndType): T = default(tp)

  override def visitConstantType(tp: ConstantType): T = default(tp)

  override def visitNoType(tp: NoType): T = default(tp)

  override def visitRefinedType(tp: RefinedType): T = default(tp)
}

trait DottyTypeVisitor extends GenDottyTypeVisitor[Unit] {
  override def default(tp: ScType): Unit = {}
}