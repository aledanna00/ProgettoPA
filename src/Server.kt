import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import Mapping
import Path
import Param

data class Route(
    val fullPath: String,
    val method: KFunction<*>,
    val controllerInstance: Any,
    val pathParams: List<String>,
    val queryParams: List<Pair<String, KParameter>>
)

class GetJson(vararg controllers: KClass<*>) {
    private val routes = mutableListOf<Route>()
    init {
        controllers.forEach { scanController(it) }
    }
    fun start(port: Int) {
        val server = HttpServer.create(InetSocketAddress(port), 0)
        server.createContext("/") { exchange ->
            if (exchange.requestMethod != "GET") {
                exchange.sendResponseHeaders(405, 0)
                return@createContext
            }
            val path = exchange.requestURI.path
            val query = exchange.requestURI.query ?: ""
            val response = handleRequest(path, query)
            exchange.sendResponseHeaders(200, response.toByteArray().size.toLong())
            exchange.responseBody.use { it.write(response.toByteArray()) }
        }
        server.executor = null
        server.start()
    }
    private fun scanController(controller: KClass<*>) { /* Reflectively map paths to methods */ }
    private fun handleRequest(path: String, query: String): String { /* Routing logic */ }
}

@Mapping("PA2025")
class Controller {


}

fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
}
