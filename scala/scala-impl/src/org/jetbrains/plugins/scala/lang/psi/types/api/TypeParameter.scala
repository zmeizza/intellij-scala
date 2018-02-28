package org.jetbrains.plugins.scala.lang.psi.types.api

import com.intellij.psi.PsiTypeParameter
import org.jetbrains.plugins.scala.extensions.{ObjectExt, PsiNamedElementExt}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.light.scala.ScExistentialLightTypeParam
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.result._

/**
  * Class representing type parameters in our type system. Can be constructed from psi.
  *
  * lowerType and upperType sometimes should be lazy, see SCL-7216
  */
sealed trait TypeParameter {
  val psiTypeParameter: PsiTypeParameter
  val typeParameters: Seq[TypeParameter]
  def lowerType: ScType
  def upperType: ScType

  def name: String = psiTypeParameter.name

  def update(function: ScType => ScType): TypeParameter = TypeParameter.StrictTp(
    psiTypeParameter,
    typeParameters.map(_.update(function)),
    function(lowerType),
    function(upperType))

  def updateWithVariance(function: (ScType, Variance) => ScType, variance: Variance): TypeParameter = TypeParameter.StrictTp(
    psiTypeParameter,
    typeParameters.map(_.updateWithVariance(function, variance)),
    function(lowerType, variance),
    function(upperType, -variance))

  def isInvariant: Boolean = psiTypeParameter.asOptionOf[ScTypeParam].exists { t =>
    !t.isCovariant && !t.isContravariant
  }

  def isCovariant: Boolean = psiTypeParameter.asOptionOf[ScTypeParam].exists(_.isCovariant)

  def isContravariant: Boolean = psiTypeParameter.asOptionOf[ScTypeParam].exists(_.isContravariant)
}

object TypeParameter {
  def apply(typeParameter: PsiTypeParameter): TypeParameter = typeParameter match {
    case typeParam: ScTypeParam => LazyScalaTp(typeParam)
    case _ => LazyJavaTp(typeParameter)
  }

  def apply(psiTypeParameter: PsiTypeParameter,
            typeParameters: Seq[TypeParameter],
            lType: ScType,
            uType: ScType): TypeParameter = StrictTp(psiTypeParameter, typeParameters, lType, uType)

  def light(name: String, typeParameters: Seq[TypeParameter], lower: ScType, upper: ScType): TypeParameter =
    LightTypeParameter(name, typeParameters, lower, upper)

  def unapply(tp: TypeParameter): Option[(PsiTypeParameter, Seq[TypeParameter], ScType, ScType)] =
    Some(tp.psiTypeParameter, tp.typeParameters, tp.lowerType, tp.upperType)

  def javaPsiTypeParameterUpperType(typeParameter: PsiTypeParameter): ScType = {
    val manager = ScalaPsiManager.instance(typeParameter.getProject)
    manager.javaPsiTypeParameterUpperType(typeParameter)
  }

  private case class StrictTp(psiTypeParameter: PsiTypeParameter,
                              typeParameters: Seq[TypeParameter],
                              override val lowerType: ScType,
                              override val upperType: ScType) extends TypeParameter

  private case class LazyScalaTp(psiTypeParameter: ScTypeParam) extends TypeParameter {
    override val typeParameters: Seq[TypeParameter] = psiTypeParameter.typeParameters.map(TypeParameter(_))

    override lazy val lowerType: ScType = psiTypeParameter.lowerBound.getOrNothing

    override lazy val upperType: ScType = psiTypeParameter.upperBound.getOrAny
  }

  private case class LazyJavaTp(psiTypeParameter: PsiTypeParameter) extends TypeParameter {
    override val typeParameters: Seq[TypeParameter] = Seq.empty

    override val lowerType: ScType = Nothing(psiTypeParameter.getProject)

    override lazy val upperType: ScType = TypeParameter.javaPsiTypeParameterUpperType(psiTypeParameter)
  }

  private case class LightTypeParameter(override val name: String,
                                        typeParameters: Seq[TypeParameter],
                                        override val lowerType: ScType,
                                        override val upperType: ScType) extends TypeParameter {

    override val psiTypeParameter: PsiTypeParameter = new ScExistentialLightTypeParam(name)(lowerType.projectContext)
  }
}