package org.jetbrains.plugins.dotty.lang.psi.types

import org.jetbrains.plugins.scala.lang.psi.types.ScType

/**
  * @author adkozlov
  */
trait DottyType extends ScType {
  override def typeSystem: DottyTypeSystem = DottyTypeSystem
}