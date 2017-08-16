package org.jetbrains.plugins.scala.lang.psi.types.nonvalue

import org.jetbrains.plugins.scala.lang.psi.types.api._
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.Update
import org.jetbrains.plugins.scala.lang.psi.types.{ScAbstractType, ScSubstitutor, ScType, ScUndefinedSubstitutor, ScalaType, api}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class ScTypePolymorphicType(internalType: ScType, typeParameters: Seq[TypeParameter]) extends NonValueType with ScalaType {

  if (internalType.isInstanceOf[ScTypePolymorphicType]) {
    throw new IllegalArgumentException("Polymorphic type can't have wrong internal type")
  }

  override implicit def projectContext: ProjectContext = internalType.projectContext

  def polymorphicTypeSubstitutor: ScSubstitutor = polymorphicTypeSubstitutor(inferValueType = false)

  def polymorphicTypeSubstitutor(inferValueType: Boolean): ScSubstitutor =
    ScSubstitutor(typeParameters.map(tp => {
      var contraVariant = 0
      var coOrInVariant = 0
      internalType.recursiveVarianceUpdate {
        case (typez: ScType, v: Variance) =>
          val pair = typez match {
            case tp: TypeParameterType => tp.nameAndId
            case UndefinedType(tp, _) => tp.nameAndId
            case ScAbstractType(tp, _, _) => tp.nameAndId
            case _ => null
          }
          if (pair != null) {
            if (tp.nameAndId == pair) {
              if (v == Contravariant) contraVariant += 1
              else coOrInVariant += 1
            }
          }
          (false, typez)
      }
      if (coOrInVariant == 0 && contraVariant != 0)
        (tp.nameAndId, tp.upperType.v.inferValueType)
      else
        (tp.nameAndId, tp.lowerType.v.inferValueType)
    }).toMap)

  def abstractTypeSubstitutor: ScSubstitutor = {
    ScSubstitutor(typeParameters.map(tp => {
      val lowerType: ScType = if (hasRecursiveTypeParameters(tp.lowerType.v)) Nothing else tp.lowerType.v
      val upperType: ScType = if (hasRecursiveTypeParameters(tp.upperType.v)) Any else tp.upperType.v
      (tp.nameAndId, ScAbstractType(TypeParameterType(tp.psiTypeParameter), lowerType, upperType))
    }).toMap)
  }

  def abstractOrLowerTypeSubstitutor: ScSubstitutor = {
    ScSubstitutor(typeParameters.map(tp => {
      val lowerType: ScType = if (hasRecursiveTypeParameters(tp.lowerType.v)) Nothing else tp.lowerType.v
      val upperType: ScType = if (hasRecursiveTypeParameters(tp.upperType.v)) Any else tp.upperType.v
      (tp.nameAndId,
        if (lowerType.equiv(Nothing)) ScAbstractType(TypeParameterType(tp.psiTypeParameter), lowerType, upperType)
        else lowerType)
    }).toMap)
  }

  private lazy val nameAndIds = typeParameters.map(_.nameAndId).toSet

  private def hasRecursiveTypeParameters(typez: ScType): Boolean = typez.hasRecursiveTypeParameters(nameAndIds)

  def typeParameterTypeSubstitutor: ScSubstitutor =
    ScSubstitutor(typeParameters.map { tp =>
      (tp.nameAndId, TypeParameterType(tp.psiTypeParameter))
    }.toMap)

  def inferValueType: ValueType = {
    polymorphicTypeSubstitutor(inferValueType = true).subst(internalType.inferValueType).asInstanceOf[ValueType]
  }

  override def removeAbstracts = ScTypePolymorphicType(internalType.removeAbstracts,
    typeParameters.map {
      case TypeParameter(parameters, lowerType, upperType, psiTypeParameter) =>
        TypeParameter(parameters, // todo: ?
          lowerType.v.removeAbstracts,
          upperType.v.removeAbstracts,
          psiTypeParameter)
    })

  override def updateSubtypes(update: Update, visited: Set[ScType]): ScType = {
    ScTypePolymorphicType(
      internalType.recursiveUpdateImpl(update, visited),
      typeParameters.map {
        case TypeParameter(parameters, lowerType, upperType, psiTypeParameter) =>
          TypeParameter(parameters, // TODO: ?
            lowerType.v.recursiveUpdateImpl(update, visited, isLazySubtype = true),
            upperType.v.recursiveUpdateImpl(update, visited, isLazySubtype = true),
            psiTypeParameter)
      })
  }

  override def recursiveVarianceUpdateModifiable[T](data: T, update: (ScType, Variance, T) => (Boolean, ScType, T),
                                                    v: Variance = Covariant, revertVariances: Boolean = false): ScType = {
    update(this, v, data) match {
      case (true, res, _) => res
      case (_, _, newData) =>
        def innerUpdate(`type`: ScType, variance: Variance) =
          `type`.recursiveVarianceUpdateModifiable(newData, update, variance)

        ScTypePolymorphicType(innerUpdate(internalType, v),
          typeParameters.map {
            case TypeParameter(parameters, lowerType, upperType, psiTypeParameter) =>
              TypeParameter(parameters, // TODO: ?
                innerUpdate(lowerType.v, -v),
                innerUpdate(upperType.v, v),
                psiTypeParameter)
          })
    }
  }

  override def equivInner(r: ScType, uSubst: ScUndefinedSubstitutor, falseUndef: Boolean): (Boolean, ScUndefinedSubstitutor) = {
    var undefinedSubst = uSubst
    r match {
      case p: ScTypePolymorphicType =>
        if (typeParameters.length != p.typeParameters.length) return (false, undefinedSubst)
        var i = 0
        while (i < typeParameters.length) {
          var t = typeParameters(i).lowerType.v.equiv(p.typeParameters(i).lowerType.v, undefinedSubst, falseUndef)
          if (!t._1) return (false,undefinedSubst)
          undefinedSubst = t._2
          t = typeParameters(i).upperType.v.equiv(p.typeParameters(i).upperType.v, undefinedSubst, falseUndef)
          if (!t._1) return (false, undefinedSubst)
          undefinedSubst = t._2
          i = i + 1
        }
        val keys = typeParameters.map(_.nameAndId)
        val values = p.typeParameters.map(TypeParameterType(_))
        val subst = ScSubstitutor(keys.zip(values).toMap)
        subst.subst(internalType).equiv(p.internalType, undefinedSubst, falseUndef)
      case _ => (false, undefinedSubst)
    }
  }

  override def visitType[T](visitor: TypeVisitor[T]): T = visitor.visitTypePolymorphicType(this)

  override def typeDepth: Int = internalType.typeDepth.max(typeParameters.toArray.depth)
}