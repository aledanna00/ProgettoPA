import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

/**
 * Represents a single route mapping between a URL path and a controller function.
 *
 * @property fullPath The full URL path pattern (e.g., "/users/{id}").
 * @property method The function to be invoked when the route is matched.
 * @property controllerInstance The instance of the controller class containing the method.
 * @property pathParams The names of path variables (e.g., `{id}`).
 * @property queryParams List of query parameter names and their corresponding KParameter.
 */
data class Route(
    val fullPath: String,
    val method: KFunction<*>,
    val controllerInstance: Any,
    val pathParams: List<String>,
    val queryParams: List<Pair<String, KParameter>>
)

/**
 * Simple HTTP GET server for routing controller methods.
 *
 * @constructor Initializes the server by scanning the provided controller classes for annotated methods.
 * @param controller Vararg of controller classes to be scanned for routes.
 */
class GetJson(vararg controller: KClass<*>) {
    private val routes = mutableListOf<Route>()
    init {
        controller.forEach { scanController(it) }
    }
    /**
     * Starts the HTTP server on the given port.
     *
     * @param port The port on which the server will listen.
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
     * Scans the given controller class for annotated methods and maps them to routes.
     *
     * @param controller The controller class to be scanned.
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
        }
    }
    /**
     * Handles incoming HTTP GET requests, matches routes, extracts parameters,
     * invokes the corresponding controller method, and returns the result as JSON.
     *
     * @param path The request URI path.
     * @param query The query string from the request URI.
     * @return The response string in JSON format.
     */
    private fun handleRequest(path: String, query: String): String {
        val queryMap = parseQuery(query)
        for (route in routes) {
            val pathPattern = route.fullPath.replace(Regex("\\{[^/]+}")) { "([^/]+)" }
            val match = Regex("^$pathPattern$").matchEntire(path) ?: continue
            val pathArgs = match.groupValues.drop(1) // skip group 0
            val args = mutableMapOf<KParameter, Any?>()
            for ((i, name) in route.pathParams.withIndex()) {
                val param = route.method.parameters.first { it.name == name }
                args[param] = pathArgs[i]
            }
            for ((name, param) in route.queryParams) {
                val value = queryMap[name] ?: return """{"error":"Missing query param: $name"}"""
                args[param] = when (param.type.classifier) {
                    Int::class -> value.toInt()
                    String::class -> value
                    else -> return """{"error":"Unsupported param type for: $name"}"""
                }
            }
            val result = route.method.callBy(
                mapOf(route.method.parameters.first() to route.controllerInstance) + args
            )
            return inferJson(result).toJsonString()
        }
        return """{"error":"Not found"}"""
    }
    /**
     * Parses a query string into a map of key-value pairs.
     *
     * @param query The query string (e.g., "name=John&age=30").
     * @return A map containing query parameters and their values.
     */
    private fun parseQuery(query: String): Map<String, String> {
        return query.split("&")
            .filter { it.contains("=") }.associate {
                val (k, v) = it.split("=", limit = 2)
                k to v
            }
    }
}

/**
 * Controller annotated with @Mapping and methods to serve different JSON responses.
 */
@Mapping("PA2025")
class Controller {
    /**
     * Returns a string parsed from a JSON array of strings.
     */
    @Mapping("array")
    fun array(): String {
        val sentence = listOf("hello", "world", "!")
        val jsonArray = toJsonArray(sentence)
        return jsonArray.toJsonString()
    }
    /**
     * Returns a string parsed from a JSON object representing a person.
     */
    @Mapping("obj")
    fun obj(): String{
        val person= linkedMapOf(
            "name" to JsonString("Emily"),
            "age" to JsonNumber(22.0),
            "isStudent" to JsonBoolean(true)
        )
        return JsonObject(person).toJsonString()
    }
    /**
     * Returns a string parsed from JSON content, different based on the path variable.
     *
     * @param pathvar A string path parameter used to determine the output.
     */
    @Mapping("path/{pathvar}")
    fun path(
        @Path pathvar: String
    ): String {
        if(pathvar=="obj"){
            val person= linkedMapOf(
                "name" to JsonString("Mark"),
                "age" to JsonNumber(19.0),
                "isStudent" to JsonBoolean(true)
            )
            return JsonObject(person).toJsonString()
        } else if (pathvar== "array"){
            val sentence= listOf("alternative", "array", "!").map { JsonString(it) }
            return JsonArray(sentence).toJsonString()
        } else{
            return pathvar + " if you put 'obj' or 'array' you'll get an alternative output!"
        }
    }
    /**
     * Returns, based on query parameters, a string parsed from a family member's JSON object.
     *
     * @param name The name of the child ("Mark" or "Emily").
     * @param family The family member requested ("mom" or "dad").
     */
    @Mapping("args")
    fun args(
        @Param name: String,
        @Param family: String
    ): String{
        if(name=="Mark"){
            if (family== "mom"){
                val person= linkedMapOf(
                    "name" to JsonString("Caroline"),
                    "age" to JsonNumber(46.0),
                    "isStudent" to JsonBoolean(false)
                )
                return JsonObject(person).toJsonString()
            }else if(family=="dad"){
                val person= linkedMapOf(
                    "name" to JsonString("Matt"),
                    "age" to JsonNumber(50.0),
                    "isStudent" to JsonBoolean(false)
                )
                return JsonObject(person).toJsonString()
            }else {
                return "Available family members are: 'mom' and 'dad'"
            }
        }else if(name== "Emily"){
            if (family== "mom"){
                val person= linkedMapOf(
                    "name" to JsonString("Madeline"),
                    "age" to JsonNumber(52.0),
                    "isStudent" to JsonBoolean(false)
                )
                return JsonObject(person).toJsonString()
            }else if(family=="dad"){
                val person= linkedMapOf(
                    "name" to JsonString("John"),
                    "age" to JsonNumber(55.0),
                    "isStudent" to JsonBoolean(false)
                )
                return JsonObject(person).toJsonString()
            }else {
                return "Available family members are: 'mom' and 'dad'"
            }
        }else{
            return "Available names are: 'Mark' and 'Emily'"
        }
    }
}

/**
 * Entry point for the server application.
 */
fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
    println("Server running on http://localhost:8080")
}
