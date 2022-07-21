package com.sox.api.utils;

/**
 * 无返回值回调，接受一个参数，大部分情况下够用
 * @param <T>
 */
public interface CallbackUtils<T> {
    void deal(T input) throws Exception;
}
