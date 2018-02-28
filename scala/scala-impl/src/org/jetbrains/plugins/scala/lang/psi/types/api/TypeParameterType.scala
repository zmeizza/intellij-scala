package org.jetbrains.plugins.scala.lang.psi.types.api

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.psi.types.{NamedType, ScType, ScUndefinedSubstitutor}
import org.jetbrains.plugins.scala.project.ProjectContext

import scala.collection.Seq

sealed trait TypeParameterType extends ValueType with NamedType {
  val arguments: Seq[TypeParameterType]

  def lowerType: ScType

  def upperType: ScType

  def typeParameter: TypeParameter

  def psiTypeParameter: PsiTypeParameter = typeParameter.psiTypeParameter

  override implicit def projectContext: ProjectContext = psiTypeParameter

  override val name: String = typeParameter.name

  def isInvariant: Boolean = typeParameter.isInvariant

  def isCovariant: Boolean = typeParameter.isCovariant

  def isContravariant: Boolean = typeParameter.isContravariant

  override def equivInner(`type`: ScType, substitutor: ScUndefinedSubstitutor, falseUndef: Boolean): (Boolean, ScUndefinedSubstitutor) =
    (`type` match {
      case that: TypeParameterType => (that.psiTypeParameter eq psiTypeParameter) || {
        (psiTypeParameter, that.psiTypeParameter) match {
          case (myBound: ScTypeParam, thatBound: ScTypeParam) =>
            //TODO this is a temporary hack, so ignore substitutor for now
            myBound.lowerBound.exists(_.equiv(thatBound.lowerBound.getOrNothing)) &&
              myBound.upperBound.exists(_.equiv(thatBound.upperBound.getOrNothing)) &&
              (myBound.name == thatBound.name || thatBound.isHigherKindedTypeParameter || myBound.isHigherKindedTypeParameter)
          case _ => false
        }
      }
      case _ => false
    }, substitutor)

  override def visitType(visitor: TypeVisitor): Unit = visitor.visitTypeParameterType(this)
}

object TypeParameterType {
  def apply(tp: TypeParameter): TypeParameterType = LazyTpt(tp)

  def apply(psiTp: PsiTypeParameter): TypeParameterType = LazyTpt(TypeParameter(psiTp))

  def apply(psiTp: PsiTypeParameter, substitutor: ScSubstitutor): TypeParameterType =
    LazyTpt(TypeParameter(psiTp), substitutor)

  def apply(typeParameter: TypeParameter,
            arguments: Seq[TypeParameterType],
            lowerType: ScType,
            upperType: ScType): TypeParameterType = StrictTpt(typeParameter, arguments, lowerType, upperType)

  def unapply(tpt: TypeParameterType): Option[(TypeParameter, Seq[TypeParameterType], ScType, ScType)] =
    Some(tpt.typeParameter, tpt.arguments, tpt.lowerType, tpt.upperType)

  object ofPsi {
    def unapply(tp: TypeParameterType): Option[PsiTypeParameter] = Some(tp.psiTypeParameter)
  }

  private case class LazyTpt(typeParameter: TypeParameter, substitutor: ScSubstitutor = ScSubstitutor.empty)
    extends TypeParameterType {

    val arguments: Seq[TypeParameterType] = typeParameter.typeParameters.map(LazyTpt(_, substitutor))

    lazy val lowerType: ScType = substitutor.subst(typeParameter.lowerType)

    lazy val upperType: ScType = substitutor.subst(typeParameter.upperType)
  }

  private case class StrictTpt(typeParameter: TypeParameter,
                               arguments: Seq[TypeParameterType],
                               override val lowerType: ScType,
                               override val upperType: ScType) extends TypeParameterType
}
