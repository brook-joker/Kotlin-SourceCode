package kotlin.properties

import kotlin.reflect.KProperty

/**
 * Standard property delegates.
 */
public object Delegates {
    /**
     * Returns a property delegate for a read/write property with a non-`null` value that is initialized not during
     * object construction time but at a later time. Trying to read the property before the initial value has been
     * assigned results in an exception.
     * 返回具有非`null`值的读/写属性的属性委托，该值在对象构造时间内但不是在稍后时间初始化。
     * 尝试在分配初始值之前读取属性会导致异常。
     *
     * @sample samples.properties.Delegates.notNullDelegate
     */
    public fun <T: Any> notNull(): ReadWriteProperty<Any?, T> = NotNullVar()

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed.
     * 返回更改时调用指定回调函数的读/写属性的属性委托。
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called after the change of the property is made. The value of the property
     *  has already been changed when this callback is invoked.
     *
     *  @sample samples.properties.Delegates.observableDelegate
     */
    public inline fun <T> observable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Unit):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(property, oldValue, newValue)
        }

    /**
     * Returns a property delegate for a read/write property that calls a specified callback function when changed,
     * allowing the callback to veto the modification.
     * 为更改时调用指定回调函数的读/写属性返回属性委托，允许回调否决修改。
     * @param initialValue the initial value of the property.
     * @param onChange the callback which is called before a change to the property value is attempted.
     *  The value of the property hasn't been changed yet, when this callback is invoked.
     *  If the callback returns `true` the value of the property is being set to the new value,
     *  and if the callback returns `false` the new value is discarded and the property remains its old value.
     *
     *  @sample samples.properties.Delegates.vetoableDelegate
     *  @sample samples.properties.Delegates.throwVetoableDelegate
     */
    public inline fun <T> vetoable(initialValue: T, crossinline onChange: (property: KProperty<*>, oldValue: T, newValue: T) -> Boolean):
        ReadWriteProperty<Any?, T> = object : ObservableProperty<T>(initialValue) {
            override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T): Boolean = onChange(property, oldValue, newValue)
        }

}


private class NotNullVar<T: Any>() : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    /**
     * 如果字段未初始化就调用就会抛出对应的异常
     */
    public override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value ?: throw IllegalStateException("Property ${property.name} should be initialized before get.")
    }

    public override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}

