import org.junit.Test
import play.plugin.routerdsl.Route
import Route._

class RouteTesRt {

    @Test
    def testDsl() {
        GET -> "/index/<id>"
    }
}
