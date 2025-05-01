import org.junit.Test
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

class Test{
    @Test
    fun testJsonStringSerialization() {
        val s = JsonString("ciao")
        assertEquals("\"ciao\"", s.toJsonString())
    }

    @Test
    fun testJsonNumberSerialization() {
        val n = JsonNumber(3.14)
        assertEquals("3.14", n.toJsonString())
    }

    @Test
    fun testJsonBooleanSerialization() {
        val b = JsonBoolean(true)
        assertEquals("true", b.toJsonString())
    }

    @Test
    fun testJsonNullSerialization() {
        assertEquals("null", JsonNull.toJsonString())
    }

    @Test
    fun testJsonArraySerialization() {
        val array = JsonArray(listOf(JsonNumber(1.0), JsonString("a"), JsonBoolean(false)))
        assertEquals("[1.0,\"a\",false]", array.toJsonString())
    }

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

    @Test
    fun testFilterJsonProperties(){
        val obj = JsonObject(mapOf(
            "name" to JsonString("Alice"),
            "age" to JsonNumber(25.0),
            "active" to JsonBoolean(true),
            "skill" to JsonArray(listOf(JsonString("Python"), JsonString("Kotlin"), JsonString("Java")))
        ))
        val type= "string"
        val output= obj.filterJsonByType(type)

        val output2= obj.filterPropertiesByKey(listOf("name", "age","skill"))
        assertEquals(output.toJsonString(), """{"name":"Alice"}""" )
        //assertEquals(output2.toJsonString(), """{"name":"Alice","age":25.0,"skill":["Python","Kotlin","Java"]}""" )
    }

    @Test
    fun testFilterJsonArray(){
        val array = JsonArray(listOf(JsonNumber(1.0), JsonString("a"), JsonBoolean(false)))
        val type = "string"
        val output= array.filterJsonByType(type)
        //val condition= "="
        assertEquals(output.toJsonString(),"[\"a\"]" )
    }

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

    // Visitor test
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

    // Visitor test
    @Test
    fun testJsonArrayHomogeneity() {
        val validArray = JsonArray(listOf(JsonString("a"), JsonString("b"), JsonString("c")))
        val invalidArray = JsonArray(listOf(JsonString("a"), JsonNumber(1.0), JsonBoolean(true)))
        val checker = JsonArrayHomogeneityVisitor()
        assertTrue(validArray.accept(checker))
        assertTrue(!invalidArray.accept(checker))
    }
}
