package org.jetbrains.plugins.scala
package lang
package psi
package api
package expr

/**
  * @author Alexander Podkhalyuzin
  *         Date: 06.03.2008
  */
trait ScFinallyBlock extends ScalaPsiElement {
  def expression: Option[ScExpression] = findChild(classOf[ScExpression])
}