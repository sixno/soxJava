package com.sox.api.utils;

/**
 * 无返回值回调，接受两个参数，通常为了减少变量类型转换而使用
 * @param <T1>
 * @param <T2>
 */
public interface CallbackUtils_2<T1, T2> {
    void deal(T1 input_1, T2 input_2) throws Exception;
}
