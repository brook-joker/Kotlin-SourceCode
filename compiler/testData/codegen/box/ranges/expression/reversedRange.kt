// TODO: muted automatically, investigate should it be ran for JS_IR or not
// IGNORE_BACKEND: JS_IR

// TODO: muted automatically, investigate should it be ran for JVM_IR or not
// IGNORE_BACKEND: JVM_IR

// Auto-generated by org.jetbrains.kotlin.generators.tests.GenerateRangesCodegenTestData. DO NOT EDIT!
// WITH_RUNTIME



fun box(): String {
    val list1 = ArrayList<Int>()
    val range1 = (3..5).reversed()
    for (i in range1) {
        list1.add(i)
        if (list1.size > 23) break
    }
    if (list1 != listOf<Int>(5, 4, 3)) {
        return "Wrong elements for (3..5).reversed(): $list1"
    }

    val list2 = ArrayList<Int>()
    val range2 = (3.toShort()..5.toShort()).reversed()
    for (i in range2) {
        list2.add(i)
        if (list2.size > 23) break
    }
    if (list2 != listOf<Int>(5, 4, 3)) {
        return "Wrong elements for (3.toShort()..5.toShort()).reversed(): $list2"
    }

    val list3 = ArrayList<Long>()
    val range3 = (3L..5L).reversed()
    for (i in range3) {
        list3.add(i)
        if (list3.size > 23) break
    }
    if (list3 != listOf<Long>(5, 4, 3)) {
        return "Wrong elements for (3L..5L).reversed(): $list3"
    }

    val list4 = ArrayList<Char>()
    val range4 = ('a'..'c').reversed()
    for (i in range4) {
        list4.add(i)
        if (list4.size > 23) break
    }
    if (list4 != listOf<Char>('c', 'b', 'a')) {
        return "Wrong elements for ('a'..'c').reversed(): $list4"
    }

    val list5 = ArrayList<UInt>()
    val range5 = (3u..5u).reversed()
    for (i in range5) {
        list5.add(i)
        if (list5.size > 23) break
    }
    if (list5 != listOf<UInt>(5u, 4u, 3u)) {
        return "Wrong elements for (3u..5u).reversed(): $list5"
    }

    val list6 = ArrayList<UInt>()
    val range6 = (3u.toUShort()..5u.toUShort()).reversed()
    for (i in range6) {
        list6.add(i)
        if (list6.size > 23) break
    }
    if (list6 != listOf<UInt>(5u, 4u, 3u)) {
        return "Wrong elements for (3u.toUShort()..5u.toUShort()).reversed(): $list6"
    }

    val list7 = ArrayList<ULong>()
    val range7 = (3uL..5uL).reversed()
    for (i in range7) {
        list7.add(i)
        if (list7.size > 23) break
    }
    if (list7 != listOf<ULong>(5u, 4u, 3u)) {
        return "Wrong elements for (3uL..5uL).reversed(): $list7"
    }

    return "OK"
}