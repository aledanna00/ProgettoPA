/**
 * A Kotlin library for building and manipulating in-memory Json structures.
 *
 * Supports:
 * - JSON serialization via `toJsonString()` and `serialize()`
 * - Filtering and mapping on `JsonObject` and `JsonArray`
 * - Recursive operations using the Visitor pattern
 *
 * Includes visitors for validating JSON object structure and checking array type homogeneity.
 */

/**
 * Visitor interface for traversing Json values.
 * @param R The return type for the visitor's operations.
 */
interface JsonVisitor<R> {
    fun visitObject(obj: JsonObject): R
    fun visitArray(array: JsonArray): R
    fun visitString(string: JsonString): R
    fun visitNumber(number: JsonNumber): R
    fun visitBoolean(boolean: JsonBoolean): R
    fun visitNull(nullValue: JsonNull): R
}

/**
 * Base class for all Json value types.
 */
abstract class JsonValue {
    /**
     * Converts the Json value to a valid Json string.
     */
    abstract fun toJsonString(): String
    /**
     * Alias for `toJsonString()`, for compatibility.
     */
    fun serialize(): String = toJsonString()
    /**
     * Accepts a visitor to process the Json value.
     * @param visitor A JsonVisitor instance.
     */
    abstract fun <R> accept(visitor: JsonVisitor<R>): R
}

/**
 * Represents a Json string.
 * @param value The string value.
 */
data class JsonString(val value: String) : JsonValue() {
    override fun toJsonString(): String = "\"$value\""
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitString(this)
}

/**
 * Represents a Json number.
 * @param value The numeric value.
 */
data class JsonNumber(val value: Double) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitNumber(this)
}

/**
 * Represents a Json boolean.
 * @param value The boolean value.
 */
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitBoolean(this)
}

/**
 * Represents the Json null value.
 */
object JsonNull : JsonValue() {
    override fun toJsonString(): String = "null"
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitNull(this)
}

/**
 * Represents a Json array.
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
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitArray(this)
    /**
     * Filters the array by element type.
     * @param condition The type name ("string", "number", "boolean", "array", "null").
     */
    fun filterJsonByType(condition: String):JsonArray{
        val filteredElements= elements.filter { entry ->
            when (condition){
                "string"-> entry is JsonString
                "number"-> entry is JsonNumber
                "boolean"-> entry is JsonBoolean
                "array"-> entry is JsonArray
                "null"-> entry is JsonNull
                else-> false
            }
        }
        return  JsonArray(filteredElements)
    }
    /**
     * Applies a transformation to each element in the array.
     * @param operation Function to transform each JsonValue.
     */
    fun mapList(operation: (JsonValue) -> JsonValue): JsonArray {
        val mappedElements = elements.map { operation(it) }
        return JsonArray(mappedElements)
    }
    companion object  {
        /**
         * Converts a Kotlin list to a JsonArray.
         * @param list The list to convert.
         * @throws IllegalArgumentException If an unsupported type is encountered.
         */
        fun fromList(list: List<Any?>): JsonArray {
            val jsonValues = list.map { value ->
                when (value) {
                    is String -> JsonString(value)
                    is Number -> JsonNumber(value.toDouble())
                    is Boolean -> JsonBoolean(value)
                    is List<*> -> fromList(value)
                    null -> JsonNull
                    else -> throw IllegalArgumentException("Unsupported type: ${value::class.simpleName}")
                }
            }
            return JsonArray(jsonValues)
        }
    }
}

/**
 * Represents a Json object.
 * @param properties A map of key-value pairs.
 */
data class JsonObject(val properties: Map<String, JsonValue>) : JsonValue() {
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
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitObject(this)
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
        return  JsonObject(filteredProperties)
    }
    /**
     * Filters properties by key.
     * @param keys Keys to retain.
     */
    fun filterPropertiesByKey(keys: List<String>): JsonObject {
        val filteredMap = properties.filterKeys { it in keys }
        return JsonObject(filteredMap)
    }
}

/**
 * Visitor to validate that:
 * - Json objects have unique keys.
 * - Values do not contain JsonNull.
 */
class JsonObjectValidationVisitor : JsonVisitor<Boolean> {
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
class JsonArrayHomogeneityVisitor : JsonVisitor<Boolean> {
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

