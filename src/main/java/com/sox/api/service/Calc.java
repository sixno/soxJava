package com.sox.api.service;

import org.nfunk.jep.JEP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Calc {
    @Autowired
    private Log log;

    public String add(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.add(b2).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String sub(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.subtract(b2).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String mul(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.multiply(b2).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String div(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        if (b2.equals(new BigDecimal(0))) return "";

        String result = b1.divide(b2, len.length > 0 ? len[0] : 10, BigDecimal.ROUND_HALF_UP).toPlainString();

        return len.length > 0 ? result : result.replaceAll("(0)+$", "");
    }

    public String pow(Object n, int num, int... len) {
        BigDecimal b1 = new BigDecimal(n.toString());

        String result = b1.pow(num).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String max(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.max(b2).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String min(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.min(b2).toPlainString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String jep(String exp, Map<String, Object> arg, int... len) {
        // 注意：公式（exp）中，变量名只能包含字母、数字、下划线且不能全是数字
        JEP jep = new JEP();

        for (String field : arg.keySet()) {
            if (arg.get(field) == null) {
                jep.addVariable(field, 0);
            } else if (arg.get(field) instanceof String) {
                String v = (String) arg.get(field);

                jep.addVariable(field, Double.parseDouble(v.equals("") ? "0" : v));
            } else if(arg.get(field) instanceof Integer) {
                jep.addVariable(field, (int) arg.get(field));
            } else {
                jep.addVariable(field, (double) arg.get(field));
            }
        }

        jep.parseExpression(exp);

        double result_d = jep.getValue();

        String result =  !Double.isNaN(result_d) && !Double.isInfinite(result_d) ? BigDecimal.valueOf(result_d).toString() : "";



        String res = len.length > 0 ? this.round(result, len[0]) : result;

        log.msg("exp: " + exp, 3);
        log.msg("arg: " + arg.toString(), 3);
        log.msg("res: " + res, 3);

        return res;
    }

    public String exp(String exp, Object... arg) {
        int len = 0;

        Map<String, Object> map = new LinkedHashMap<>();

        if (arg.length % 2 == 1) {
            len = (int) arg[0];
        }

        for (int i = arg.length % 2;i < arg.length;i += 2) {
            map.put(arg[i].toString(), arg[i + 1]);
        }

        return this.jep(exp, map, len);
    }

    public String round(Object n, int len){
        if (n.equals("")) return "";

        BigDecimal b1 = new BigDecimal(n.toString()) ;
        BigDecimal b2 = new BigDecimal(1) ;

        return b1.divide(b2, len, BigDecimal.ROUND_HALF_UP).toPlainString() ;
    }
}
