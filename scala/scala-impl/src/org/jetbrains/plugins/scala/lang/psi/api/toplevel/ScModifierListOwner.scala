package org.jetbrains.plugins.scala
package lang
package psi
package api
package toplevel

import com.intellij.psi._
import org.jetbrains.plugins.scala.caches.DropOn
import org.jetbrains.plugins.scala.extensions.StubBasedExt
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.psi.adapters.PsiModifierListOwnerAdapter
import org.jetbrains.plugins.scala.lang.psi.api.base._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScAnnotationsHolder
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory
import org.jetbrains.plugins.scala.macroAnnotations.Cached

/**
* @author ilyas
*/

trait ScModifierListOwner extends ScalaPsiElement with ScAnnotationsHolder with PsiModifierListOwnerAdapter {

  @Cached(DropOn.anyScalaPsiChange, this)
  override def getModifierList: ScModifierList = {
    val child = this.stubOrPsiChild(ScalaElementTypes.MODIFIERS)
    child.getOrElse(ScalaPsiElementFactory.createEmptyModifierList(this))
  }

  override def hasAnnotation(fqn: String): Boolean = super[ScAnnotationsHolder].hasAnnotation(fqn)

  def hasModifierProperty(name: String): Boolean = hasModifierPropertyInner(name)

  // TODO This method is, in fact, ...Java, as it interprets the absence of 'private' / 'protected' as the presence of 'public'
  // TODO We need to implement this in the hasModifierProperty itself.
  // TODO Also, we should probably do that at the level of ScModifierList.
  def hasModifierPropertyScala(name: String): Boolean = {
    if (name == PsiModifier.PUBLIC)
      !hasModifierPropertyScala("private") && !hasModifierPropertyScala("protected")
    else
      hasModifierPropertyInner(name)
  }

  // TODO Should probably be unified as "isAbstract" (which doesn't always require an 'abstract' modifier'.
  def hasAbstractModifier: Boolean = hasModifierPropertyInner("abstract")

  // TODO isFinal
  def hasFinalModifier: Boolean = hasModifierPropertyInner("final")

  private def hasModifierPropertyInner(name: String): Boolean =
    Option(getModifierList).exists(_.hasModifierProperty(name))

  def setModifierProperty(name: String, value: Boolean) {
    Option(getModifierList).foreach(_.setModifierProperty(name, value))
  }
}