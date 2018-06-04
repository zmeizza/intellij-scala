package org.jetbrains.plugins.scala
package lang
package psi
package impl
package toplevel
package typedef

/**
 * @author ilyas
 */

import com.intellij.lang.ASTNode
import com.intellij.lang.java.JavaLanguage
import com.intellij.navigation._
import com.intellij.openapi.editor.colors._
import com.intellij.psi._
import com.intellij.psi.impl._
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon
import org.jetbrains.plugins.scala.caches.DropOn
import org.jetbrains.plugins.scala.conversion.JavaToScala
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.lang.lexer._
import org.jetbrains.plugins.scala.lang.psi.api.ScalaFile
import org.jetbrains.plugins.scala.lang.psi.api.base.ScModifierList
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScBlock
import org.jetbrains.plugins.scala.lang.psi.api.statements.params.ScTypeParam
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScPackaging
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.{ScExtendsBlock, ScTemplateBody, ScTemplateParents}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef._
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createMethodFromText
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.JavaIdentifier
import org.jetbrains.plugins.scala.lang.psi.stubs.ScTemplateDefinitionStub
import org.jetbrains.plugins.scala.lang.psi.stubs.elements.ScTemplateDefinitionElementType
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api.TypeParameterType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScProjectionType, ScThisType}
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.macroAnnotations.Cached
import org.jetbrains.plugins.scala.projectView.{ClassAndCompanionObject, SingularDefinition, TraitAndCompanionObject}

import scala.annotation.tailrec
import scala.collection.Seq
import scala.reflect.NameTransformer

abstract class ScTypeDefinitionImpl protected (stub: ScTemplateDefinitionStub,
                                               nodeType: ScTemplateDefinitionElementType[_ <: ScTypeDefinition],
                                               node: ASTNode)
  extends ScalaStubBasedElementImpl(stub, nodeType, node) with ScTypeDefinition with PsiClassFake {

  override def hasTypeParameters: Boolean = typeParameters.nonEmpty

  override def typeParameters: Seq[ScTypeParam] = desugaredElement match {
    case Some(td: ScTypeDefinition) => td.typeParameters
    case _ => super.typeParameters
  }

  override def add(element: PsiElement): PsiElement = {
    element match {
      case member: PsiMember if member.getLanguage.isKindOf(JavaLanguage.INSTANCE) =>
        val newMemberText = JavaToScala.convertPsiToText(member).trim()
        val mem: Option[ScMember] = member match {
          case _: PsiMethod =>
            Some(createMethodFromText(newMemberText))
          case _ => None
        }
        mem match {
          case Some(m) => addMember(m, None)
          case _ => super.add(element)
        }
      case mem: ScMember => addMember(mem, None)
      case _ => super.add(element)
    }
  }

  override def getSuperTypes: Array[PsiClassType] = {
    superTypes.flatMap {
      case tp =>
        val psiType = tp.toPsiType
        psiType match {
          case c: PsiClassType => Seq(c)
          case _ => Seq.empty
        }
    }.toArray
  }

  override def isAnnotationType: Boolean = {
    elementScope.getCachedClass("scala.annotation.Annotation")
      .exists(isInheritor(_, deep = true))
  }

  def `type`(): TypeResult = {
    val parentClass: ScTemplateDefinition = containingClass
    if (typeParameters.isEmpty) {
      if (parentClass != null) {
        Right(ScProjectionType(ScThisType(parentClass), this))
      } else {
        Right(ScalaType.designator(this))
      }
    } else {
      if (parentClass != null) {
        Right(ScParameterizedType(ScProjectionType(ScThisType(parentClass), this),
          typeParameters.map(TypeParameterType(_))))
      } else {
        Right(ScParameterizedType(ScalaType.designator(this),
          typeParameters.map(TypeParameterType(_))))
      }
    }
  }

  def getTypeWithProjections(thisProjections: Boolean = false): TypeResult = {
    def args = typeParameters.map(TypeParameterType(_))
    def innerType = if (typeParameters.isEmpty) ScalaType.designator(this)
    else ScParameterizedType(ScalaType.designator(this), args)
    val parentClazz = containingClass
    if (parentClazz != null) {
      val tpe: ScType = if (!thisProjections) parentClazz.getTypeWithProjections()
        .getOrElse(return Failure("Cannot resolve parent class"))
      else ScThisType(parentClazz)

      val innerProjection = ScProjectionType(tpe, this)
      Right(if (typeParameters.isEmpty) innerProjection
      else ScParameterizedType(innerProjection, args))
    } else Right(innerType)
  }

  override def getModifierList: ScModifierList = super[ScTypeDefinition].getModifierList

  // TODO Should be unified, see ScModifierListOwner
  override def hasModifierProperty(name: String): Boolean = super[ScTypeDefinition].hasModifierProperty(name)

  override def getNavigationElement: PsiElement = getContainingFile match {
    case s: ScalaFileImpl if s.isCompiled => getSourceMirrorClass
    case _ => this
  }

  private def hasSameScalaKind(other: PsiClass) = (this, other) match {
    case (_: ScTrait, _: ScTrait)
            | (_: ScObject, _: ScObject)
            | (_: ScClass, _: ScClass) => true
    case _ => false
  }

  def getSourceMirrorClass: PsiClass = {
    val classParent = PsiTreeUtil.getParentOfType(this, classOf[ScTypeDefinition], true)
    val name = this.name
    if (classParent == null) {
      val classes: Array[PsiClass] = getContainingFile.getNavigationElement match {
        case o: ScalaFile => o.typeDefinitions.toArray
        case o: PsiClassOwner => o.getClasses
      }
      val classesIterator = classes.iterator
      while (classesIterator.hasNext) {
        val c = classesIterator.next()
        if (name == c.name && hasSameScalaKind(c)) return c
      }
    } else {
      val parentSourceMirror = classParent.asInstanceOf[ScTypeDefinitionImpl].getSourceMirrorClass
      parentSourceMirror match {
        case td: ScTypeDefinitionImpl => for (i <- td.typeDefinitions if name == i.name && hasSameScalaKind(i))
          return i
        case _ => this
      }
    }
    this
  }

  override def isLocal: Boolean =
    byStubOrPsi(_.isLocal)(containingClass == null && PsiTreeUtil.getParentOfType(this, classOf[ScTemplateDefinition]) != null)

  def nameId: PsiElement = findChildByType[PsiElement](ScalaTokenTypes.tIDENTIFIER)

  override def getTextOffset: Int = nameId.getTextRange.getStartOffset

  override def getContainingClass: PsiClass = {
    super[ScTypeDefinition].getContainingClass match {
      case o: ScObject => o.fakeCompanionClassOrCompanionClass
      case containingClass => containingClass
    }
  }

  @Cached(DropOn.anyScalaPsiChange, this)
  override final def getQualifiedName: String = byStubOrPsi(_.javaQualifiedName)(javaQualName())

  private def javaQualName(): String = {
    var res = qualifiedName(".", encodeName = true).split('.').map { s =>
      if (s.startsWith("`") && s.endsWith("`") && s.length > 2) s.drop(1).dropRight(1)
      else s
    }.mkString(".")
    this match {
      case o: ScObject =>
        if (o.isPackageObject) res = res + ".package$"
        else res = res + "$"
      case _ =>
    }
    res
  }

  @Cached(DropOn.anyScalaPsiChange, this)
  override def qualifiedName: String = byStubOrPsi(_.getQualifiedName)(qualName())

  private def qualName(): String = qualifiedName(".")

  override def getExtendsListTypes: Array[PsiClassType] = innerExtendsListTypes

  override def getImplementsListTypes: Array[PsiClassType] = innerExtendsListTypes

  def getTruncedQualifiedName: String = qualifiedName(".", trunced = true)

  def getQualifiedNameForDebugger: String = {
    containingClass match {
      case td: ScTypeDefinition => td.getQualifiedNameForDebugger + "$" + transformName(encodeName = true, name)
      case _ =>
        if (this.isPackageObject) qualifiedName("", encodeName = true) + ".package"
        else qualifiedName("$", encodeName = true)
    }
  }

  protected def transformName(encodeName: Boolean, name: String): String = {
    if (!encodeName) name
    else {
      val deticked =
        if (name.startsWith("`") && name.endsWith("`") && name.length() > 1)
          name.substring(1, name.length() - 1)
        else name
      NameTransformer.encode(deticked)
    }
  }

  protected def qualifiedName(classSeparator: String, trunced: Boolean = false,
                              encodeName: Boolean = false): String = {
    // Returns prefix with convenient separator sep
    @tailrec
    def _packageName(e: PsiElement, sep: String, k: (String) => String): String = e.getContext match {
      case o: ScObject if o.isPackageObject && o.name == "`package`" => _packageName(o, sep, k)
      case _: ScClass | _: ScTrait if trunced => k("")
      case t: ScTypeDefinition => _packageName(t, sep, (s) => {
        val name = t.name
        k(s + transformName(encodeName, name) + sep)
      })
      case p: ScPackaging => _packageName(p, ".", (s) => k(s + p.packageName + "."))
      case _: ScalaFile => val pn = ""; k(if (pn.length > 0) pn + "." else "")
      case _: PsiFile | null => k("")
      case _: ScBlock => k("")
      case parent: ScTemplateBody => _packageName(parent, sep, k)
      case parent: ScExtendsBlock => _packageName(parent, sep, k)
      case parent: ScTemplateParents => _packageName(parent, sep, k)
      case parent => _packageName(parent, sep, identity)
    }

    val packageName = _packageName(this, classSeparator, identity)
    packageName + transformName(encodeName, name)
  }

  override def getPresentation: ItemPresentation = {
    val presentableName = this match {
      case o: ScObject if o.isPackageObject && o.name == "`package`" =>
        val packageName = o.qualifiedName.stripSuffix(".`package`")
        val index = packageName.lastIndexOf('.')
        if (index < 0) packageName else packageName.substring(index + 1, packageName.length)
      case _ => name
    }

    new ItemPresentation() {
      def getPresentableText: String = presentableName

      def getTextAttributesKey: TextAttributesKey = null

      def getLocationString: String = getPath match {
        case "" => "<default>"
        case p => '(' + p + ')'
      }

      override def getIcon(open: Boolean): Icon = ScTypeDefinitionImpl.this.getIcon(0)
    }
  }


  override def findMethodBySignature(patternMethod: PsiMethod, checkBases: Boolean): PsiMethod = {
    super[ScTypeDefinition].findMethodBySignature(patternMethod, checkBases)
  }

  override def findMethodsBySignature(patternMethod: PsiMethod, checkBases: Boolean): Array[PsiMethod] = {
    super[ScTypeDefinition].findMethodsBySignature(patternMethod, checkBases)
  }

  import _root_.java.util.{Collection => JCollection, List => JList}

  import com.intellij.openapi.util.{Pair => IPair}

  override def findMethodsAndTheirSubstitutorsByName(name: String,
                                                     checkBases: Boolean): JList[IPair[PsiMethod, PsiSubstitutor]] = {
    super[ScTypeDefinition].findMethodsAndTheirSubstitutorsByName(name, checkBases)
  }

  override def getAllMethodsAndTheirSubstitutors: JList[IPair[PsiMethod, PsiSubstitutor]] = {
    super[ScTypeDefinition].getAllMethodsAndTheirSubstitutors
  }

  override def getVisibleSignatures: JCollection[HierarchicalMethodSignature] = {
    super[ScTypeDefinition].getVisibleSignatures
  }

  override def findMethodsByName(name: String, checkBases: Boolean): Array[PsiMethod] = {
    super[ScTypeDefinition].findMethodsByName(name, checkBases)
  }

  override def findFieldByName(name: String, checkBases: Boolean): PsiField = {
    super[ScTypeDefinition].findFieldByName(name, checkBases)
  }

  override def checkDelete() {
  }

  override def delete(): Unit = getContainingFile match {
    case file @ (SingularDefinition(_) |
                 ClassAndCompanionObject(_, _) |
                 TraitAndCompanionObject(_, _)) => file.delete()

    case _ => getParent.getNode.removeChild(getNode)
  }

  override def psiTypeParameters: Array[PsiTypeParameter] = typeParameters.toArray

  override def getSupers: Array[PsiClass] = extendsBlock.supers.filter(_ != this).toArray

  override def isInheritor(baseClass: PsiClass, deep: Boolean): Boolean =
    super[ScTypeDefinition].isInheritor(baseClass, deep)


  def signaturesByName(name: String): Seq[PhysicalSignature] = {
    (for ((s: PhysicalSignature, _) <- TypeDefinitionMembers.getSignatures(this).forName(name)._1) yield s) ++
            syntheticMethodsNoOverride.filter(_.name == name).map(new PhysicalSignature(_, ScSubstitutor.empty))
  }

  override def getNameIdentifier: PsiIdentifier = {
    Predef.assert(nameId != null, "Class hase null nameId. Class text: " + getText) //diagnostic for EA-20122
    new JavaIdentifier(nameId)
  }

  override def getDocComment: PsiDocComment = super[ScTypeDefinition].getDocComment

  override def isDeprecated: Boolean = byStubOrPsi(_.isDeprecated)(super[PsiClassFake].isDeprecated)

  override def psiInnerClasses: Array[PsiClass] = {
    val inCompanionModule = baseCompanionModule.toSeq.flatMap {
      case o: ScObject =>
        o.members.flatMap {
          case o: ScObject => Seq(o) ++ o.fakeCompanionClass
          case t: ScTrait => Seq(t, t.fakeCompanionClass)
          case c: ScClass => Seq(c)
          case _ => Seq.empty
        }
      case _ => Seq.empty
    }

    (members.collect {
      case c: PsiClass => c
    } ++ inCompanionModule).toArray
  }

  override def getAllInnerClasses: Array[PsiClass] = {
    PsiClassImplUtil.getAllInnerClasses(this)
  }

  override def findInnerClassByName(name: String, checkBases: Boolean): PsiClass = {
    super[ScTypeDefinition].findInnerClassByName(name, checkBases)
  }

  override def getAllFields: Array[PsiField] = {
    super[ScTypeDefinition].getAllFields
  }

  override def getOriginalElement: PsiElement = {
    ScalaPsiImplementationHelper.getOriginalClass(this)
  }

  @Cached(DropOn.semanticChange(this), this)
  private def cachedDesugared(tree: scala.meta.Tree): ScTemplateDefinition = {
    ScalaPsiElementFactory.createTemplateDefinitionFromText(tree.toString(), getContext, this)
      .setDesugared(actualElement = this)
  }

  override def desugaredElement: Option[ScTemplateDefinition] = {
    import scala.meta.intellij.psiExt._
    import scala.meta.{Defn, Term}

    val defn = this.getMetaExpansion match {
      case Right(templ: Defn.Class) => Some(templ)
      case Right(templ: Defn.Trait) => Some(templ)
      case Right(templ: Defn.Object) => Some(templ)
      case Right(Term.Block(Seq(templ: Defn.Class, _))) => Some(templ)
      case Right(Term.Block(Seq(templ: Defn.Trait, _))) => Some(templ)
      case Right(Term.Block(Seq(templ: Defn.Object, _))) => Some(templ)
      case _ => None
    }

    defn.map(cachedDesugared)
  }
}
