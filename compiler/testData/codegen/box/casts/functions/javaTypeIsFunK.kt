// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FILE: JFun.java

class JFun implements kotlin.jvm.functions.Function0<String> {
    public String invoke() {
        return "OK";
    }
}

// FILE: test.kt

fun box(): String {
    val jfun = JFun()
    val jf = jfun as Any
    if (jf is Function0<*>) return jfun()
    else return "Failed: jf is Function0<*>"
}