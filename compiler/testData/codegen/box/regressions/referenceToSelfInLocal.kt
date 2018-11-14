// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME
// KT-4351 Cannot resolve reference to self in init of class local to function

fun box(): String {
    var accessedFromConstructor: Class<*>? = null

    class MyClass() {
        init {
            accessedFromConstructor = MyClass::class.java
        }
    }

    MyClass()
    if (accessedFromConstructor!!.getName().endsWith("MyClass")) {
        return "OK"
    } else {
        return accessedFromConstructor.toString()
    }
}