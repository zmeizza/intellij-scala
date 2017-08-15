package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.{ScType, api}

/**
  * @author adkozlov
  */
trait GenDottyTypeVisitor[T] extends api.CommonTypeVisitor[T]
  with DottyOrTypeVisitor[T]
  with DottyAndTypeVisitor[T]
  with DottyConstantTypeVisitor[T]
  with DottyNoTypeVisitor[T]
  with DottyRefinedTypeVisitor[T]

trait DottyTypeVisitor extends GenDottyTypeVisitor[Unit] {
  override def default(tp: ScType): Unit = {}
}