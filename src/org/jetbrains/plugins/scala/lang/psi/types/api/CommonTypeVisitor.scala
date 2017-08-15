package org.jetbrains.plugins.scala.lang.psi.types.api

import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{DesignatorTypeVisitor, ProjectionTypeVisitor, ThisTypeVisitor}
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.MethodTypeVisitor

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */

/**
  * Visitor for types present both in Scala and Dotty
  */
trait CommonTypeVisitor[T] extends TypeVisitor[T]
  with StdTypeVisitor[T]
  with JavaArrayTypeVisitor[T]
  with MethodTypeVisitor[T]
  with UndefinedTypeVisitor[T]
  with TypeParameterTypeVisitor[T]
  with ProjectionTypeVisitor[T]
  with ThisTypeVisitor[T]
  with DesignatorTypeVisitor[T]
