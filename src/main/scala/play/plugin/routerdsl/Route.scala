package play.plugin.routerdsl

object RouteVerbType extends Enumeration {
    type RouteVerb = Value
    val GET, POST = Value
}

class RouteVerbWithPath(verb: RouteVerbType.RouteVerb, path: String, params: Map[String,Any]) {

}

class RouteVerb(verb: RouteVerbType.RouteVerb) {
    def ->(path: String) = new RouteVerbWithPath(this.verb, path, Map())
}

object Route {
    def GET = new RouteVerb(RouteVerbType.GET)
}