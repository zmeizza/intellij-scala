package org.jetbrains.plugins.scala
package lang
package psi
package api
package statements
package params

import com.intellij.psi._
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import org.jetbrains.plugins.scala.caches.DropOn
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.psi.adapters.PsiParameterAdapter
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPrimaryConstructor
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScExpression, ScFunctionExpr, ScUnderScoreSectionUtil}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScMember}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.{ScImportableDeclarationsOwner, ScModifierListOwner, ScTypedDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.api.{FunctionType, ValueType}
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.psi.types.{ScParameterizedType, ScType, ScTypeExt, ScalaType}
import org.jetbrains.plugins.scala.macroAnnotations.Cached

import scala.annotation.tailrec


/**
 * @author Alexander Podkhalyuzin
 * Date: 22.02.2008
 */

trait ScParameter extends ScTypedDefinition with ScModifierListOwner
                  with PsiParameterAdapter with ScImportableDeclarationsOwner {
  override def getTypeElement: PsiTypeElement

  def isWildcard: Boolean = "_" == name

  override def isVarArgs: Boolean = isRepeatedParameter

  override def computeConstantValue: Object = null

  override def normalizeDeclaration(): Unit = {}

  override def hasInitializer: Boolean = false

  override def getInitializer: PsiExpression = null

  def typeElement: Option[ScTypeElement]

  def paramType: Option[ScParameterType] = findChild(classOf[ScParameterType])

  override def getTextOffset: Int = nameId.getTextRange.getStartOffset

  override def getIcon(flags: Int): Icon = Icons.PARAMETER

  def isRepeatedParameter: Boolean

  def isCallByNameParameter: Boolean

  def baseDefaultParam: Boolean

  def getActualDefaultExpression: Option[ScExpression]

  def getRealParameterType: TypeResult =
    `type`() match {
      case result if !isRepeatedParameter => result
      case f@Right(tp) =>
        elementScope.getCachedClass("scala.collection.Seq")
          .map(ScalaType.designator)
          .map(ScParameterizedType(_, Seq(tp)))
          .map((result: ValueType) => Right(result))
          .getOrElse(f)
      case f => f
    }

  override def getDeclarationScope: ScalaPsiElement = PsiTreeUtil.getContextOfType(this, classOf[ScParameterOwner], classOf[ScFunctionExpr])

  def deprecatedName: Option[String]

  def owner: PsiElement = {
    ScalaPsiUtil.getContextOfType(this, true, classOf[ScFunctionExpr],
      classOf[ScFunction], classOf[ScPrimaryConstructor])
  }

  def isImplicitParameter: Boolean = {
    val clause = PsiTreeUtil.getParentOfType(this, classOf[ScParameterClause])
    if (clause == null) return false
    clause.isImplicit
  }

  def index: Int = getParent.getParent match {
    case parameters: ScParameters => parameters.params.indexOf(this)
    case _ => getParent.asInstanceOf[ScParameterClause].parameters.indexOf(this)
  }

  def getType: PsiType = getRealParameterType.getOrNothing.toPsiType

  def isAnonymousParameter: Boolean = getContext match {
    case clause: ScParameterClause => clause.getContext.getContext match {
      case _: ScFunctionExpr => true
      case _ => false
    }
    case _ => false
  }

  def expectedParamType: Option[ScType] = getContext match {
    case clause: ScParameterClause => clause.getContext.getContext match {
      // For parameter of anonymous functions to infer parameter's type from an appropriate
      // an. fun's type
      case f: ScFunctionExpr =>
        var flag = false
        var result: Option[ScType] = None //strange logic to handle problems with detecting type
        for (tp <- f.expectedTypes(fromUnderscore = false) if !flag) {
          @tailrec
          def applyForFunction(tp: ScType, checkDeep: Boolean) {
            tp.removeAbstracts match {
              case FunctionType(ret, _) if checkDeep => applyForFunction(ret, checkDeep = false)
              case FunctionType(_, params) if params.length == f.parameters.length =>
                val i = clause.parameters.indexOf(this)
                if (result.isDefined) {
                  result = None
                  flag = true
                } else result = Some(params(i))
              case any if f.isSAMEnabled =>
                //infer type if it's a Single Abstract Method
                ScalaPsiUtil.toSAMType(any, f) match {
                  case Some(FunctionType(_, params)) =>
                    val i = clause.parameters.indexOf(this)
                    if (i < params.length) result = Some(params(i))
                  case _ =>
                }
              case _ =>
            }
          }
          applyForFunction(tp, ScUnderScoreSectionUtil.underscores(f).nonEmpty)
        }
        result
      case _ => None
    }
  }

  def getTypeNoResolve: PsiType = PsiType.VOID

  @Cached(DropOn.semanticChange(this), this)
  def isDefaultParam: Boolean = calcIsDefaultParam(this, Set.empty)


  @tailrec
  private def calcIsDefaultParam(param: ScParameter, visited: Set[ScParameter]): Boolean = {
    if (param.baseDefaultParam) return true
    if (visited.contains(param)) return false
    getSuperParameter match {
      case Some(superParam) =>
        calcIsDefaultParam(superParam, visited + param)
      case _ => false
    }
  }

  def getDefaultExpression: Option[ScExpression] = {
    val res = getActualDefaultExpression
    if (res.isEmpty) {
      getSuperParameter.flatMap(_.getDefaultExpression)
    } else res
  }

  def getDefaultExpressionInSource: Option[ScExpression] = {
    val res = getActualDefaultExpression
    if (res.isEmpty) {
      getSuperParameter.flatMap(_.getDefaultExpressionInSource)
    } else {
      getContainingFile match {
        case file: ScalaFile =>
          if (file.isCompiled) {
            val containingMember = PsiTreeUtil.getContextOfType(this, true, classOf[ScMember])
            if (containingMember == null) res
            else {
              def extractFromParameterOwner(owner: ScParameterOwner): Option[ScExpression] = {
                owner.parameters.find(_.name == name) match {
                  case Some(param) => param.getDefaultExpression
                  case _ => res
                }
              }
              containingMember match {
                case c: ScClass =>
                  c.getSourceMirrorClass match {
                    case c: ScClass => extractFromParameterOwner(c)
                    case _ => res
                  }
                case f: ScFunction =>
                  f.getNavigationElement match {
                    case f: ScFunction => extractFromParameterOwner(f)
                    case _ => res
                  }
                case _ => res
              }
            }
          } else res
        case _ => res
      }
    }
  }

  def getSuperParameter: Option[ScParameter] = {
    getParent match {
      case clause: ScParameterClause =>
        val i = clause.parameters.indexOf(this)
        clause.getParent match {
          case p: ScParameters =>
            val j = p.clauses.indexOf(clause)
            p.getParent match {
              case fun: ScFunction =>
                fun.superMethod match {
                  case Some(method: ScFunction) =>
                    val clauses: Seq[ScParameterClause] = method.paramClauses.clauses
                    if (j >= clauses.length) return None
                    val parameters: Seq[ScParameter] = clauses.apply(j).parameters
                    if (i >= parameters.length) return None
                    Some(parameters.apply(i))
                  case _ => None
                }
              case _ => None
            }
          case _ => None
        }
      case _ => None
    }
  }
}
