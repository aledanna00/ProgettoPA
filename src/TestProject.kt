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
    fun testMappingArray(){
        val numberArray = JsonArray(listOf(JsonNumber(1.0),JsonString("cane"), JsonNumber(2.0), JsonNumber(3.0)))
        val stringArray= JsonArray(listOf(JsonString("ciao"), JsonString("cane"), JsonString("a")))

        val multipliedArray = numberArray.multiplyBy(2.0)
        val uppercaseArray = stringArray.toUpperCase()

        assertEquals(multipliedArray.toJsonString(),"[2.0,\"cane\",4.0,6.0]")
        assertEquals(uppercaseArray.toJsonString(),"[\"CIAO\",\"CANE\",\"A\"]")

        val list = listOf(1,2,3)
    }
}
