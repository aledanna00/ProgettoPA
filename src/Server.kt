/**
 * Commento
 */

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import Mapping
import Path
import Param

/**
 * Commento
 */
data class Route(
    val fullPath: String,
    val method: KFunction<*>,
    val controllerInstance: Any,
    val pathParams: List<String>,
    val queryParams: List<Pair<String, KParameter>>
)

/**
 * Commento
 */
class GetJson(vararg controller: KClass<*>) {
    private val routes = mutableListOf<Route>()
    init {
        controller.forEach { scanController(it) }
    }
    /**
     * Commento
     */
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
    /**
     * Commento
     */
    private fun scanController(controller: KClass<*>) {
        val basePathMapping = controller.annotations.filterIsInstance<Mapping>().firstOrNull()?.path ?: ""
        val instance = controller.constructors.first { it.parameters.isEmpty() }.call()
        for (func in controller.members.filterIsInstance<KFunction<*>>()) {
            val mapping = func.annotations.filterIsInstance<Mapping>().firstOrNull() ?: continue
            val fullPath = ("/$basePathMapping/${mapping.path}").replace("//", "/")
            val pathParams = mutableListOf<String>()
            val queryParams = mutableListOf<Pair<String, KParameter>>()
            for (param in func.parameters.filter { it.kind == KParameter.Kind.VALUE }) {
                when {
                    param.annotations.any { it is Path } -> {
                        pathParams += param.name!!
                    }
                    param.annotations.any { it is Param } -> {
                        queryParams += param.name!! to param
                    }
                }
            }
            routes += Route(
                fullPath = fullPath,
                method = func,
                controllerInstance = instance,
                pathParams = pathParams,
                queryParams = queryParams
            )
        }/* Reflectively map paths to methods */
    }
    /**
     * Commento
     */
    private fun handleRequest(path: String, query: String): String {
        val queryMap = parseQuery(query)
        for (route in routes) {
            val pathPattern = route.fullPath.replace(Regex("\\{[^/]+}")) { "([^/]+)" }
            val match = Regex("^$pathPattern$").matchEntire(path) ?: continue
            val pathArgs = match.groupValues.drop(1) // skip group 0
            val args = mutableMapOf<KParameter, Any?>()
            // Inserisci parametri da path
            for ((i, name) in route.pathParams.withIndex()) {
                val param = route.method.parameters.first { it.name == name }
                args[param] = pathArgs[i]
            }
            // Inserisci parametri da query string
            for ((name, param) in route.queryParams) {
                val value = queryMap[name] ?: return """{"error":"Missing query param: $name"}"""
                args[param] = when (param.type.classifier) {
                    Int::class -> value.toInt()
                    String::class -> value
                    else -> return """{"error":"Unsupported param type for: $name"}"""
                }
            }
            // Chiama il metodo e serializza il risultato
            val result = route.method.callBy(
                mapOf(route.method.parameters.first() to route.controllerInstance) + args
            )
            return JsonObject.inferJson(result).toJsonString()
        }
        return """{"error":"Not found"}"""/* Routing logic */
    }
    /**
     * Commento
     */
    private fun parseQuery(query: String): Map<String, String> {
        return query.split("&")
            .filter { it.contains("=") }.associate {
                val (k, v) = it.split("=", limit = 2)
                k to v
            }
    }
}

@Mapping("PA2025")
class Controller {
    @Mapping("ints")
    fun demo(): List<Int> = listOf(1, 2, 3)

    @Mapping("pair")
    fun obj(): Pair<String, String> = Pair("um", "dois")

    @Mapping("path/{pathvar}")
    fun path(
        @Path pathvar: String
    ): String = pathvar + "!"

    @Mapping("args")
    fun args(
        @Param n: Int,
        @Param text: String
    ): Map<String, String> = mapOf(text to text.repeat(n))
}

fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
}
