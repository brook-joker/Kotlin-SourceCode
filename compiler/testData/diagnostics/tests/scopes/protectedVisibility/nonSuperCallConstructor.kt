// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

open class A protected constructor(x: Int) {
    protected constructor() : this(1)
    public constructor(x: Double) : this(3)
}

class B4 : A(1) {
    init {
        <!OI;PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>()
        <!OI;PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>(1)
        A(5.0)
    }

    fun foo() {
        <!OI;PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>()
        <!OI;PROTECTED_CONSTRUCTOR_NOT_IN_SUPER_CALL!>A<!>(1)
        A(5.0)

        object : A() {}
        object : A(1) {}
        object : A(5.0) {}

        class Local : A()
    }
}