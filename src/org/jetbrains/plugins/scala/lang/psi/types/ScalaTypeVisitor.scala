package org.jetbrains.plugins.scala.lang.psi
package types

import org.jetbrains.plugins.scala.lang.psi.types.nonvalue._

/**
  * User: Alefas
  * Date: 28.09.11
  */

trait GenScalaTypeVisitor[T] extends api.TypeVisitor[T] {
  override def visitAbstractType(tp: ScAbstractType): T = default(tp)

  override def visitCompoundType(tp: ScCompoundType): T = default(tp)

  override def visitExistentialArgument(tp: ScExistentialArgument): T = default(tp)

  override def visitExistentialType(tp: ScExistentialType): T = default(tp)

  override def visitParameterizedType(tp: ScParameterizedType): T = default(tp)

  override def visitTypePolymorphicType(tp: ScTypePolymorphicType): T = default(tp)
}


trait ScalaTypeVisitor extends GenScalaTypeVisitor[Unit] {
  override def default(tp: ScType): Unit = {}
}