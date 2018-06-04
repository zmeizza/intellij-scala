package org.jetbrains.plugins.scala
package lang
package psi
package impl
package base
package types

import com.intellij.lang.ASTNode
import com.intellij.psi._
import com.intellij.psi.scope.PsiScopeProcessor
import org.jetbrains.plugins.scala.caches.DropOn
import org.jetbrains.plugins.scala.lang.psi.api.ScalaElementVisitor
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypeParametersOwner
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createTypeElementFromText
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.ScSyntheticClass
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api.Any
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.resolve.ScalaResolveResult
import org.jetbrains.plugins.scala.macroAnnotations.Cached

import scala.annotation.tailrec

/**
 * @author Alexander Podkhalyuzin, ilyas
 */

class ScParameterizedTypeElementImpl(node: ASTNode) extends ScalaPsiElementImpl(node) with ScParameterizedTypeElement {
  override def desugarizedText: String = {
    val inlineSyntaxIds = Set("?", "+?", "-?")

    def kindProjectorFunctionSyntax(elem: ScTypeElement): String = {
      def convertParameterized(param: ScParameterizedTypeElement): String = {
        param.typeElement.getText match {
          case v@("+" | "-") => //λ[(-[A], +[B]) => Function2[A, Int, B]]
            param.typeArgList.typeArgs match {
              case Seq(simple) => v ++ simple.getText
              case _ => "" //should have only one type arg
            }
          case _ => param.getText //it's a higher kind type
        }
      }

      def convertSimpleType(simple: ScSimpleTypeElement) = simple.getText.replaceAll("`", "")

      elem match {
        case fun: ScFunctionalTypeElement =>
          fun.returnTypeElement match {
            case Some(ret) =>
              val typeName = "Λ$"
              val paramText = fun.paramTypeElement match {
                case tuple: ScTupleTypeElement =>
                  val paramList = tuple.components.map {
                    case parameterized: ScParameterizedTypeElement => convertParameterized(parameterized)
                    case simple: ScSimpleTypeElement => convertSimpleType(simple)
                    case _ => return null //something went terribly wrong
                  }
                  paramList.mkString(sep = ", ")
                case simple: ScSimpleTypeElement => simple.getText.replaceAll("`", "")
                case parameterized: ScParameterizedTypeElement => convertParameterized(parameterized)
                case _ => return null
              }
              s"({type $typeName[$paramText] = ${ret.getText}})#$typeName"
            case _ => null
          }
        case _ => null
      }
    }

    def kindProjectorInlineSyntax(e: PsiElement) = {
      def generateName(i: Int): String = {
        //kind projector generates names the same way
        val res = ('α' + (i % 25)).toChar.toString
        if (i < 25) res
        else res + (i / 25)
      }

      val (paramOpt: Seq[Option[String]], body: Seq[String]) = typeArgList.typeArgs.zipWithIndex.map {
        case (simple: ScSimpleTypeElement, i) if inlineSyntaxIds.contains(simple.getText) =>
          val name = generateName(i)
          (Some(simple.getText.replace("?", name)), name)
        case (param: ScParameterizedTypeElement, i) if inlineSyntaxIds.contains(param.typeElement.getText) =>
          val name = generateName(i)
          (Some(param.getText.replace("?", name)), name)
        case (a, _) => (None, a.getText)
      }.unzip
      val paramText = paramOpt.flatten.mkString(start = "[", sep = ", ", end = "]")
      val bodyText = body.mkString(start = "[", sep = ", ", end = "]")

      s"({type ${"Λ$"}$paramText = ${typeElement.getText}$bodyText})#${"Λ$"}"
    }

    def existentialType = {
      val forSomeBuilder = new StringBuilder
      var count = 1
      forSomeBuilder.append(" forSome {")
      val typeElements = typeArgList.typeArgs.map {
        case w: ScWildcardTypeElement =>
          forSomeBuilder.append("type _" + "$" + count +
            w.lowerTypeElement.fold("")(te => s" >: ${te.getText}") +
            w.upperTypeElement.fold("")(te => s" <: ${te.getText}"))
          forSomeBuilder.append("; ")
          val res = s"_$$$count"
          count += 1
          res
        case t => t.getText
      }
      forSomeBuilder.delete(forSomeBuilder.length - 2, forSomeBuilder.length)
      forSomeBuilder.append("}")
      s"(${typeElement.getText}${typeElements.mkString("[", ", ", "]")} ${forSomeBuilder.toString()})"
    }

    val kindProjectorEnabled = this.kindProjectorPluginEnabled
    def isKindProjectorFunctionSyntax(element: PsiElement): Boolean = {
      typeElement.getText match {
        case "Lambda" | "λ" if kindProjectorEnabled => true
        case _ => false
      }
    }

    @tailrec
    def isKindProjectorInlineSyntax(element: PsiElement): Boolean = {
      element match {
        case simple: ScSimpleTypeElement if kindProjectorEnabled && inlineSyntaxIds.contains(simple.getText) => true
        case parametrized: ScParameterizedTypeElement if kindProjectorEnabled =>
          isKindProjectorInlineSyntax(parametrized.typeElement)
        case _ => false
      }
    }

    typeArgList.typeArgs.find {
      case e: ScFunctionalTypeElement if isKindProjectorFunctionSyntax(e) => true
      case e if isKindProjectorInlineSyntax(e) => true
      case _: ScWildcardTypeElementImpl => true
      case _ => false
    } match {
      case Some(fun) if isKindProjectorFunctionSyntax(fun) => kindProjectorFunctionSyntax(fun)
      case Some(e) if isKindProjectorInlineSyntax(e) => kindProjectorInlineSyntax(e)
      case Some(_) => existentialType
      case _ => null
    }
  }

  //computes desugarized type either for existential type or one of kind projector types
  @Cached(DropOn.semanticChange(this), this)
  override def computeDesugarizedType: Option[ScTypeElement] = Option(desugarizedText) match {
    case Some(text) => Option(createTypeElementFromText(text, getContext, this))
    case _ => None
  }

  override protected def innerType: TypeResult = {
    computeDesugarizedType match {
      case Some(typeElement) =>
        return typeElement.`type`()
      case _ =>
    }
    val tr = typeElement.`type`()
    val res = tr.getOrElse(return tr)

    //todo: possible refactoring to remove parameterized type inference in simple type
    typeElement match {
      case s: ScSimpleTypeElement =>
        s.reference match {
          case Some(ref) =>
            if (ref.isConstructorReference) {
              ref.resolveNoConstructor match {
                case Array(ScalaResolveResult(to: ScTypeParametersOwner, _: ScSubstitutor))
                  if to.isInstanceOf[PsiNamedElement] =>
                  return tr //all things were done in ScSimpleTypeElementImpl.innerType
                case Array(ScalaResolveResult(to: PsiTypeParameterListOwner, _: ScSubstitutor))
                  if to.isInstanceOf[PsiNamedElement] =>
                  return tr //all things were done in ScSimpleTypeElementImpl.innerType
                case _ =>
              }
            }
            ref.bind() match {
              case Some(ScalaResolveResult(_: PsiMethod, _)) =>
                return tr //all things were done in ScSimpleTypeElementImpl.innerType
              case _ =>
            }
          case _ =>
        }
      case _ =>
    }

    typeArgList.typeArgs match {
      case Seq() => tr
      case args =>
        val result = ScParameterizedType(res, args.map(_.`type`().getOrAny))
        Right(result)
    }
  }

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitParameterizedTypeElement(this)
  }

  override def accept(visitor: PsiElementVisitor) {
    visitor match {
      case s: ScalaElementVisitor => s.visitParameterizedTypeElement(this)
      case _ => super.accept(visitor)
    }
  }

  override def processDeclarations(processor: PsiScopeProcessor,
                                   state: ResolveState,
                                   lastParent: PsiElement,
                                   place: PsiElement): Boolean = {
    if (this.kindProjectorPluginEnabled) {
      computeDesugarizedType match {
        case Some(projection: ScTypeProjection) =>
          projection.typeElement match {
            case paren: ScParenthesisedTypeElement => paren.innerElement match {
              case Some(compound: ScCompoundTypeElement) =>
                compound.refinement match {
                  case Some(ref) => ref.types match {
                    case Seq(alias: ScTypeAliasDefinition) =>
                      for (tp <- alias.typeParameters) {
                        val text = tp.getText
                        val lowerBound = text.indexOf(">:")
                        val upperBound = text.indexOf("<:")
                        //we have to call processor execute so both `+A` and A resolve: Lambda[`+A` => (A, A)]
                        processor.execute(tp, state)
                        processor.execute(new ScSyntheticClass(s"`$text`", Any), state)
                        if (lowerBound < 0 && upperBound > 0) {
                          processor.execute(new ScSyntheticClass(text.substring(0, upperBound), Any), state)
                        } else if (upperBound < 0 && lowerBound > 0) {
                          processor.execute(new ScSyntheticClass(text.substring(0, lowerBound), Any), state)
                        } else if (upperBound > 0 && lowerBound > 0) {
                          val actualText = text.substring(0, math.min(lowerBound, upperBound))
                          processor.execute(new ScSyntheticClass(actualText, Any), state)
                        }
                      }
                    case _ =>
                  }
                  case _ =>
                }
              case _ =>
            }
            case _ =>
          }
          processor.execute(new ScSyntheticClass("+", Any), state)
          processor.execute(new ScSyntheticClass("-", Any), state)
        case _ =>
      }
    }
    super.processDeclarations(processor, state, lastParent, place)
  }
}
