package org.jetbrains.plugins.scala.lang.psi.types


trait TypePresentationTransformer { self =>
  def transform(tpe: ScType): ScType = transformImpl.applyOrElse(tpe, identity[ScType])
  
  def |>(other: TypePresentationTransformer): TypePresentationTransformer = 
    new TypePresentationTransformer {
    override protected def transformImpl: PartialFunction[ScType, ScType] = self.transformImpl.andThen(other.transform)
  }
  
  protected def transformImpl: PartialFunction[ScType, ScType]
}

object TypePresentationTransformer {
  private[this] val objectTypeName       = "_root_.java.lang.Object"
  private[this] val productTypeName      = "_root_.scala.Product"
  private[this] val serializableTypeName = "_root_.scala.Serializable"
  private[this] val uselessTypeNames     = Set(objectTypeName, productTypeName, serializableTypeName)
  
  val cleanUp: TypePresentationTransformer =
    RemoveUnnecessaryRefinements |> RemoveUselessComponents(uselessTypeNames)
  
  final case object RemoveUnnecessaryRefinements extends TypePresentationTransformer {
    override protected def transformImpl: PartialFunction[ScType, ScType] = {
      case tpe @ ScCompoundType(Seq(obj), _, _) if obj.canonicalText == objectTypeName => tpe
      case tpe: ScCompoundType =>
        tpe.copy(signatureMap = Map.empty, typesMap = Map.empty)(tpe.projectContext)
    }
  }
  
  final case class RemoveUselessComponents(uselessTypes: Set[String]) extends TypePresentationTransformer {
    override protected def transformImpl: PartialFunction[ScType, ScType] = {
      case tpe @ ScCompoundType(components, _, _) => 
        val filtered = components.filterNot(tpe => uselessTypes.contains(tpe.canonicalText))
        tpe.copy(components = filtered)(tpe.projectContext)
    }
  }
  
  final case object Identity extends TypePresentationTransformer {
    override protected def transformImpl: PartialFunction[ScType, ScType] = PartialFunction.empty
  }
}
