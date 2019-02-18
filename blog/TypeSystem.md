
# 类型系统
经过前面的章节学习，相信大家对Kotlin的语法有一定的熟悉程度了.本章节将给大家介绍Kotlin中较为重要的一部分：`类型系统`.

通过本章节的学习，你将会写出更加健壮和优雅的Kotlin代码.

本章节内容安排如下
>处理NULL的可空类型和语法
>
>基本数据类型与Java的关系
>
>Kotlin中集合数组与Java的关系

## 可空性
可空性是Kotlin类型系统中帮助开发者避免`NullPointerException`异常而引入的新特性.相信各种使用Java语言的开发者都熟知空指针异常是Java代码中`最最最常见的错误`. 
无论你是新手还是经验丰富的老司机、相信都曾在某个不经意的瞬间写下了引发空指针异常的代码。
如今，Kotlin为了解决这个问题，把运行时异常转变成编译器的错误。通过可空性特性，编译器就能在编译期间发现潜在的错误，大大减少了运行时抛出异常的可能性.

这一小节，我们主要讨论可空性是什么？Kotlin怎么表示可空的值，如何利用Kotlin提供的工具（sugar）正确且优雅的处理NULL.

### 可空类型
在Kotlin中，所有类型默认都是`非null`值. 如果你需要声明的是一个可空类型时，需要在对应的类型后面加 `?` 来表明此类型可以持有`null`值
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


### 非空断言!!

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

### 安全转换 " :as? "
`as?`会尝试把值转换成指定的类型，如果类型不合适就返回null.
`as`会尝试把值转换成指定的类型，如果类型不合适就抛出类型转换异常.
```kotlin
val str = "kotlin"
//抛异常java.lang.ClassCastException: java.base/java.lang.String cannot be cast to java.base/java.lang.Integer
println(str as Int)
//输出结果null
println(str as? Int)
```

### Elvis 运算符 “?:”
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


### let函数处理可空类型
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

### 延迟属性初始化
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



**lateinit var和by lazy优劣之处？**

  lateinit var 在编译期忽略对属性未初始化的检查，后续何时何地初始化需要开发者自己决定，有一定的未初始化风险。

  by lazy 在声明的同时也指定了延迟初始化时的行为，在属性被第一次被使用的时候能自动初始化。不存在属性未初始化风险，但需要注意可空性的问题：例如属性声明为非空，但是初始化行为返回了null。

lateinit var不支持对可空属性的延迟初始化，by lazy支持对可空属性的延迟初始化.
两者的使用场景不同，需要根据实际开发情况去选择使用.


### 可空类型的扩展函数
为可空类型定义扩展函数时一种更优雅的处理null值的方式，它允许接收者为null的（扩展函数）调用，并在该函数中处理null，而不是在确保变量不为null后再调用它的方法。

只有扩展函数能做到这点，普通成员方法的调用是通过对象实例分发的，因此实例为null时（成员方法）永远不能被执行。


在Kotlin标准库中定义的`isNullOfEmpty`就可以由String?类型的接收者调用.
`isNullOfEmpty`的定义如下：
```kotlin
@kotlin.internal.InlineOnly
public inline fun CharSequence?.isNullOrEmpty(): Boolean {
    contract {
        returns(false) implies (this@isNullOrEmpty != null)
    }

    return this == null || this.length == 0
}
```


### 平台类型

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

## 基本类型进阶

### 基本类型与数字的安全转换
Kotlin在处理数字转换时要求必须是显式的转换，为了支持显式转换，kotlin中的除了布尔以为的基本数据类型都定义有转换函数: toByte(),toShort(),toChar()等等.

错误示例：
```kotlin
val i = 1
//无法通过编译 类型不匹配
val j : Long = i
```

正确示例：
```kotlin
val i = 1
val j : Long = i.toLong()
```

装箱类型比较示例：
```kotlin
val x = 1
val y = 1
val list = listOf(1L,2L,3L)
//false
x in list
//true
y.toLong() in list
```


### 理解Any、Nothing、Unit
#### Any

Any是Kotlin所有非空类型的超类型，包含下面三个方法：`toString`、`equals`和`hashCode`.如果你需要的类型是一个可空类型，则必须使用`Any?`类型。
在底层，Any类型对应java.lang.Object，Kotlin把Java方法参数和返回类型中用到的Object类型看作Any，当Kotlin函数函数中使用Any时，它会被编译成Java字节码中的Object。
Any不能使用其它Object的方法（例如wait和notify），但是可以通过手动把值转换成java.lang.Object来调用这些方法。


#### Nothing
Nothing是⽤于标记永远不能达到的代码位置。在你⾃⼰的代码中，你可以使⽤ Nothing 来标
记⼀个永远不会返回的函数：
```kotlin
fun fail(message: String): Nothing {
    throw IllegalArgumentException(message)
}
```
当你调⽤该函数时，编译器会知道执⾏不会超出该调⽤：
```kotlin
val s = person.name ?: fail("Name required")
println(s) // 在此已知“s”已初始化
```

可能会遇到这个类型的另⼀种情况是类型推断。这个类型的可空变体 Nothing? 有⼀个可能的值是 null 。如果⽤ null 来初始化⼀个要推断类型的值，⽽⼜没有其他信息可⽤于确定更具体的类型时，编译器会推断出 Nothing? 类型：
```kotlin
val x = null // “x”具有类型 `Nothing?`
val l = listOf(null) // “l”具有类型 `List<Nothing?>
```


#### Unit

kotlin中的Unit对应于Java中的void,表示无意义的值，在用作函数返回值时可以省略掉.


## 集合与数组


### 集合简介
与平时我们接触的编程语言不一样，Kotlin 区分可变集合和不可变集合（lists、sets、maps 等）。精确控制什么时候集合可编辑有助于消除 bug 和设计良好的 API。
- kotlin.collections.Collection：只能对集合中的元素进行读取数据的操作。
- kotlin.collections.MutableCollection：对集合中的数据可以执行正常的增删改查操作。

使用原则: 如果你的集合在初始化赋值完成以后不会再对其进行增删改，那么你可以使用不可变集合，否则你需要使用可变集合.
```kotlin
//只读list
val list: List<Int> = arrayListOf(1, 2, 3)
//可以对集合进行增删改查对应于Java的ArrayList
val mutableList: MutableList<Int> = arrayListOf(1, 3, 3)
```

### 可空性和集合
在前面我们讨论可空性，在集合中时常也需要持有null元素，那么我们就需要创建一个可空性的集合. 创建方式很简单在集合的通配符类型参数后面加上对应的可空性符号？
```kotlin
//可以包含null值
val nulllist: List<Int?> = arrayListOf(1, 2, 3, null)
//不可以包含null值
val noNullList: List<Int> = arrayListOf(1, 3, 3)
```

### Kotlin集合与Java集合的关系
Java中的集合接口在kotlin中分成了两种：只读的和可变的.
在下图中可以看出，Java类都继承了kotlin的可变接口（MutableXX）
![43439f3ac61a841b700e38c0f3db59c1.png](evernotecid://A22DE78C-58E6-459C-BE8D-A67C525FF247/appyinxiangcom/21926780/ENResource/p91)

Kotlin中只读接口和可变接口的基本结构与java.util中的Java集合接口的结构是平行的。可变接口直接对应java.util包中的接口，而它们的只读版本缺少了所有产生改变的方法。


当你有一个使用java.util.Collection做形参的Java方法，可以把任意Collection或MutableCollection的值作为实参传递给这个形参。Java并不会区分只读集合和可变集合，也就是说即使Kotlin中把集合声明成只读的，Java代码也可以修改这个集合，例如下面的代码，虽然我们将printInUppercase接收的list参数声明为只读的，但是仍然可以通过Java代码修改它。
```kotlin
//CollectionUtils.java
public class CollectionUtils {
    public static List<String> uppercaseAll(List<String> items) {
        for (int i = 0; i < items.size(); i++) {
            items.set(i, items.get(i).toUpperCase());
        }
        return items;
    }
}

//collections.kt
fun printInUppercase(list : List<String>) {
    println(CollectionUtils.uppercaseAll(list));
    println(list.first())
}
```

### 平台类型的集合
kotlin会把那些定义在Java代码中的类型看成`平台类型`，Kotlin没有任何关于平台类型的可空性信息，所以编译器允许Kotlin代码将其视为可空或者非空，同样，Java中声明的集合类型的变量也被视为平台类型。

那么当我们实现或者重写某个有集合类型的Java方法时，我们需要考虑使用哪种kotlin类型来表示这个Java类型来消除以下疑问：
- 集合是否null
- 集合元素是否可能会持有null
- 集合会不会被修改

例如下面这个Java接口
```java
interface Parser<T> {
    void parse(String data, List<T> result);
}
```
考虑如下：
- data可能为null
- List<T>可能也会为null
- List<T>集合会被频繁的修改

Kotlin实现如下：
```kotlin
class DataParser : Parser<Data> {
    override fun parse(data : String?, result : MutableList<Data>?)
}
```

### 集合常见操作符
#### filter 和 map
filter函数用来从集合中移除你不想要的元素并放在一个新集合.
```kotlin
val list = listOf(1, 2, 3, 4)
println(list.filter { it % 2 == 0 })
```
![filter](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/filter.jpg?raw=true)

map函数对集合中的每一个元素应用给定的函数并把结果收集到一个新集合
```kotlin
val list = listOf(1, 2, 3, 4)
println(list.map { it * it })
```
![map](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/map.jpg?raw=true)
 

filter和map函数调用之后都是返回新集合，所以我们可以链式调用使用.
```kotlin
val list = listOf(1, 2, 3, 4)
println(list.filter { it % 2 == 0 }.map { it * it })
```

![map](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/filter&map.jpg?raw=true)

#### 集合查询判断：all any count find
以前我们为了判断集合中的元素是否满足某个特定的条件，不得不去手撕代码一遍一遍去做重复工作. 现在kotlin为我们提供了集合操作符来帮助我们提升工作效率.
##### all
判断集合中的元素是否都满足条件
```kotlin
val list = listOf(1, 2, 3, 4)
//集合元素是否全都大于0 true  
println(list.all { it > 0 })
//集合元素是否全都小于0 false
println(list.all { it < 0 })
```
##### any
检查集合中是否至少存在一个匹配的元素
```kotlin
val list = listOf(1, 2, 3, 4)
//集合元素是否存在大于3的元素 true
println(list.any { it > 3 })
//集合元素是否存在小于0的元素 false
println(list.any { it < 0 })
```
##### count
统计集合中有多少个元素满足了特定的条件
```kotlin
val list = listOf(1, 2, 3, 4)
//集合元素大于1的元素有多少个 
//结果3
println(list.count { it > 1 })
```
>使用正确的函数完成工作: 'count' vs 'size'
>count方法容易被遗忘，经常会使用过滤集合之后再取大小来实现：
> ```kotlin
>  println(list.filter { it > 1 }.size)
> ```
> 在这种情况下，一个中间集合会被创建并用来存储所有满足判断式的元素.
> count方法只是跟踪匹配元素的数量，不关心元素本身，所以更高效.

##### find
寻找满足条件的元素并返回，如果有多个匹配的元素则返回第一个，没有匹配上返回null.
```kotlin
val list = listOf(1, 2, 3, 4)
//集合元素大于1的元素有多少个
//返回结果 2
println(list.find { it > 1 })
//返回结果 null
println(list.find { it < 1 })
```

#### 列表转换成分组: groupBy
groupBy可以需要根据不同的条件将集合中所有元素划分成不同的分组.
比如: 你想把人按年龄进行分组，相同年龄的人一组,将这个条件当作参数传递十分方便.
```kotlin
val people = listOf(Person("小明", 18), Person("小红", 19), Person("小强", 18))
println(people.groupBy { it.age })
```
操作过程：根据元素分组依赖的key(本例子中的age属性)和元素分组values(本例中的Person)之间的映射关系，存在一个Map中
![groupBy](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/groupBy.jpg?raw=true)

结果的类型是Map<Int,List<Person>>,我们可以根据需要使用mapKey和mapValues这样的函数进一步修改Map.


#### 处理嵌套集合中的元素: flatMap和flatten
##### flatMap
flatMap函数根据作为实参给定的函数对集合中的每个元素做变换（映射）; 然后把多个列表合并（平铺）成一个列表.

例如下面的字符串合并例子:
```kotlin
 val strings = listOf("abc","def")
 //输出结果[a,b,c,d,e,f]
 println(strings.flatMap { it.toList() })
```
![flatMap](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/flatMap.jpg?raw=true)

如果想对字符串进行去重操作，可以修改代码如下:
```kotlin
 val strings = listOf("abc","cef")
 //输出结果[a,b,c,e,f]
 println(strings.flatMap { it.toSet() })
```

##### flatten
flatten相比flatMap少了变换（映射）这一步骤，直接将多个列表合并成一个列表.
比如下面的字符串合并例子:
```kotlin
val strings = listOf(listOf("abc","def"), listOf("hij","klm"))
//输出结果[abc, def, hij, klm]
println(strings.flatten())
//输出结果[a, b, c, d, e, f, h, i, j, k, l, m]
println(strings.flatten().flatMap { it.toList() })
```

### 惰性集合操作:序列
在前面我们学习了map、filter等函数,掌握了使用链式调用快速处理集合.但是这些函数都会及早地创建中间集合来存放处理结果,也就是说我们每一步的中间结果其实都有中间集合被创建出来。序列就是为了避免创建这些中间对象.

回顾之前我们的例子：
```kotlin
val list = listOf(1, 2, 3, 4)
println(list.filter { it % 2 == 0 }.map { it * it })
```
这个链式调用会产生2个列表来存储filter和map的结果，如果我们的列表数量较小，影响可能我们感觉不到，但是如果这个列表有上百万的元素，这个调用就会变得很十分低效。

为了提高效率，我们可以把操作变成使用序列，而不是直接操作使用集合。
```kotlin
list.asSequence()
  .filter { it % 2 == 0 }
  .map { it * it }
  .toList()
```
这样处理的结果和前面的例子结果一样，但是没有创建任何用于存储元素的中间集合，所以在大数据列表的情况性能提升显著.

Kotlin的惰性集合操作的入口就是Sequence接口。该接口表示的是一个可以逐个列举元素的元素序列. Sequence提供`iterator`方法用来获取序列中的元素.

那为什么使用Sequence不会产生中间集合呢？
我们来对比一个使用普通的链式调用和Sequence的处理过程有什么区别，看下图
![Sequence](https://github.com/brook-joker/Kotlin-SourceCode/blob/master/blog/resource/Sequence.jpg?raw=true)

经过对比我们可以发现:序列操作集合是将所有的操作顺序应用在每一个元素上，普通的链式调用则是对所有的元素顺序应用每一个操作. 所以这就造成了普通的链式调用需要大量的中间集合来存储操作结果，而序列操作不需要.

那为什么需要把序列处理完以后需要转换回集合？使用序列替代集合不是很方便？序列还有那么多有优势

有时候确实是这样，如果你只需要迭代序列中的元素，可以直接使用序列。但是如果你需要其他API的方法，比如使用下标访问元素，那么你需要转换成列表.或者你需要对序列去重，那么你需要转换成Set.



### 对象和基本类型的数组
Kotlin中的数组是持有类型参数的类，其元素类型被指定为相应的类型参数，使用以下方式创建数组：
- arrayOf : 包含的元素是指定为该函数的实参
- arrayOfNulls :创建一个给定大小的数组，包含的是null元素，当然，它只能用来创建包含元素类型可空的数组
- Array : 构造方法接收数组的大小和一个lambda表达式，调用lambda表达式来创建每一个数组元素，这就是使用非空元素类型来初始化数组，但不用显示地传递每个元素的方式

```kotlin
//如果包含的元素中有null 数组会自动转换成可空性数组 否则为非空性数组
val array1 = arrayOf(1, 2, 3)
//可以持有null值
val array2 = arrayOfNulls<Int>(3)
array2[0] = null
array2[1] = 1
array2[2] = 2
// 1，2，3
val array3 = Array<Int>(3) { i ->
  i + 1
}
```

Array<T>创建的数组是一个包含装箱类型的数组，那么如果我们需要创建一个基本类型的数组怎么做呢? Kotlin提供了一组基本数据类型数组的特殊类, 例如Int类型值的数组为IntArray.
创建方式如下：
```kotlin
//第一种创建方式
val array1 = IntArray(1)
array1[0] = 1
//第二种创建方式
val array2 = intArrayOf(1,2,3)
//第三种创建方式
val array3 = IntArray(3) { i -> i + 1 }
```



