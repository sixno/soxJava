package com.sox.api.utils;

/**
 * 匿名回调
 * 可使用内部类实现丰富的参数类型
 */
public interface CallbackUtils<T> {
    void deal(T input) throws Exception;
}

