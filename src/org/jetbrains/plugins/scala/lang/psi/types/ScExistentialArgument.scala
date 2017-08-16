package org.jetbrains.plugins.scala.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.api.{Covariant, TypeParameterType, TypeVisitor, ValueType, Variance}
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.Update
import org.jetbrains.plugins.scala.project.ProjectContext

/**
  * Nikolay.Tropin
  * 15-Aug-17
  */
case class ScExistentialArgument(name: String, args: List[TypeParameterType], lower: ScType, upper: ScType)
  extends NamedType with ValueType with ScalaType {

  override implicit def projectContext: ProjectContext = lower.projectContext

  def withoutAbstracts: ScExistentialArgument = ScExistentialArgument(name, args, lower.removeAbstracts, upper.removeAbstracts)

  override def updateSubtypes(update: Update, visited: Set[ScType]): ScExistentialArgument = {
    ScExistentialArgument(name, args, lower.recursiveUpdateImpl(update, visited), upper.recursiveUpdateImpl(update, visited))
  }

  def recursiveVarianceUpdateModifiableNoUpdate[T](data: T, update: (ScType, Variance, T) => (Boolean, ScType, T),
                                                            variance: Variance = Covariant): ScExistentialArgument = {
    ScExistentialArgument(name, args, lower.recursiveVarianceUpdateModifiable(data, update, -variance),
      upper.recursiveVarianceUpdateModifiable(data, update, variance))
  }

  override def recursiveVarianceUpdateModifiable[T](data: T, update: (ScType, Variance, T) => (Boolean, ScType, T),
                                                    v: Variance = Covariant, revertVariances: Boolean = false): ScType = {
    update(this, v, data) match {
      case (true, res, _) => res
      case (_, _, newData) =>
        recursiveVarianceUpdateModifiableNoUpdate(newData, update, v)
    }
  }

  override def equivInner(r: ScType, uSubst: ScUndefinedSubstitutor, falseUndef: Boolean): (Boolean, ScUndefinedSubstitutor) = {
    r match {
      case exist: ScExistentialArgument =>
        var undefinedSubst = uSubst
        val s = (exist.args zip args).foldLeft(ScSubstitutor.empty) {(s, p) => s bindT ((p._1.name, -1), p._2)}
        val t = lower.equiv(s.subst(exist.lower), undefinedSubst, falseUndef)
        if (!t._1) return (false, undefinedSubst)
        undefinedSubst = t._2
        upper.equiv(s.subst(exist.upper), undefinedSubst, falseUndef)
      case _ => (false, uSubst)
    }
  }

  override def visitType[T](visitor: TypeVisitor[T]): T = visitor.visitExistentialArgument(this)
}
