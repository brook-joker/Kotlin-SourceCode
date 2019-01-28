
# 类型系统

## 可空性
### 可空类型
在kotlin中，所有类型默认都是`非null`值. 如果你需要声明的是一个可空类型时，需要在对应的类型后面加 `?` 来表明此类型可以持有`null`值
例如，我们在Java中声明一个方法:
```java
  void test(String str){
    //...
  }
```
对应kotlin的实现应该是：
```kotlin
  //正确的写法      
  fun test(str: String?) {
    //...
  }
    
  //编译器自动转换的错误写法 
  //这种写法一旦调用的地方传入了null值会抛异常  
  fun test(str: String) {
    //...
  }
```

#### 这种设计的好处和坏处是什么？

先说好处，回想在Java中，我们经常为了忘记判空而导致NPE出现在各个犄角旮旯里面。kotlin为了解决这个问题，将类型区分成非空和可空，一旦我们声明了可空类型，编译器在编译阶段会检查代码中是否存在NPE的地方，通过编译失败的方式来强制我们去进行检查判空从而大大减少了NPE的发生. 但是，一旦我们将声明了非空类型时，如果尝试给非空类型赋值`null`时，会出现类型转换异常. 尤其是我们在声明XXBean类的时候，建议尽可能的将所有类型声明为可空类型.



### 安全调用运算符 ”？.“
#### 可空对象与属性的安全处理方式
`?.`将null检查和方法调用合并成一个操作
比如打印后端下发数据中的人的名字.
```kotlin
data class DataBean(val id: Int, val person: PersonBean?)

data class PersonBean(val name: String?, val age: Int)

fun getName(dataBean: DataBean?): String? = dataBean?.person?.name

fun main(args: Array<String>) {
    val dataBean1 = DataBean(1,null);
    val dataBean2 = DataBean(2, PersonBean("kotlin",2))
    println(getName(dataBean1))
    println(getName(dataBean2))
}    
```
>运行结果:
>null
>kotlin


#### 非空断言!!

非空断言是kotlin提供的最简单的处理可空类型的工具,它使用!!表示，目的是将任何值转成非空值，一旦对null值做非空断言，则会抛出异常，例如以下示例：
```kotlin
fun main(args: Array<String>) {
    val str: String? = null
    println(str!!.length)
}
```
>Exception in thread "main" kotlin.KotlinNullPointerException
>	at com.kotlin.MainKt.main(Main.kt:26)
    
    
**建议检查项目中所有的!!使用的地方，使用?.和?.let等安全操作去替换，否则线上可能会出现KNPE导致你的项目出现无法预知的事故**

#### 安全转换 " :as? "
`as?`会尝试把值转换成指定的类型，如果类型不合适就返回null.
`as`会尝试把值转换成指定的类型，如果类型不合适就抛出类型转换异常.
//todo 代码示例

#### Elvis 运算符 “?:”
Elvis运算符有点像简化版的三目运算符,下面是它的使用方式
```kotlin
val str = a ?: b
```
这行代码的意思是
若a!=null，则str = a；
若a==null，str=b。
>常用于处理某些场景我们不想让数据为null, 进行的一些空字符、空对象、空集合等替换null的保护措施.


需要注意的是Elvis运算符的优先级是比加减乘除还要低的，所以在使用的时需要考虑是否加上括号来提高优先级保证逻辑正常.
```kotlin
//先计算3 + c 然后判断加法的结果是否为null 
val a = 3 + c ?: 0
//先判断c是否为null 然后再进行加法运算
val b = 3 + (c ?: 0)
```


#### let函数处理可空类型
日常开发中，我们经常需要在执行某些代码块或者调用函数的时候，保证数据类型不为null，在Java中我们经常写的就是判断某个对象不为null，然后再接着调用.在kotlin中，我们可以使用`?.let`来代替对某个对象的判空操作.

```kotlin
data class PersonBean(val name: String?, val age: Int)

fun toString(dataBean: PersonBean) {
    println(dataBean.name + "  " + dataBean.age)
}


fun main(args: Array<String>) {
    val data: PersonBean? = PersonBean("kotlin", 2)
    val nullData = null
    data?.let { toString(it) }
    nullData?.let { toString(it) }
 }
```
>输出结果
>kotlin 2
>
>在let的lambda表达式中it指向是调用let的对象（即data和nullData）.
关于let的详解请关注lambda表达式章节

#### 延迟属性初始化
Koltin中属性在声明的同时也要求要被初始化，否则会报错。但是很多时候，我们知道变量在某些时候肯定会进行初始化，并且是`非null`值，我们不想在调用的地方再次进行判空操作，这个时候你就要kotlin提供的延迟属性初始化了.

Kotlin中有两种延迟初始化的方式。一种是`lateinit var`，一种是`by lazy`。

**lateinit var**
```kotlin
private lateinit var str: String
```
  lateinit var只能用来修饰类属性，不能用来修饰局部变量，并且只能用来修饰对象，不能用来修饰基本类型(基本类型的属性在类加载后的准备阶段都会被初始化为默认值)。
lateinit var的作用也比较简单，就是让编译期在检查时不要因为属性变量未被初始化而报错。
lateinit vard不能用在可空类型的对象上，具体原因是源码内部会根据null值来判断是否进行了初始化.
  Kotlin相信当开发者显式使用lateinit var 关键字的时候，他一定也会在后面某个合理的时机将该属性对象初始化的。

**by lazy**
by lazy本身是一种属性委托。属性委托的关键字是by，用法如下:
```kotlin
//用于属性延迟初始化
val str: String by lazy { ”kotlin“ }

//用于局部变量延迟初始化
public fun foo() {
    val str by lazy { "kotlin" }
    println(bar)
}
```
by lazy要求属性声明为val，即不可变变量，在java中相当于被final修饰。
这意味着该变量一旦初始化后就不允许再被修改值了(基本类型是值不能被修改，对象类型是引用不能被修改)。{}内的操作就是返回唯一一次初始化的结果。
by lazy可以使用于类属性或者局部变量。
by lazy有三种线程模式(默认SYNCHRONIZED):
- LazyThreadSafetyMode.SYNCHRONIZED:
- LazyThreadSafetyMode.PUBLICATION:
- LazyThreadSafetyMode.NONE:



**lateinit var和by lazy哪个更好用？**







#### 可空的扩展函数
todo

#### 平台类型

对于Kotlin来说，Java中的任何引⽤都可能是null，这使得 Kotlin 对来⾃ Java 的对象要求严格空安全是不现实的。Java声明的类型在Kotlin中会被特别对待并称为平台类型。(其他语言也一样)

对这种类型的空检查会放宽，因此它们的安全保证与在 Java 中相同
```kotlin
val list = ArrayList<String>()  // ⾮空（构造函数结果）
list.add("Item")
val size = list.size  // ⾮空（原⽣ int）
val item = list[0]     // 推断为平台类型（普通 Java 对象
```
当我们调⽤平台类型变量的⽅法时，Kotlin 不会在编译时报告可空性错误，但在运⾏时调⽤可能会失败，因为空指针异常或者 Kotlin ⽣成的阻⽌空值传 播的断⾔：
```kotlin
item.substring(1) // 允许，如果 item == null 可能会抛出异常
```
平台类型是不可标⽰的，意味着不能在语⾔中明确地写下它们。当把⼀个平台值赋值给⼀个 Kotlin 变量时，可以依赖类型推断（该变量会具有推断出的的 平台类型，如上例中 item 所具有的类型），或者我们可以选择我们期望的类型（可空或⾮空类型均可）：
```kotlin
val nullable: String? = item  // 允许，没有问题
val notNull: String = item     // 允许，运⾏时可能失败
```
如果我们选择⾮空类型，编译器会在赋值时触发⼀个断⾔。这防⽌ Kotlin 的⾮空变量保存空值。当我们把平台值传递给期待⾮空值等的 Kotlin 函数时，也 会触发断⾔。总的来说，编译器尽⼒阻⽌空值通过程序向远传播（尽管鉴于泛型的原因，有时这不可能完全消除）


返回平台类型表达式的公共函数/方法必须显式声明其Kotlin类型：
```kotlin
fun apiCall(): String = MyJavaApi.getProperty("name")
```
任何使用平台类型表达式初始化的属性（包级别或类级别）必须明确声明其Kotlin类型：
```kotlin
class Person {
    val name: String = MyJavaApi.getProperty("name")
}
```

使用平台类型表达式初始化的局部变量可能有或没有类型声明：
```kotlin
fun main(args: Array<String>) {
    val name = MyJavaApi.getProperty("name")
    println(name)
}
```

### 基本类型进阶

#### 基本类型与数字的安全转换

#### 理解Any、Nothing、Unit


### 集合与数组


#### 集合简介

#### 集合的