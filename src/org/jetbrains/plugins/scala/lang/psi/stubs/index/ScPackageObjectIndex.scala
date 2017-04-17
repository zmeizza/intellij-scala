package org.jetbrains.plugins.scala.lang.psi
package stubs
package index

import com.intellij.psi.stubs.StubIndexKey
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

/**
  * @author ilyas
  */
class ScPackageObjectIndex extends ScIntStubIndexExtension[ScObject] {

  override def getKey: StubIndexKey[Integer, ScObject] =
    ScalaIndexKeys.PACKAGE_OBJECT_KEY
}
