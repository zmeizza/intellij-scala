package org.jetbrains.plugins.scala.lang.psi.api

import org.jetbrains.plugins.scala.caches.DropOn
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiElement
import org.jetbrains.plugins.scala.lang.psi.controlFlow.Instruction
import org.jetbrains.plugins.scala.lang.psi.controlFlow.impl.ScalaControlFlowBuilder
import org.jetbrains.plugins.scala.macroAnnotations.Cached

/**
 * Represents elements with control flow cached
 * @author ilyas
 */

trait ScControlFlowOwner extends ScalaPsiElement {

  @Cached(DropOn.anyPhysicalPsiChange(getProject), this)
  def getControlFlow: Seq[Instruction] = {
    val builder = new ScalaControlFlowBuilder(null, null)
    controlFlowScope match {
      case Some(elem) => builder.buildControlflow(elem)
      case None => Seq.empty
    }
  }

  def controlFlowScope: Option[ScalaPsiElement]
}
