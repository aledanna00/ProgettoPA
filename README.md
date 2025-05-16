# KotlinJsonModel

A Kotlin library to build and manipulate in-memory JSON models with support for serialization and type-safe inference.

## Features
- Build JSON programmatically (objects, arrays, strings, numbers, booleans, null)
- Filter/map JSON structures
- Validate JSON
- Serialize to JSON strings
- Infer JSON models from Kotlin data structures using reflection

## Library Class Diagram
![ClassDiagram](https://github.com/aledanna00/ProgettoPA/blob/master/Class_diagram/ClassDiagram.png)

## Usage

### 1. Building a JSON Object
```kotlin
val obj = JsonObject(
    mapOf(
        "name" to JsonString("John"),
        "age" to JsonNumber(30),
        "active" to JsonBoolean(true)
    )
)

println(obj.toJsonString()) 
// Output: {"name":"John","age":30.0,"active":true}
```

### 2. Filtering Properties by Keys
```kotlin
val obj = JsonObject(
    mapOf(
        "name" to JsonString("John"),
        "age" to JsonNumber(30),
        "active" to JsonBoolean(true)
    )
)

val filtered = obj.filterPropertiesByKey(listOf("name", "student"))
println(filtered.toJsonString())  
// Output: {"name":"John"}
```

### 3. Mapping JSON Arrays
```kotlin
val numbers = JsonArray.fromList(listOf(1, 2, 3))
val doubled = numbers.mapList { value ->
    if (value is JsonNumber) JsonNumber(value.value * 2)
    else value
}
println(doubled.toJsonString())  
// Output: [2.0, 4.0, 6.0]
```

### 4. Object Validation
```kotlin
val obj = JsonObject(
    mapOf(
        "name" to JsonString("John"),
        "age" to JsonNumber(30),
        "active" to JsonBoolean(true)
    )
)

val validator = JsonObjectValidationVisitor()
val isValid = obj.accept(validator)
println("Valid JSON? $isValid")  
// Output: true (or false if invalid)
```

### 5. Array Homogeneity Check
```kotlin
val homogeneous = JsonArray(listOf(JsonString("a"), JsonString("b"))).accept(JsonArrayHomogeneityVisitor())
println("Homogeneous? $homogeneous")  
// Output: true

```
# Kotlin Framework

In the file Server.kt we implemented an HTTP server in Kotlin using Java's built-in HttpServer, capable of handling GET requests with dynamic routing, path parameters, and query parameters. It also includes basic JSON serialization for response data.
The postman collection is [here](https://github.com/aledanna00/ProgettoPA/blob/master/PA2025.postman_collection.json)

## Usage
Firstly it is mandatory to open the connection with the server, so that it listens at port 8080. Below you can find: 
1. The routes implemented
2. The associated script
3. Their output

http://localhost:8080/PA2025/array

```kotlin
@Mapping("array")
    fun array(): String {
        val sentence = listOf("hello", "world", "!")
        val jsonArray = toJsonArray(sentence)
        return jsonArray.toJsonString()
    }
```
The relative output is:
```json
"["hello","world","!"]"
```

http://localhost:8080/PA2025/obj
```kotlin
@Mapping("obj")
    fun obj(): String{
        val person= linkedMapOf(
            "name" to JsonString("Emily"),
            "age" to JsonNumber(22.0),
            "isStudent" to JsonBoolean(true)
        )
        return JsonObject(person).toJsonString()
    }
```
The relative output is:
```json
"{"name":"Emily","age":22.0,"isStudent":true}"
```

http://localhost:8080/PA2025/path/obj
```kotlin
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
```
The relative output is:
```json
"{"name":"Mark","age":19.0,"isStudent":true}"
```

http://localhost:8080/PA2025/args?name=Mark&family=mom
```kotlin
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
```
The relative output is:
```json
"{"name":"Caroline","age":46.0,"isStudent":false}"
```

## File JAR
A JAR file was created for the server. The main class defined in the manifest is `ServerKt`. It can be found [here](https://github.com/aledanna00/ProgettoPA/blob/master/artifacts/ProgettoPA.jar )

### Requirements

- **Java 8** or higher installed

### How to Run

1. Open a terminal inside IntelliJ IDEA or your operating system.
2. Navigate to the directory where the `ProgettoPA.jar` file is located.
3.  Run the server using the command `java -jar ProgettoPA.jar`
4.  If the server starts successfully, it will begin listening for incoming HTTP requests (on port 8080).
