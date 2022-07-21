package com.sox.api.utils;

import java.lang.reflect.Method;

/**
 * 为强制类型转换时不被警告
 */
public interface CastUtils {
    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    @SuppressWarnings("unchecked")
    static <T> T call(Object object, String method_name, Object... params) {
        try {
            Class<?> o_class = object.getClass();
            Object o_instance = o_class.newInstance();

            Class<?>[] p_class = new Class[params.length];

            for (int i = 0;i < params.length;i++) {
                p_class[i] = params[i].getClass();
            }

            Method method = o_class.getMethod(method_name, p_class);

            return (T) method.invoke(o_instance, params);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
