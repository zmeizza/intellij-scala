package org.jetbrains.plugins.scala
package lang
package psi
package api
package statements


import java.util

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiReferenceList.Role
import com.intellij.psi._
import com.intellij.psi.impl.source.HierarchicalMethodSignatureImpl
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import com.intellij.util.PlatformIcons
import javax.swing.Icon
import org.jetbrains.plugins.scala.JavaArrayFactoryUtil.ScFunctionFactory
import org.jetbrains.plugins.scala.extensions._
import org.jetbrains.plugins.scala.icons.Icons
import org.jetbrains.plugins.scala.lang.lexer.ScalaTokenTypes
import org.jetbrains.plugins.scala.lang.parser.ScalaElementTypes._
import org.jetbrains.plugins.scala.lang.psi.api.base.ScMethodLike
import org.jetbrains.plugins.scala.lang.psi.api.base.types._
import org.jetbrains.plugins.scala.lang.psi.api.expr.{ScBlock, ScBlockStatement}
import org.jetbrains.plugins.scala.lang.psi.api.statements.params._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel._
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.templates.ScExtendsBlock
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef._
import org.jetbrains.plugins.scala.lang.psi.fake.{FakePsiReferenceList, FakePsiTypeParameterList}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiElementFactory.createClauseFromText
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.synthetic.{JavaIdentifier, SyntheticClasses}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.light.ScFunctionWrapper
import org.jetbrains.plugins.scala.lang.psi.light.scala.{ScLightFunctionDeclaration, ScLightFunctionDefinition}
import org.jetbrains.plugins.scala.lang.psi.types._
import org.jetbrains.plugins.scala.lang.psi.types.api._
import org.jetbrains.plugins.scala.lang.psi.types.nonvalue._
import org.jetbrains.plugins.scala.lang.psi.types.recursiveUpdate.ScSubstitutor
import org.jetbrains.plugins.scala.lang.psi.types.result._
import org.jetbrains.plugins.scala.lang.refactoring.util.ScalaNamesUtil
import org.jetbrains.plugins.scala.macroAnnotations.{Cached, CachedInUserData, ModCount}
import org.jetbrains.plugins.scala.project.UserDataHolderExt

import scala.annotation.tailrec
import scala.collection.Seq
import scala.collection.immutable.Set
import scala.collection.mutable.ArrayBuffer

/**
 * @author Alexander Podkhalyuzin
 */

/**
 * Represents Scala's internal function definitions and declarations
 */
trait ScFunction extends ScalaPsiElement with ScMember with ScTypeParametersOwner
  with ScParameterOwner with ScDocCommentOwner with ScTypedDefinition with ScCommentOwner
  with ScDeclaredElementsHolder with ScMethodLike with ScBlockStatement with ScDecoratedIconOwner {

  def isSyntheticCopy: Boolean = synthNavElement.nonEmpty && name == "copy"
  def isSyntheticApply: Boolean = synthNavElement.nonEmpty && name == "apply"
  def isSyntheticUnapply: Boolean = synthNavElement.nonEmpty && name == "unapply"
  def isSyntheticUnapplySeq: Boolean = synthNavElement.nonEmpty && name == "unapplySeq"

  def hasUnitResultType: Boolean = {
    @tailrec
    def hasUnitRT(t: ScType): Boolean = t match {
      case _ if t.isUnit => true
      case ScMethodType(result, _, _) => hasUnitRT(result)
      case _ => false
    }
    this.returnType.exists(hasUnitRT)
  }

  def isParameterless: Boolean = paramClauses.clauses.isEmpty

  private val probablyRecursive: ThreadLocal[Boolean] = new ThreadLocal[Boolean]() {
    override def initialValue(): Boolean = false
  }
  def isProbablyRecursive: Boolean = probablyRecursive.get()
  def setProbablyRecursive(b: Boolean) {probablyRecursive.set(b)}

  def isEmptyParen: Boolean = paramClauses.clauses.size == 1 && paramClauses.params.isEmpty

  def addEmptyParens() {
    val clause = createClauseFromText("()")
    paramClauses.addClause(clause)
  }

  def removeAllClauses() {
    paramClauses.clauses.headOption.zip(paramClauses.clauses.lastOption).foreach { p =>
      paramClauses.deleteChildRange(p._1, p._2)
    }
  }

  def isNative: Boolean = hasAnnotation("scala.native")

  // TODO Should be unified, see ScModifierListOwner
  override def hasModifierProperty(name: String): Boolean = {
    if (name == "abstract") {
      this match {
        case _: ScFunctionDeclaration =>
          containingClass match {
            case _: ScTrait => return true
            case c: ScClass if c.hasAbstractModifier => return true
            case _ =>
          }
        case _ =>
      }
    }
    super.hasModifierProperty(name)
  }

  override def getTextOffset: Int = nameId.getTextRange.getStartOffset

  def hasParameterClause: Boolean = {
    if (effectiveParameterClauses.nonEmpty) return true
    superMethod match {
      case Some(fun: ScFunction) => fun.hasParameterClause
      case Some(_: PsiMethod) => true
      case None => false
    }
  }

  /**
   * Signature has repeated param, which is not the last one
   */
  def hasMalformedSignature: Boolean = {
    val clausesIterator = paramClauses.clauses.iterator
    while (clausesIterator.hasNext) {
      val clause = clausesIterator.next()
      val paramsIterator = clause.parameters.iterator
      while (paramsIterator.hasNext) {
        val param = paramsIterator.next()
        if (paramsIterator.hasNext && param.isRepeatedParameter) return true
      }
    }
    false
  }

  def definedReturnType: TypeResult = {
    returnTypeElement match {
      case Some(ret) => ret.`type`()
      case _ if !hasAssign => Right(Unit)
      case _ =>
        superMethod match {
          case Some(f: ScFunction) => f.definedReturnType
          case Some(m: PsiMethod) =>
            Right(m.getReturnType.toScType())
          case _ => Failure("No defined return type")
        }
    }
  }

  /**
   * Optional Type Element, denotion function's return type
   * May be omitted for non-recursive functions
   */
  def returnTypeElement: Option[ScTypeElement]

  def hasExplicitType: Boolean = returnTypeElement.isDefined

  def removeExplicitType() {
    val colon = this.children.find(_.getNode.getElementType == ScalaTokenTypes.tCOLON)
    (colon, returnTypeElement) match {
      case (Some(first), Some(last)) => deleteChildRange(first, last)
      case _ =>
    }
  }

  def paramClauses: ScParameters

  def parameterList: ScParameters = paramClauses // TODO merge

  def isProcedure: Boolean = paramClauses.clauses.isEmpty

  protected def returnTypeInner: TypeResult

  def declaredType: TypeResult = this.flatMapType(returnTypeElement)

  def clauses: Option[ScParameters] = Some(paramClauses)

  @CachedInUserData(this, ModCount.getBlockModificationCount)
  def effectiveParameterClauses: Seq[ScParameterClause] = {
    val maybeOwner = if (isConstructor) {
      containingClass match {
        case owner: ScTypeParametersOwner => Some(owner)
        case _ => None
      }
    } else Some(this)

    paramClauses.clauses ++ maybeOwner.flatMap {
      ScalaPsiUtil.syntheticParamClause(_, paramClauses, isClassParameter = false)()
    }
  }

  def declaredElements = Seq(this)

  /**
   * Seek parameter with appropriate name in appropriate parameter clause.
    *
    * @param name          parameter name
   * @param clausePosition = -1, effective clause number, if -1 then parameter in any explicit? clause
   */
  def getParamByName(name: String, clausePosition: Int = -1): Option[ScParameter] = {
    clausePosition match {
      case -1 =>
        parameters.find { param =>
          ScalaNamesUtil.equivalent(param.name, name) ||
            param.deprecatedName.exists(ScalaNamesUtil.equivalent(_, name))
        }
      case i if i < 0 || i >= effectiveParameterClauses.length => None
      case _ =>
        effectiveParameterClauses.apply(clausePosition).effectiveParameters.find { param =>
          ScalaNamesUtil.equivalent(param.name, name) ||
            param.deprecatedName.exists(ScalaNamesUtil.equivalent(_, name))
        }
    }
  }

  override def accept(visitor: ScalaElementVisitor) {
    visitor.visitFunction(this)
  }

  def getGetterOrSetterFunction: Option[ScFunction] = {
    containingClass match {
      case clazz: ScTemplateDefinition =>
        if (name.endsWith("_=")) {
          clazz.functions.find(_.name == name.substring(0, name.length - 2))
        } else if (!hasParameterClause) {
          clazz.functions.find(_.name == name + "_=")
        } else None
      case _ => None
    }
  }

  def isBridge: Boolean = {
    //todo: fix algorithm for annotation resolve to not resolve objects (if it's possible)
    //heuristic algorithm to avoid SOE in MixinNodes.build
    annotations.exists(annot => {
      annot.typeElement match {
        case s: ScSimpleTypeElement => s.reference match {
          case Some(ref) => ref.refName == "bridge"
          case _ => false
        }
        case _ => false
      }
    })
  }

  def psiTypeParameters: Array[PsiTypeParameter] = typeParameters.toArray

  def getTypeParameterList = new FakePsiTypeParameterList(getManager, getLanguage, typeParameters.toArray, this)

  def hasTypeParameters: Boolean = typeParameters.nonEmpty

  def getParameterList: ScParameters = paramClauses

  @tailrec
  private def isJavaVarargs: Boolean = {
    if (hasAnnotation("scala.annotation.varargs")) true
    else {
      superMethod match {
        case Some(f: ScFunction) => f.isJavaVarargs
        case Some(m: PsiMethod) => m.isVarArgs
        case _ => false
      }
    }
  }

  /**
   * @return Empty array, if containing class is null.
   */
  @Cached(ModCount.getBlockModificationCount, this)
  def getFunctionWrappers(isStatic: Boolean, isInterface: Boolean, cClass: Option[PsiClass] = None): Seq[ScFunctionWrapper] = {
    val buffer = new ArrayBuffer[ScFunctionWrapper]
    if (cClass.isDefined || containingClass != null) {
      buffer += new ScFunctionWrapper(this, isStatic, isInterface, cClass)
      for {
        clause <- clauses
        first <- clause.clauses.headOption
        if first.hasRepeatedParam
        if isJavaVarargs
      } {
        buffer += new ScFunctionWrapper(this, isStatic, isInterface, cClass, isJavaVarargs = true)
      }

      val params = parameters
      for (i <- params.indices if params(i).baseDefaultParam) {
        buffer += new ScFunctionWrapper(this, isStatic = isStatic || isConstructor, isInterface, cClass, forDefault = Some(i + 1))
      }
    }
    buffer
  }

  def parameters: Seq[ScParameter] = paramClauses.params

  // TODO unify with ScValue and ScVariable
  protected override def getBaseIcon(flags: Int): Icon = {
    var parent = getParent
    while (parent != null) {
      parent match {
        case _: ScExtendsBlock =>
          return if (isAbstractMember) PlatformIcons.ABSTRACT_METHOD_ICON else PlatformIcons.METHOD_ICON
        case (_: ScBlock | _: ScalaFile) => return Icons.FUNCTION
        case _ => parent = parent.getParent
      }
    }
    null
  }

  def getReturnType: PsiType = {
    if (DumbService.getInstance(getProject).isDumb || !SyntheticClasses.get(getProject).isClassesRegistered) {
      return null //no resolve during dumb mode or while synthetic classes is not registered
    }
    getReturnTypeImpl
  }

  @CachedInUserData(this, ModCount.getBlockModificationCount)
  private def getReturnTypeImpl: PsiType = {
    val resultType = `type`().getOrAny match {
      case FunctionType(rt, _) => rt
      case tp => tp
    }
    resultType.toPsiType
  }

  def superMethods: Seq[PsiMethod] = {
    val clazz = containingClass
    if (clazz != null) TypeDefinitionMembers.getSignatures(clazz).forName(ScalaNamesUtil.clean(name))._1.
      get(new PhysicalSignature(this, ScSubstitutor.empty)).getOrElse(return Seq.empty).supers.
      filter(_.info.isInstanceOf[PhysicalSignature]).map {_.info.asInstanceOf[PhysicalSignature].method}
    else Seq.empty
  }

  def superMethod: Option[PsiMethod] = superMethodAndSubstitutor.map(_._1)

  def superMethodAndSubstitutor: Option[(PsiMethod, ScSubstitutor)] = {
    val clazz = containingClass
    if (clazz != null) {
      val option = TypeDefinitionMembers.getSignatures(clazz).forName(name)._1.
        fastPhysicalSignatureGet(new PhysicalSignature(this, ScSubstitutor.empty))
      if (option.isEmpty) return None
      option.get.primarySuper.filter(_.info.isInstanceOf[PhysicalSignature]).
        map(node => (node.info.asInstanceOf[PhysicalSignature].method, node.info.substitutor))
    }
    else None
  }


  def superSignatures: Seq[Signature] = {
    val clazz = containingClass
    val s = new PhysicalSignature(this, ScSubstitutor.empty)
    if (clazz == null) return Seq(s)
    val t = TypeDefinitionMembers.getSignatures(clazz).forName(ScalaNamesUtil.clean(name))._1.
      fastPhysicalSignatureGet(s) match {
      case Some(x) => x.supers.map {_.info}
      case None => Seq[Signature]()
    }
    t
  }

  def superSignaturesIncludingSelfType: Seq[Signature] = {
    val clazz = containingClass
    val s = new PhysicalSignature(this, ScSubstitutor.empty)
    if (clazz == null) return Seq(s)
    val withSelf = clazz.selfType.isDefined
    if (withSelf) {
      val signs = TypeDefinitionMembers.getSelfTypeSignatures(clazz).forName(ScalaNamesUtil.clean(name))._1
      signs.fastPhysicalSignatureGet(s) match {
        case Some(x) if x.info.namedElement == this => x.supers.map { _.info }
        case Some(x) => x.supers.filter {_.info.namedElement != this }.map { _.info } :+ x.info
        case None => signs.get(s) match {
          case Some(x) if x.info.namedElement == this => x.supers.map { _.info }
          case Some(x) => x.supers.filter {_.info.namedElement != this }.map { _.info } :+ x.info
          case None => Seq.empty
        }
      }
    } else {
      TypeDefinitionMembers.getSignatures(clazz).forName(ScalaNamesUtil.clean(name))._1.
        fastPhysicalSignatureGet(s) match {
        case Some(x) => x.supers.map { _.info }
        case None => Seq.empty
      }
    }
  }


  override def getNameIdentifier: PsiIdentifier = new JavaIdentifier(nameId)

  def findDeepestSuperMethod: PsiMethod = {
    val s = superMethods
    if (s.isEmpty) null
    else s.last
  }

  def getReturnTypeElement = null

  def findSuperMethods(parentClass: PsiClass) = PsiMethod.EMPTY_ARRAY

  def findSuperMethods(checkAccess: Boolean) = PsiMethod.EMPTY_ARRAY

  def findSuperMethods: Array[PsiMethod] = superMethods.toArray // TODO which other xxxSuperMethods can/should be implemented?

  def findDeepestSuperMethods = PsiMethod.EMPTY_ARRAY

  def getReturnTypeNoResolve: PsiType = PsiType.VOID

  def getPom = null

  def findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean) =
    new util.ArrayList[MethodSignatureBackedByPsiMethod]()

  def getSignature(substitutor: PsiSubstitutor): MethodSignatureBackedByPsiMethod = MethodSignatureBackedByPsiMethod.create(this, substitutor)

  //todo implement me!
  def isVarArgs = false

  def isConstructor: Boolean = name == "this"

  def getBody: PsiCodeBlock = null

  def getThrowsList = new FakePsiReferenceList(getManager, getLanguage, Role.THROWS_LIST) {
    override def getReferenceElements: Array[PsiJavaCodeReferenceElement] = {
      getReferencedTypes.map {
        tp => PsiElementFactory.SERVICE.getInstance(getProject).createReferenceElementByType(tp)
      }
    }

    override def getReferencedTypes: Array[PsiClassType] = {
      annotations("scala.throws").headOption match {
        case Some(annotation) =>
          annotation.constructor.args.map(_.exprs).getOrElse(Seq.empty).flatMap {
            _.`type`() match {
              case Right(ParameterizedType(des, Seq(arg))) => des.extractClass match {
                case Some(clazz) if clazz.qualifiedName == "java.lang.Class" =>
                  arg.toPsiType match {
                    case c: PsiClassType => Seq(c)
                    case _ => Seq.empty
                  }
                case _ => Seq.empty
              }
              case _ => Seq.empty
            }
          }.toArray
        case _ => PsiClassType.EMPTY_ARRAY
      }
    }
  }

  def `type`(): TypeResult = {
    this.returnType match {
      case Right(tp) =>
        var res: TypeResult = Right(tp)
        val paramClauses = effectiveParameterClauses
        var i = paramClauses.length - 1
        while (i >= 0) {
          res match {
            case Right(t) =>
              val parameters = paramClauses.apply(i).effectiveParameters
              val paramTypes = parameters.map(_.`type`().getOrNothing)
              res = Right(FunctionType(t, paramTypes))
            case _ =>
          }
          i = i - 1
        }
        res
      case x => x
    }
  }

  override protected def isSimilarMemberForNavigation(m: ScMember, strictCheck: Boolean): Boolean = m match {
    case f: ScFunction => f.name == name && {
      if (strictCheck) new PhysicalSignature(this, ScSubstitutor.empty).
        paramTypesEquiv(new PhysicalSignature(f, ScSubstitutor.empty))
      else true
    }
    case _ => false
  }

  def hasAssign: Boolean = getNode.getChildren(TokenSet.create(ScalaTokenTypes.tASSIGN)).nonEmpty

  def getHierarchicalMethodSignature: HierarchicalMethodSignature = {
    new HierarchicalMethodSignatureImpl(getSignature(PsiSubstitutor.EMPTY))
  }

  override def getName: String = {
    if (isConstructor) Option(getContainingClass).map(_.getName).getOrElse(super.getName)
    else super.getName
  }

  override def setName(name: String): PsiElement = {
    if (isConstructor) this
    else super.setName(name)
  }

  override def getOriginalElement: PsiElement = {
    val ccontainingClass = containingClass
    if (ccontainingClass == null) return this
    val originalClass: PsiClass = ccontainingClass.getOriginalElement.asInstanceOf[PsiClass]
    if (ccontainingClass eq originalClass) return this
    if (!originalClass.isInstanceOf[ScTypeDefinition]) return this
    val c = originalClass.asInstanceOf[ScTypeDefinition]
    val membersIterator = c.members.iterator
    val buf: ArrayBuffer[ScMember] = new ArrayBuffer[ScMember]
    while (membersIterator.hasNext) {
      val member = membersIterator.next()
      if (isSimilarMemberForNavigation(member, strictCheck = false)) buf += member
    }
    if (buf.isEmpty) this
    else if (buf.length == 1) buf(0)
    else {
      val filter = buf.filter(isSimilarMemberForNavigation(_, strictCheck = true))
      if (filter.isEmpty) buf(0)
      else filter(0)
    }
  }

  private def collectReverseParamTypesNoImplicits: Option[Seq[Seq[ScType]]] = {
    val buffer = ArrayBuffer.empty[Seq[ScType]]
    val clauses = paramClauses.clauses

    //for performance
    var idx = clauses.length - 1
    while (idx >= 0) {
      val cl = clauses(idx)
      if (!cl.isImplicit) {
        val parameters = cl.parameters
        val paramTypes = parameters.flatMap(_.`type`().toOption)

        if (paramTypes.size != parameters.size) return None
        else buffer += paramTypes
      }
      idx -= 1
    }
    Some(buffer)
  }
}

object ScFunction {
  implicit class Ext(val function: ScFunction) extends AnyVal {

    import Ext._

    private implicit def project = function.getProject
    private implicit def resolveScope = function.resolveScope
    private implicit def elementScope = function.elementScope

    def isApplyMethod: Boolean = function.name == Apply

    def isUnapplyMethod: Boolean = Unapplies(function.name)

    /** Is this function sometimes invoked without it's name appearing at the call site? */
    def isSpecial: Boolean = Special(function.name)

    def returnType: TypeResult = {
      if (importantOrderFunction(function)) {
        val parent = function.getParent
        val isCalculating = isCalculatingFor(parent)

        if (isCalculating.get()) function.returnTypeInner
        else {
          isCalculating.set(true)
          try {
            val children = parent.stubOrPsiChildren(FUNCTION_DEFINITION, ScFunctionFactory).iterator

            while (children.hasNext) {
              val nextFun = children.next()
              if (importantOrderFunction(nextFun)) {
                ProgressManager.checkCanceled()
                nextFun.returnTypeInner
              }
            }
            function.returnTypeInner
          }
          finally {
            isCalculating.set(false)
          }
        }
      } else function.returnTypeInner
    }

    def functionTypeNoImplicits(forcedReturnType: Option[ScType] = None): Option[ScType] = {
      val retType = forcedReturnType match { //avoid getOrElse for reduced stacktrace
        case None => returnType.toOption
        case some => some
      }
      function.collectReverseParamTypesNoImplicits match {
        case Some(params) =>
          retType.map(params.foldLeft(_)((res, params) => FunctionType(res, params)))
        case None => None
      }
    }

    private def importantOrderFunction(fun: ScFunction) = fun match {
      case funDef: ScFunctionDefinition => funDef.hasModifierProperty("implicit") && !funDef.hasExplicitType
      case _ => false
    }
  }

  object Ext {
    val Apply = "apply"
    val Update = "update"
    val GetSet = Set(Apply, Update)

    val Unapply = "unapply"
    val UnapplySeq = "unapplySeq"
    val Unapplies = Set(Unapply, UnapplySeq)

    val Foreach = "foreach"
    val Map = "map"
    val FlatMap = "flatMap"
    val Filter = "filter"
    val WithFilter = "withFilter"
    val ForComprehensions: Set[String] = Set(Foreach, Map, FlatMap, Filter, WithFilter)

    private val Special: Set[String] = GetSet ++ Unapplies ++ ForComprehensions
  }

  private val calculatingBlockKey: Key[ThreadLocal[Boolean]] = Key.create("calculating.function.returns.block")

  private def isCalculatingFor(e: PsiElement) = e.getOrUpdateUserData(ScFunction.calculatingBlockKey, new ThreadLocal[Boolean] {
    override def initialValue(): Boolean = false
  })

  @tailrec
  def getCompoundCopy(pTypes: Seq[Seq[ScType]], tParams: List[TypeParameter], rt: ScType, fun: ScFunction): ScFunction = {
    fun match {
      case light: ScLightFunctionDeclaration => getCompoundCopy(pTypes, tParams, rt, light.fun)
      case light: ScLightFunctionDefinition  => getCompoundCopy(pTypes, tParams, rt, light.fun)
      case decl: ScFunctionDeclaration       => new ScLightFunctionDeclaration(pTypes, tParams, rt, decl)
      case definition: ScFunctionDefinition  => new ScLightFunctionDefinition(pTypes, tParams, rt, definition)
    }
  }
}
