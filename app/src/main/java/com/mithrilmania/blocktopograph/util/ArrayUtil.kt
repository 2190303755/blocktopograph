package com.mithrilmania.blocktopograph.util

import java.lang.reflect.Array

inline fun <reified A : Any> A.expandAt(
    index: Int,
    factory: (Int) -> A
): A {
    val length = Array.getLength(this)
    if (index !in 0..length) {
        throw IndexOutOfBoundsException("Index: $index, Length: $length")
    }
    val result = factory(length + 1)
    if (index > 0) {
        System.arraycopy(this, 0, result, 0, index)
    }
    if (index < length) {
        System.arraycopy(this, index, result, index + 1, length - index)
    }
    return result
}

inline fun <reified A : Any> A.shrinkAt(
    index: Int,
    factory: (Int) -> A
): A {
    val length = Array.getLength(this)
    if (index !in 0..<length) {
        throw IndexOutOfBoundsException("Index: $index, Length: $length")
    }
    val result = factory(length - 1)
    System.arraycopy(this, 0, result, 0, index)
    if (index < length - 1) {
        System.arraycopy(this, index + 1, result, index, length - index - 1)
    }
    return result
}

private inline fun <T : Any> T.add(
    index: Int,
    length: (T) -> Int,
    factory: (Int) -> T,
    action: (T) -> Unit
): T {
    val size = length(this)
    if (index !in 0..size) throw IndexOutOfBoundsException()
    return factory(size + 1).also {
        System.arraycopy(this, 0, it, 0, index)
        action(it)
        if (index < size) {
            System.arraycopy(this, index, it, index + 1, size - index)
        }
    }
}

private inline fun <T : Any> T.removeAt(
    index: Int,
    length: (T) -> Int,
    factory: (Int) -> T
): T {
    val size = length(this)
    if (index !in 0 until size) throw IndexOutOfBoundsException()
    return factory(size - 1).also {
        System.arraycopy(this, 0, it, 0, index)
        if (index < size - 1) {
            System.arraycopy(this, index + 1, it, index, size - index - 1)
        }
    }
}

fun ByteArray.add(index: Int, value: Byte) =
    this.add(index, ByteArray::size, ::ByteArray) {
        this[index] = value
    }

fun IntArray.add(index: Int, value: Int) =
    this.add(index, IntArray::size, ::IntArray) {
        this[index] = value
    }

fun LongArray.add(index: Int, value: Long) =
    this.add(index, LongArray::size, ::LongArray) {
        this[index] = value
    }

fun ByteArray.removeAt(index: Int) =
    this.removeAt(index, ByteArray::size, ::ByteArray)

fun IntArray.removeAt(index: Int) =
    this.removeAt(index, IntArray::size, ::IntArray)

fun LongArray.removeAt(index: Int) =
    this.removeAt(index, LongArray::size, ::LongArray)