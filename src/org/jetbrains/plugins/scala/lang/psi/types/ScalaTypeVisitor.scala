package org.jetbrains.plugins.scala.lang.psi
package types

import org.jetbrains.plugins.scala.lang.psi.types.nonvalue._

/**
  * User: Alefas
  * Date: 28.09.11
  */

trait GenScalaTypeVisitor[T] extends api.CommonTypeVisitor[T]
  with CompoundTypeVisitor[T]
  with ExistentialTypeVisitor[T]
  with ExistentialArgumentVisitor[T]
  with AbstractTypeVisitor[T]
  with TypePolymorphicTypeVisitor[T]
  with ParameterizedTypeVisitor[T]


trait ScalaTypeVisitor extends GenScalaTypeVisitor[Unit] {
  override def default(tp: ScType): Unit = {}
}