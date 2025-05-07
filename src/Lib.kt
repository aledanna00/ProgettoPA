import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * A Kotlin library for building and manipulating in-memory JSON structures.
 *
 * Supports:
 * - JSON serialization via `toJsonString()` and `serialize()`
 * - Filtering and mapping on `JsonObject` and `JsonArray`
 * - Recursive operations using the Visitor pattern
 *
 * Includes visitors for validating JSON object structure and checking array type homogeneity.
 */

/**
 * Visitor interface for traversing JSON values.
 * @param R The return type for the visitor's operations.
 */
interface JsonVisitor {
    fun visitObject(obj: JsonObject): Boolean
    fun visitArray(array: JsonArray): Boolean
    fun visitString(string: JsonString): Boolean
    fun visitNumber(number: JsonNumber): Boolean
    fun visitBoolean(boolean: JsonBoolean): Boolean
    fun visitNull(nullValue: JsonNull): Boolean
}


/**
 * Base class for all JSON value types.
 */
abstract class JsonValue {
    /**
     * Converts the JSON value to a valid JSON string.
     */
    abstract fun toJsonString(): String
    /**
     * Accepts a visitor to process the JSON value.
     * @param visitor A JsonVisitor instance.
     */
    abstract fun accept(visitor: JsonVisitor): Boolean
}

/**
 * Represents a JSON string.
 * @param value The string value.
 */
data class JsonString(val value: String) : JsonValue() {
    override fun toJsonString(): String = "\"$value\""
    override fun accept(visitor: JsonVisitor): Boolean= visitor.visitString(this)
}

/**
 * Represents a JSON number.
 * @param value The numeric value.
 */
data class JsonNumber(val value: Double) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun accept(visitor: JsonVisitor): Boolean = visitor.visitNumber(this)
}

/**
 * Represents a JSON boolean.
 * @param value The boolean value.
 */
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun  accept(visitor: JsonVisitor): Boolean= visitor.visitBoolean(this)
}

/**
 * Represents the JSON null value.
 */
object JsonNull : JsonValue() {
    override fun toJsonString(): String = "null"
    override fun accept(visitor: JsonVisitor): Boolean = visitor.visitNull(this)
}

/**
 * Represents a JSON array.
 * @param elements The list of JSON values.
 */
data class JsonArray(val elements: List<JsonValue>) : JsonValue() {
    /**
     * Overrides the function "toJsonString".
     */
    override fun toJsonString(): String =
        elements.joinToString(separator=",",prefix = "[",postfix ="]") { it.toJsonString() }
    /**
     * Overrides the function "accept".
     */
    override fun accept(visitor: JsonVisitor): Boolean = visitor.visitArray(this)
    /**
     * Applies a transformation to each element in the array.
     * @param operation Function to transform each JsonValue.
     */
    fun mapList(operation: (JsonValue) -> JsonValue): JsonArray {
        val mappedElements = elements.map { operation(it) }
        return JsonArray(mappedElements)
    }
}

/**
 * Represents a JSON object.
 * @param properties A map of key-value pairs.
 */
data class JsonObject(val properties: LinkedHashMap<String, JsonValue>) : JsonValue() {
    /**
     * Overrides the function "toJsonString".
     */
    override fun toJsonString(): String =
        properties.entries.joinToString(separator=",",prefix = "{", postfix = "}") {
            "\"${it.key}\":${it.value.toJsonString()}"
        }
    /**
     * Overrides the function "accept".
     */
    override fun accept(visitor: JsonVisitor): Boolean = visitor.visitObject(this)
    /**
     * Filters properties by value type.
     * @param condition Type name to filter by.
     */
    fun filterJsonByType(condition: String):JsonObject{
        val filteredProperties= properties.filter { entry ->
            when (condition){
                "string"-> entry.value is JsonString
                "number"-> entry.value is JsonNumber
                "boolean"-> entry.value is JsonBoolean
                "array"-> entry.value is JsonArray
                "null"-> entry.value is JsonNull
                else-> false
            }
        }
        return  JsonObject(java.util.LinkedHashMap(filteredProperties))
    }
    /**
     * Filters properties by key.
     * @param keys Keys to retain.
     */
    fun filterPropertiesByKey(keys: List<String>): JsonObject {
        val filteredMap = properties.filterKeys { it in keys }
        return JsonObject(java.util.LinkedHashMap(filteredMap))
    }
}
/**
 * Infers a JSON model from a supported Kotlin object using reflection.
 *
 * Supports: null, Int, Double, Boolean, String, Enums, Lists, Maps (with String keys), and data classes.
 *
 * @param any the Kotlin object to convert to a JsonValue
 * @return a corresponding JsonValue representation
 * @throws IllegalArgumentException if the object type is not supported
 */
fun inferJson(any: Any?): JsonValue {
        val inference: JsonValue = when (any) {
            null -> JsonNull
            is String -> JsonString(any)
            is Int, is Double -> JsonNumber((any as Number).toDouble())
            is Boolean -> JsonBoolean(any)
            is Enum<*> -> JsonString(any.name)
            is List<*> -> JsonArray(any.map { inferJson(it) })
            is Map<*, *> -> {
                val mapEntries = LinkedHashMap<String, JsonValue>()
                any.entries.forEach { (k, v) ->
                    if (k !is String) {
                        throw IllegalArgumentException("Only maps with String keys are supported.")
                    }
                    mapEntries[k] = inferJson(v)
                }
                JsonObject(mapEntries)
            }
            else -> {
                val kClass = any::class
                if (kClass.isData) {
                    val props = LinkedHashMap<String, JsonValue>()
                    val constructor = kClass.primaryConstructor
                        ?: throw IllegalArgumentException("Data class must have a primary constructor")
                    constructor.parameters.forEach { param ->
                        val name = param.name ?: return@forEach
                        val value = kClass.memberProperties
                            .first { it.name == name }
                            .apply { isAccessible = true }
                            .getter.call(any)
                        props[name] = inferJson(value)
                    }
                    return JsonObject(props)
                } else {
                    throw IllegalArgumentException("Unsupported type: ${any::class.simpleName}")
                }
            }
        }
        return inference
}


/**
 * Visitor to validate that:
 * - JSON objects have unique keys.
 * - Values do not contain JsonNull.
 */
class JsonObjectValidationVisitor : JsonVisitor{
    override fun visitObject(obj: JsonObject): Boolean {
        val keys = obj.properties.keys
        if (keys.size != keys.toSet().size) return false
        return obj.properties.values.all { it.accept(this) }
    }
    override fun visitArray(array: JsonArray): Boolean {
        return array.elements.all { it.accept(this) }
    }
    override fun visitString(string: JsonString): Boolean = true
    override fun visitNumber(number: JsonNumber): Boolean = true
    override fun visitBoolean(boolean: JsonBoolean): Boolean = true
    override fun visitNull(nullValue: JsonNull): Boolean = false
}

/**
 * Visitor to check if all elements of each JsonArray are of the same type.
 */
class JsonArrayHomogeneityVisitor : JsonVisitor{
    override fun visitArray(array: JsonArray): Boolean {
        val nonNullElements = array.elements.filter { it !is JsonNull }
        if (nonNullElements.isEmpty()) return true
        val firstType = nonNullElements.first()::class
        return nonNullElements.all { it::class == firstType && it.accept(this) }
    }
    override fun visitObject(obj: JsonObject): Boolean {
        return obj.properties.values.all { it.accept(this) }
    }
    override fun visitString(string: JsonString): Boolean = true
    override fun visitNumber(number: JsonNumber): Boolean = true
    override fun visitBoolean(boolean: JsonBoolean): Boolean = true
    override fun visitNull(nullValue: JsonNull): Boolean = true
}

