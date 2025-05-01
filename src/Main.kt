/*
Creare una libreria Kotlin che ti permette di costruire e manipolare strutture JSON
 direttamente da codice, senza leggere o scrivere file.
 */
// utilizzo pattern Visitor
interface JsonVisitor<R> {
    fun visitObject(obj: JsonObject): R
    fun visitArray(array: JsonArray): R
    fun visitString(string: JsonString): R
    fun visitNumber(number: JsonNumber): R
    fun visitBoolean(boolean: JsonBoolean): R
    fun visitNull(nullValue: JsonNull): R
}
// Classe base sigillata
abstract class JsonValue {
    abstract fun toJsonString(): String
    abstract fun <R> accept(visitor: JsonVisitor<R>): R
}

// JSON String
data class JsonString(val value: String) : JsonValue() {
    override fun toJsonString(): String = "\"$value\""
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitString(this)
}

// JSON Number
data class JsonNumber(val value: Double) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitNumber(this)
}

// JSON Boolean
data class JsonBoolean(val value: Boolean) : JsonValue() {
    override fun toJsonString(): String = value.toString()
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitBoolean(this)
}

// JSON Null
object JsonNull : JsonValue() {
    override fun toJsonString(): String = "null"
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitNull(this)
}

// JSON Array
data class JsonArray(val elements: List<JsonValue>) : JsonValue() {
    override fun toJsonString(): String =
        elements.joinToString(separator=",",prefix = "[",postfix ="]") { it.toJsonString() }
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitArray(this)
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

    companion object  {
        // Function to convert a list in a JsonArray
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

    // Map function
    fun mapList(operation: (JsonValue) -> JsonValue): JsonArray {
        val mappedElements = elements.map { operation(it) }
        return JsonArray(mappedElements)
    }
}

// JSON Object
data class JsonObject(val properties: Map<String, JsonValue>) : JsonValue() {
    override fun toJsonString(): String =
        properties.entries.joinToString(separator=",",prefix = "{", postfix = "}") {
            "\"${it.key}\":${it.value.toJsonString()}"
        }
    override fun <R> accept(visitor: JsonVisitor<R>): R = visitor.visitObject(this)
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
    fun filterPropertiesByKey(keys: List<String>): JsonObject {
        val filteredMap = properties.filterKeys { it in keys }
        return JsonObject(filteredMap)
    }
}

/*
* Il secondo punto del progetto riguarda il filtraggio delle strutture JSON in memoria,
* ossia la capacità di selezionare o escludere elementi da un oggetto JSON (JsonObject)
* o da un array JSON (JsonArray), creando nuove istanze con gli elementi che soddisfano
* un certo criterio di filtro, senza modificare l'originale.
* */

