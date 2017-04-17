package org.jetbrains.plugins.scala.lang.psi
package stubs
package index

import com.intellij.psi.PsiClass
import com.intellij.psi.stubs.{StringStubIndexExtension, StubIndexKey}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject

/**
  * @author ilyas
  */
class ScShortClassNameIndex extends StringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.SHORT_NAME_KEY
}

class ScNotVisibleInJavaShortClassNameIndex extends StringStubIndexExtension[PsiClass] {
  override def getKey: StubIndexKey[String, PsiClass] =
    ScalaIndexKeys.NOT_VISIBLE_IN_JAVA_SHORT_NAME_KEY
}

class ScShortNamePackageObjectIndex extends StringStubIndexExtension[ScObject] {
  override def getKey: StubIndexKey[String, ScObject] =
    ScalaIndexKeys.PACKAGE_OBJECT_SHORT_NAME_KEY
}
