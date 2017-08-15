package org.jetbrains.plugins.scala
package lang.psi.types.nonvalue

import org.jetbrains.plugins.scala.lang.psi.ElementScope
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api._
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.Update
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * @author ilyas
 */
case class ScMethodType(returnType: ScType, params: Seq[Parameter], isImplicit: Boolean)
                       (implicit val elementScope: ElementScope) extends NonValueType {
  implicit def projectContext: ProjectContext = elementScope.projectContext

  override def visitType[T](visitor: TypeVisitor[T]): T = visitor match {
    case v: MethodTypeVisitor[T] => v.visitMethodType(this)
    case _ => visitor.notSupported(this)
  }

  override def typeDepth: Int = returnType.typeDepth

  def inferValueType: ValueType = {
    FunctionType(returnType.inferValueType, params.map(p => {
      val inferredParamType = p.paramType.inferValueType
      if (!p.isRepeated) inferredParamType
      else {
        val seqClass = elementScope.getCachedClass("scala.collection.Seq")
        seqClass.fold(inferredParamType) { inferred =>
            ScParameterizedType(ScDesignatorType(inferred), Seq(inferredParamType))
        }
      }
    }))
  }

  override def removeAbstracts = ScMethodType(returnType.removeAbstracts,
    params.map(p => p.copy(paramType = p.paramType.removeAbstracts)),
    isImplicit)

  override def updateSubtypes(update: Update, visited: Set[ScType]): ScMethodType = {
    ScMethodType(
      returnType.recursiveUpdateImpl(update, visited),
      params.map(p => p.copy(paramType = p.paramType.recursiveUpdateImpl(update, visited))),
      isImplicit
    )
  }

  override def recursiveVarianceUpdateModifiable[T](data: T, update: (ScType, Variance, T) => (Boolean, ScType, T),
                                           variance: Variance = Covariant, revertVariances: Boolean = false): ScType = {
    update(this, variance, data) match {
      case (true, res, _) => res
      case (_, _, newData) =>
        ScMethodType(returnType.recursiveVarianceUpdateModifiable(newData, update, variance),
          params.map(p => p.copy(paramType = p.paramType.recursiveVarianceUpdateModifiable(newData, update, -variance))),
          isImplicit)
    }
  }

  override def equivInner(r: ScType, uSubst: ScUndefinedSubstitutor, falseUndef: Boolean): (Boolean, ScUndefinedSubstitutor) = {
    var undefinedSubst = uSubst
    r match {
      case m: ScMethodType =>
        if (m.params.length != params.length) return (false, undefinedSubst)
        var t = m.returnType.equiv(returnType, undefinedSubst, falseUndef)
        if (!t._1) return (false, undefinedSubst)
        undefinedSubst = t._2
        var i = 0
        while (i < params.length) {
          //todo: Seq[Type] instead of Type*
          if (params(i).isRepeated != m.params(i).isRepeated) return (false, undefinedSubst)
          t = params(i).paramType.equiv(m.params(i).paramType, undefinedSubst, falseUndef)
          if (!t._1) return (false, undefinedSubst)
          undefinedSubst = t._2
          i = i + 1
        }
        (true, undefinedSubst)
      case _ => (false, undefinedSubst)
    }
  }
}

trait MethodTypeVisitor[T] extends TypeVisitor[T] {
  def visitMethodType(tp: ScMethodType): T = default(tp)
}