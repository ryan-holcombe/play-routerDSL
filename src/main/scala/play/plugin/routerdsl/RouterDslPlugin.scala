package play.plugin.routerdsl

import play.api._

class RouterDslPlugin(app: Application) extends Plugin {
    override def onStart() {

    }

    override def onStop() {

    }

    override def enabled = app.configuration.getString("routerdsl-plugin").filter(_ == "disabled").isEmpty
}
