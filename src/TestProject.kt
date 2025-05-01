import org.junit.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

/**
 * Class for JUnit tests that validate the functionality of the Json library.
 *
 * Tests cover serialization, filtering, mapping, and validation operations on different Json types
 * including JsonString, JsonNumber, JsonBoolean, JsonNull, JsonArray, and JsonObject.
 */
class Test {

    /**
     * Test for serializing a JsonString object to a valid Json string.
     */
    @Test
    fun testJsonStringSerialization() {
        val s = JsonString("ciao")
        assertEquals("\"ciao\"", s.toJsonString())
    }

    /**
     * Test for serializing a JsonNumber object to a valid Json string.
     */
    @Test
    fun testJsonNumberSerialization() {
        val n = JsonNumber(3.14)
        assertEquals("3.14", n.toJsonString())
    }

    /**
     * Test for serializing a JsonBoolean object to a valid Json string.
     */
    @Test
    fun testJsonBooleanSerialization() {
        val b = JsonBoolean(true)
        assertEquals("true", b.toJsonString())
    }

    /**
     * Test for serializing a JsonNull object to a valid Json string.
     */
    @Test
    fun testJsonNullSerialization() {
        assertEquals("null", JsonNull.toJsonString())
    }

    /**
     * Test for serializing a JsonArray containing mixed types
     * (JsonNumber, JsonString, JsonBoolean) to a valid Json string.
     */
    @Test
    fun testJsonArraySerialization() {
        val array = JsonArray(listOf(JsonNumber(1.0), JsonString("a"), JsonBoolean(false)))
        assertEquals("[1.0,\"a\",false]", array.toJsonString())
    }

    // Secondo me (Agnese) si può togliere, visto che poi chiede di avere
    // specificamente una funzione per la serializzazione di oggetti compositi.
    // (C'è un test uguale sotto)
    /**
     * Test for serializing a JsonObject containing various Json types
     * (JsonString, JsonNumber, JsonBoolean, JsonArray) to a valid Json string.
     */
    @Test
    fun testJsonObjectSerialization() {
        val obj = JsonObject(mapOf(
            "name" to JsonString("Alice"),
            "age" to JsonNumber(25.0),
            "active" to JsonBoolean(true),
            "skill" to JsonArray(listOf(JsonString("Python"), JsonString("Kotlin"), JsonString("Java")))
        ))
        val expected = """{"name":"Alice","age":25.0,"active":true,"skill":["Python","Kotlin","Java"]}"""
        assertEquals(expected, obj.toJsonString())
    }

    // Da sistemare
    /**
     * Test for filtering a JsonObject based on a specified type ("string").
     * Also tests filtering properties by keys.
     */
    @Test
    fun testFilterJsonProperties() {
        val obj = JsonObject(mapOf(
            "name" to JsonString("Alice"),
            "age" to JsonNumber(25.0),
            "active" to JsonBoolean(true),
            "skill" to JsonArray(listOf(JsonString("Python"), JsonString("Kotlin"), JsonString("Java")))
        ))
        val type = "string"
        val output = obj.filterJsonByType(type)
        val output2 = obj.filterPropertiesByKey(listOf("name", "age", "skill"))

        assertEquals(output.toJsonString(), """{"name":"Alice"}""")
        //assertEquals(output2.toJsonString(), """{"name":"Alice","age":25.0,"skill":["Python","Kotlin","Java"]}""")
    }

    /**
     * Test for filtering elements in a JsonArray based on a specified type ("string").
     */
    @Test
    fun testFilterJsonArray() {
        val array = JsonArray(listOf(JsonNumber(1.0), JsonString("a"), JsonBoolean(false)))
        val type = "string"
        val output = array.filterJsonByType(type)
        //val condition= "="
        assertEquals(output.toJsonString(),"[\"a\"]")
    }

    /**
     * Test for the mapping function on a JsonArray.
     * The test multiplies numeric values by 2 and returns a new JsonArray.
     */
    @Test
    fun testMappingArray() {
        val list = JsonArray.fromList(listOf(1, 2, 3))
        val mapped = list.mapList { jsonValue ->
            if (jsonValue is JsonNumber) {
                JsonNumber(jsonValue.value * 2)
            } else {
                jsonValue
            }
        }
        assertEquals("[2.0,4.0,6.0]", mapped.toJsonString())
    }

    /**
     * Test for the JsonObjectValidationVisitor to validate a JsonObject.
     * Ensures that all object properties and their types are valid.
     */
    @Test
    fun testJsonObjectValidation() {
        val validObj = JsonObject(mapOf(
            "name" to JsonString("Alice"),
            "age" to JsonNumber(30.0),
            "isStudent" to JsonBoolean(false)
        ))
        val invalidObj = JsonObject(mapOf(
            "name" to JsonString("Bob"),
            "nullValue" to JsonNull
        ))
        val validator = JsonObjectValidationVisitor()
        assertTrue(validObj.accept(validator))
        assertTrue(!invalidObj.accept(validator))
    }

    /**
     * Test for the JsonArrayHomogeneityVisitor to check if all elements in a JsonArray are of the same type.
     * Ensures homogeneity of the array's elements.
     */
    @Test
    fun testJsonArrayHomogeneity() {
        val validArray = JsonArray(listOf(JsonString("a"), JsonString("b"), JsonString("c")))
        val invalidArray = JsonArray(listOf(JsonString("a"), JsonNumber(1.0), JsonBoolean(true)))
        val checker = JsonArrayHomogeneityVisitor()
        assertTrue(validArray.accept(checker))
        assertTrue(!invalidArray.accept(checker))
    }

    /**
     * Test for the serialization alias function,
     * ensuring that both `toJsonString()` and `serialize()` return the same result.
     */
    @Test
    fun testSerializeAliasFunction() {
        val obj = JsonObject(mapOf(
            "name" to JsonString("Agnese"),
            "age" to JsonNumber(24.0),
            "skills" to JsonArray(listOf(JsonString("Kotlin"), JsonString("Java"))),
        ))
        assertEquals(obj.toJsonString(), obj.serialize())
    }
}
