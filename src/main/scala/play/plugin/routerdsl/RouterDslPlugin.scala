package play.plugin.routerdsl

import play.api._

class RouterDslPlugin(app: Application) extends Plugin {
    override def onStart() {
        app.routes
    }

    override def onStop() {

    }

    override def enabled = app.configuration.getString("routerdsl-plugin").filter(_ == "disabled").isEmpty
}
