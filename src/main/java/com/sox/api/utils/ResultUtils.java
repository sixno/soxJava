package com.sox.api.utils;

/**
 * 有返回值回调，接受一个输入类型，对应一个输出类型，由于Java的引用传递特性，这种回调的使用场景几乎没有
 * @param <I>
 * @param <O>
 */
public interface ResultUtils<I, O> {
    O deal(I input) throws Exception;
}
