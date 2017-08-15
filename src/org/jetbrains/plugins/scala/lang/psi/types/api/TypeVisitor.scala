package org.jetbrains.plugins.scala.lang.psi.types.api

import org.jetbrains.plugins.scala.lang.psi.types.ScType

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
}