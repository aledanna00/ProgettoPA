/**
 * Commento
 */

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter

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
            return inferJson(result).toJsonString()
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
    @Mapping("array")
    fun array(): String {
        val sentence = listOf("hello", "world", "!")
        val jsonArray = toJsonArray(sentence)
        return jsonArray.toJsonString()
    }

    @Mapping("obj")
    fun obj(): JsonObject{
        val person= linkedMapOf(
            "name" to JsonString("Emily"),
            "age" to JsonNumber(22.0),
            "isStudent" to JsonBoolean(true)
        )
        return JsonObject(person)
    }

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
            return "Available names are: 'Giorgio' and 'Alessia'"
        }
    }
}

fun main() {
    val app = GetJson(Controller::class)
    app.start(8080)
    println("Server avviato su http://localhost:")
}
