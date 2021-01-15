package com.sox.api.interceptor;

import com.sox.api.service.Api;
import com.sox.api.service.Com;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class AlwaysInterceptor implements HandlerInterceptor {
    @Autowired
    private Api api;

    @Autowired
    private Com com;

    long start = System.currentTimeMillis();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        start = System.currentTimeMillis();

        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Methods", "GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS");
        response.setHeader("Access-Control-Max-Age", "86400");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Token");

        if (HttpMethod.OPTIONS.toString().equals(request.getMethod())) {
            response.setStatus(HttpStatus.NO_CONTENT.value());

            return false;
        }

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;

            // 登录拦截
            CheckLogin checkLogin = handlerMethod.getMethod().getAnnotation(CheckLogin.class);

            if (checkLogin == null) {
                checkLogin = handlerMethod.getMethod().getDeclaringClass().getAnnotation(CheckLogin.class);
            }

            if (checkLogin != null) {
                if (!("," + checkLogin.except() + ",").contains("," + handlerMethod.getMethod().getName() + ",")) {
                    if (!com.check_login()) {
                        api.set("out", "0");
                        api.set("msg", "您尚未登录");

                        api.set("code", "1000");

                        api.output();

                        return false;
                    }
                }
            }

            // 权限拦截
            CheckAuth checkAuth = handlerMethod.getMethod().getAnnotation(CheckAuth.class);

            if (checkAuth == null) {
                checkAuth = handlerMethod.getMethod().getDeclaringClass().getAnnotation(CheckAuth.class);
            }

            if (checkAuth != null) {
                if (!("," + checkAuth.except() + ",").contains("," + handlerMethod.getMethod().getName() + ",")) {
                    String index = checkAuth.index();
                    String value = checkAuth.value();

                    if (!com.check_auth(index, value)) {
                        api.set("out", "0");
                        api.set("msg", "您权限不够");

                        api.set("code", "1001");

                        api.output();

                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView mav) {
        api.req.remove();

        System.out.println("Interceptor cost: " + (System.currentTimeMillis() - start) + "ms");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception e) {
    }
}
