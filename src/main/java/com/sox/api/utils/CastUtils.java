package com.sox.api.utils;

/**
 * 强制类型转换
 * 方法动态调用
 */
public interface CastUtils {
    @SuppressWarnings("unchecked")
    static <T> T cast(Object object) {
        return (T) object;
    }

    @SuppressWarnings("unchecked")
    static <T> T call(Object object, String method_name, Object... params) {
        try {
            Class<?>[] classes = new Class[params.length];

            for (int i = 0;i < params.length;i++) {
                classes[i] = params[i].getClass();
            }

            return (T) object.getClass().getMethod(method_name, classes).invoke(object, params);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
