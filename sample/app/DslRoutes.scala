import play.core.Router

object DslRoutes extends Router.Routes with PlayNavigator
{
    val application = new controllers.Application

    // Basic
    GET  on root       to (() => application.index)
    GET  on "index"    to (() => application.index)
    GET  on "about"    to (() => application.about)
    POST on "foo"      to (() => application.about)
    GET  on "foo" / *  to ((id:Int) => application.user(id))
  
    // Namespace ...
    namespace("api")
    {
      namespace("v1")
      {
        GET on "index" to (() => application.index)
      }
    }

    // ... or with reverse routing support
    val api = new Namespace("api")
    {
      val v2 = new Namespace("v2")
      {
        val about = GET on "about" to (() => application.about)
      }
    }
}