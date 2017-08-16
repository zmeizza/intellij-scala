package org.jetbrains.plugins.scala.lang.psi.types.api

import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScDesignatorType, ScProjectionType, ScThisType}
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue.{ScMethodType, ScTypePolymorphicType}

/**
  * @author adkozlov
  * @author Nikolay.Tropin
  */
trait TypeVisitor[T] {

  def default(tp: ScType): T

  def notSupported(tp: ScType): T = {
    val visitorClass = this.getClass.getSimpleName
    val tpClass = this.getClass.getSimpleName

    throw new IllegalArgumentException(s"Visitor of type $visitorClass doesn't support $tpClass")
  }

  //common types
  def visitDesignatorType(tp: ScDesignatorType): T = default(tp)

  def visitJavaArrayType(tp: JavaArrayType): T = default(tp)

  def visitUndefinedType(tp: UndefinedType): T = default(tp)

  def visitProjectionType(tp: ScProjectionType): T = default(tp)

  def visitTypeParameterType(tp: TypeParameterType): T = default(tp)

  def visitStdType(tp: StdType): T = default(tp)

  def visitMethodType(tp: ScMethodType): T = default(tp)

  def visitThisType(tp: ScThisType): T = default(tp)

  //scala-only types
  def visitAbstractType(tp: ScAbstractType): T = notSupported(tp)

  def visitCompoundType(tp: ScCompoundType): T = notSupported(tp)

  def visitExistentialArgument(tp: ScExistentialArgument): T = notSupported(tp)

  def visitExistentialType(tp: ScExistentialType): T = notSupported(tp)

  def visitParameterizedType(tp: ScParameterizedType): T = notSupported(tp)

  def visitTypePolymorphicType(tp: ScTypePolymorphicType): T = notSupported(tp)

  //dotty types
  def visitOrType(tp: OrType): T = notSupported(tp)

  def visitAndType(tp: AndType): T = notSupported(tp)

  def visitConstantType(tp: ConstantType): T = notSupported(tp)

  def visitNoType(tp: NoType): T = notSupported(tp)

  def visitRefinedType(tp: RefinedType): T = notSupported(tp)
}