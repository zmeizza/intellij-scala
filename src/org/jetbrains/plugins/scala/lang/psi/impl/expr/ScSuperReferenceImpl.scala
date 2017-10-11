package org.jetbrains.plugins.scala
package lang
package psi
package impl
package expr

import com.intellij.lang.ASTNode
import com.intellij.psi._
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes
import org.jetbrains.plugins.scala.lang.psi.api.base.ScStableCodeReferenceElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScObject, ScTemplateDefinition, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.api.{ScalaElementVisitor, ScalaFile}
import org.jetbrains.plugins.scala.lang.psi.types.result.{Failure, Typeable}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult

import _root_.scala.collection.mutable.ArrayBuffer

/**
* @author Alexander Podkhalyuzin
* Date: 14.03.2008
*/

class ScSuperReferenceImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScSuperReference {
  override def toString = "SuperReference"

  def isHardCoded: Boolean = {
    val id = findChildByType[PsiElement](ScalaTokenTypes.tIDENTIFIER)
    if (id == null) false else {
      ScalaPsiUtil.fileContext(id) match {
        case file: ScalaFile if file.isCompiled =>
          val next = id.getNode.getTreeNext
          if (next == null) false
          else next.getPsi match {
            case comment: PsiComment =>
              val commentText = comment.getText
              val path = commentText.substring(2, commentText.length - 2)
              val classes = ScalaPsiManager.instance(getProject).getCachedClasses(getResolveScope, path)
              if (classes.length == 1) {
                drvTemplate.exists(td => !ScalaPsiUtil.isInheritorDeep(td, classes(0)))
              } else {
                val clazz: Option[PsiClass] = classes.find(!_.isInstanceOf[ScObject])
                clazz match {
                  case Some(psiClass) =>
                    drvTemplate.exists(td => !ScalaPsiUtil.isInheritorDeep(td, psiClass))
                  case _ => false
                }
              }
            case _ => false
          }
        case _ => false
      }
    }
  }



  def drvTemplate: Option[ScTemplateDefinition] = reference match {
    case Some(q) => q.bind() match {
      case Some(ScalaResolveResult(td : ScTypeDefinition, _)) => Some(td)
      case _ => None
    }
    case None => ScalaPsiUtil.drvTemplate(this)
  }

  def staticSuperPsi: Option[ScStableCodeReferenceElement] = {
    Option(findLastChildByType[ScStableCodeReferenceElement](ScalaElementTypes.REFERENCE)).filter(ScalaPsiUtil.isInSqBrackets)
  }

  def staticSuper: Option[ScType] = {
    staticSuperPsi.flatMap(_.resolve() match {
      case t: Typeable =>
        t.getType().toOption
      case _ =>
        None
    })
  }

  def staticSuperName: String = Option(findChildByType[PsiElement](ScalaTokenTypes.tIDENTIFIER)).map(_.getText).getOrElse("")

  override def getReference = null

  def findSuper(id : PsiElement) : Option[ScType] = superTypes match {
    case None => None
    case Some(types) =>
      val name = id.getText
      for (t <- types) {
        t.extractClass match {
          case Some(c) if name == c.name => return Some(t)
          case _ =>
        }
      }
      None
  }

  private def superTypes: Option[Seq[ScType]] = reference match {
    case Some(q) => q.resolve() match {
      case clazz: PsiClass => Some(clazz.getSuperTypes.map(_.toScType()))
      case _ => None
    }
    case None =>
      PsiTreeUtil.getContextOfType(this, false, classOf[ScExtendsBlock]) match {
        case null => None
        case eb: ScExtendsBlock => Some(eb.superTypes)
      }
  }

  protected override def innerType = Failure("Cannot infer type of `super' expression", Some(this))

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitSuperReference(this)
  }

  override def accept(visitor: PsiElementVisitor) {
    visitor match {
      case visitor: ScalaElementVisitor => visitor.visitSuperReference(this)
      case _ => super.accept(visitor)
    }
  }
}