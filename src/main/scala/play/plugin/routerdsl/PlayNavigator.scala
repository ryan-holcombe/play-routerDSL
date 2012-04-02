package play.plugin.routerdsl

import play.api.mvc._
import play.core.Router

trait Navigator[Out] {
    val navigatorRoutes = new collection.mutable.ListBuffer[Route]

    def addRoute[T <: Route](route: T): T = {
        navigatorRoutes += route
        route
    }

    type In = Array[String]

    sealed trait PathElem

    case class Static(name: String) extends PathElem {
        override def toString = name
    }

    case object * extends PathElem

    case object ** extends PathElem

    sealed trait Method {
        def on[T](path: RoutePath[T]): T = path.withMethod(this)

        def on(name: String) = RoutePath0(this, Static(name) :: Nil)
    }

    val root = RoutePath0(ANY, Nil)

    case object ANY extends Method

    // http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
    case object OPTIONS extends Method

    case object GET extends Method

    case object HEAD extends Method

    case object POST extends Method

    case object PUT extends Method

    case object DELETE extends Method

    case object TRACE extends Method

    case object CONNECT extends Method


    trait BasicRoutePath {
        def parts: List[PathElem]

        def method: Method

        def ext: Option[String]

        def variableIndices = parts.zipWithIndex.collect {
            case (e, i) if e == * => i
        }

        def length = parts.length

        override def toString = method.toString + "\t/" + parts.mkString("/") + extString

        def extString = ext.map {
            "." + _
        } getOrElse ""
    }

    sealed trait RoutePath[Self] extends BasicRoutePath {
        def withMethod(method: Method): Self
    }

    case class RoutePath0(method: Method, parts: List[PathElem], ext: Option[String] = None) extends RoutePath[RoutePath0] {
        def /(static: Static) = RoutePath0(method, parts :+ static)

        def /(p: PathElem) = RoutePath1(method, parts :+ p)

        def to(f0: () => Out) = addRoute(Route0(this.copy(parts = currentNamespace ++ parts), f0))

        def withMethod(method: Method) = RoutePath0(method, parts)

        def as(ext: String) = RoutePath0(method, parts, Some(ext))
    }

    case class RoutePath1(method: Method, parts: List[PathElem], ext: Option[String] = None) extends RoutePath[RoutePath1] {
        def /(static: Static) = RoutePath1(method, parts :+ static)

        def /(p: PathElem) = RoutePath2(method, parts :+ p)

        def to[A: ParamMatcher : Manifest](f1: A => Out) = addRoute(Route1(this.copy(parts = currentNamespace ++ parts), f1))

        def withMethod(method: Method) = RoutePath1(method, parts)

        def as(ext: String) = RoutePath1(method, parts, Some(ext))
    }

    case class RoutePath2(method: Method, parts: List[PathElem], ext: Option[String] = None) extends RoutePath[RoutePath2] {
        def /(static: Static) = RoutePath2(method, parts :+ static)

        def /(p: *.type) = RoutePath3(method, parts :+ p)

        def to[A: ParamMatcher : Manifest, B: ParamMatcher : Manifest](f2: (A, B) => Out) = addRoute(Route2(this.copy(parts = currentNamespace ++ parts), f2))

        def withMethod(method: Method) = RoutePath2(method, parts)

        def as(ext: String) = RoutePath2(method, parts, Some(ext))
    }

    case class RoutePath3(method: Method, parts: List[PathElem], ext: Option[String] = None) extends RoutePath[RoutePath3] {
        def /(static: Static) = RoutePath3(method, parts :+ static)

        // def /(p: *.type) = RoutePath3(method, parts :+ p)
        def to[A: ParamMatcher : Manifest, B: ParamMatcher : Manifest, C: ParamMatcher : Manifest](f3: (A, B, C) => Out) = addRoute(Route3(this.copy(parts = currentNamespace ++ parts), f3))

        def withMethod(method: Method) = RoutePath3(method, parts)

        def as(ext: String) = RoutePath3(method, parts, Some(ext))
    }

    implicit def stringToRoutePath0(name: String) = RoutePath0(ANY, Static(name) :: Nil)

    implicit def asterixToRoutePath1(ast: *.type) = RoutePath1(ANY, ast :: Nil)

    implicit def stringToStatic(name: String) = Static(name)

    trait ParamMatcher[T] {
        def unapply(s: String): Option[T]
    }

    def silent[T](f: => T) = try {
        Some(f)
    } catch {
        case _ => None
    }

    implicit val IntParamMatcher = new ParamMatcher[Int] {
        def unapply(s: String) = silent(s.toInt)
    }
    implicit val LongParamMatcher = new ParamMatcher[Long] {
        def unapply(s: String) = silent(s.toLong)
    }
    implicit val FloatParamMatcher = new ParamMatcher[Float] {
        def unapply(s: String) = silent(s.toFloat)
    }
    implicit val DoubleParamMatcher = new ParamMatcher[Double] {
        def unapply(s: String) = silent(s.toDouble)
    }
    implicit val StringParamMatcher = new ParamMatcher[String] {
        def unapply(s: String) = Some(s)
    }
    implicit val BooleanParamMatcher = new ParamMatcher[Boolean] {
        def unapply(s: String) = s match {
            case "1" | "true" => Some(true)
            case "0" | "false" => Some(false)
            case _ => None
        }
    }

    object Resolver {
        def resolvePath0(parts: List[PathElem], in: In, fun: () => Out): Option[() => Out] = {
            if (in.length == parts.length && parts.zipWithIndex.forall {
                case (elem, i) => elem match {
                    case * | ** => true
                    case Static(name) => name == in(i)
                }
            }) Some(fun)
            else None
        }

        def resolvePath1[A: ParamMatcher](parts: List[PathElem], in: In, fun: A => Out): Option[() => Out] = {
            val pm1 = implicitly[ParamMatcher[A]]
            (in.headOption, parts) match {
                case (Some(first), Static(name) :: rest) if name == first => resolvePath1(rest, in.drop(1), fun)
                case (Some(pm1(a)), * :: rest) => resolvePath0(rest, in.drop(1), () => fun(a))
                case (Some(first), ** :: rest) => join(in) match {
                    case pm1(a) => resolvePath0(Nil, Array(), () => fun(a))
                    case _ => None
                }
                case _ => None
            }
        }

        def resolvePath2[A: ParamMatcher, B: ParamMatcher](parts: List[PathElem], in: In, fun: (A, B) => Out): Option[() => Out] = {
            val pm1 = implicitly[ParamMatcher[A]]
            (in.headOption, parts) match {
                case (Some(first), Static(name) :: rest) if name == first => resolvePath2(rest, in.drop(1), fun)
                case (Some(pm1(a)), * :: rest) => resolvePath1(rest, in.drop(1), (b: B) => fun(a, b))
                case (Some(first), ** :: rest) => join(in) match {
                    case pm1(a) => resolvePath1(Nil, Array(), (b: B) => fun(a, b))
                    case _ => None
                }
                case _ => None
            }
        }

        def resolvePath3[A: ParamMatcher, B: ParamMatcher, C: ParamMatcher](parts: List[PathElem], in: In, fun: (A, B, C) => Out): Option[() => Out] = {
            val pm1 = implicitly[ParamMatcher[A]]
            (in.headOption, parts) match {
                case (Some(first), Static(name) :: rest) if name == first => resolvePath3(rest, in.drop(1), fun)
                case (Some(pm1(a)), * :: rest) => resolvePath2(rest, in.drop(1), (b: B, c: C) => fun(a, b, c))
                case (Some(first), ** :: rest) => join(in) match {
                    case pm1(a) => resolvePath2(Nil, Array(), (b: B, c: C) => fun(a, b, c))
                    case _ => None
                }
                case _ => None
            }
        }

        def join(in: In) = in.mkString("/")
    }

    sealed trait Route {
        def path: BasicRoutePath

        def matches(method: String, parts: Array[String]): Option[() => Out] = {
            if (path.method.toString == method) {
                path.ext map {
                    ext =>
                        for {
                            last <- parts.lastOption
                            lastSplitted = last.split("\\.")
                            urlext <- lastSplitted.lastOption
                            if ext == urlext
                            matched <- matchPath(parts.dropRight(1) :+ lastSplitted.dropRight(1).mkString("."))
                        } yield matched
                } getOrElse matchPath(parts)
            } else {
                None
            }
        }

        def args: List[scala.reflect.Manifest[_]]

        def matchPath(in: In): Option[() => Out]

        def createPath(args: List[String]) = (("", args) /: path.parts) {
            case ((res, x :: xs), *) => (res + "/" + x, xs)
            case ((res, xs), e) => (res + "/" + e.toString, xs)
        }._1
    }

    case class Route0(path: RoutePath0, fun: () => Out) extends Route {
        def apply() = createPath(Nil)

        def matchPath(in: In) = Resolver.resolvePath0(path.parts, in, fun)

        def args = Nil
    }

    case class Route1[A: ParamMatcher : Manifest](path: RoutePath1, fun: A => Out) extends Route {
        def apply(a: A) = createPath(a.toString :: Nil)

        def matchPath(in: In) = Resolver.resolvePath1(path.parts, in, fun)

        def args = manifest[A] :: Nil
    }

    case class Route2[A: ParamMatcher : Manifest, B: ParamMatcher : Manifest](path: RoutePath2, fun: (A, B) => Out) extends Route {
        def apply(a: A, b: B) = createPath(a.toString :: b.toString :: Nil)

        def matchPath(in: In) = Resolver.resolvePath2(path.parts, in, fun)

        def args = manifest[A] :: manifest[B] :: Nil
    }

    case class Route3[A: ParamMatcher : Manifest, B: ParamMatcher : Manifest, C: ParamMatcher : Manifest](path: RoutePath3, fun: (A, B, C) => Out) extends Route {
        def apply(a: A, b: B, c: C) = createPath(a.toString :: b.toString :: c.toString :: Nil)

        def matchPath(in: In) = Resolver.resolvePath3(path.parts, in, fun)

        def args = List(manifest[A], manifest[B], manifest[C])
    }

    lazy val _documentation = navigatorRoutes.map {
        route =>

            val (parts, _) = ((List[String](), route.args) /: route.path.parts) {
                case ((res, x :: xs), *) => (res :+ ("[" + x + "]"), xs)
                case ((res, xs), e) => (res :+ e.toString, xs)
            }

            (route.path.method.toString, parts.mkString("/", "/", "") + route.path.extString, route.args.mkString("(", ", ", ")"))
    }

    trait ResourcesRouting[T] {
        val index: Route0
        val `new`: Route0
        val create: Route0
        val show: Route1[T]
        val edit: Route1[T]
        val update: Route1[T]
        val delete: Route1[T]
    }

    // namespace
    protected val namespaceStack = new collection.mutable.Stack[Static]

    def currentNamespace = namespaceStack.toList.reverse

    def namespace(path: Static)(f: => Unit) = {
        namespaceStack push path
        f
        namespaceStack.pop
    }

    class Namespace(path: Static) extends DelayedInit {
        def delayedInit(body: => Unit) = namespace(path)(body)
    }


    // resources
    def resources[T: ParamMatcher : Manifest](name: String, controller: Resources[T, Out]) = new ResourcesRouting[T] {
        val index = GET on name to controller.index _
        val `new` = GET on name / "new" to controller.`new` _
        val create = POST on name to controller.create _
        val show = GET on name / * to controller.show _
        val edit = GET on name / * / "edit" to controller.edit _
        val update = PUT on name / * to controller.update _
        val delete = DELETE on name / * to controller.delete _
    }
}

trait Resources[T, Out] {
    def index: Out

    def `new`: Out

    def create: Out

    def show(id: T): Out

    def edit(id: T): Out

    def update(id: T): Out

    def delete(id: T): Out
}


trait PlayResources[T] extends Resources[T, Handler]

trait PlayNavigator extends Navigator[Handler] {

    def documentation = _documentation

    def routes = new PartialFunction[RequestHeader, Handler] {
        private var _lastHandler: () => Handler = null // this one sucks a lot

        def isDefinedAt(req: RequestHeader) = {
            // documentation foreach println

            val parts = req.path.split("/").dropWhile(_ == "")
            navigatorRoutes.view.map(_.matches(req.method, parts)).collectFirst {
                case Some(e) => e
            } match {
                case Some(handler) =>
                    _lastHandler = handler
                    true
                case None =>
                    false
            }
        }

        def apply(req: RequestHeader) = _lastHandler()
    }
}

trait DslRoutes extends Router.Routes with PlayNavigator
