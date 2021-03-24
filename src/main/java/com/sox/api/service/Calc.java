package com.sox.api.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class Calc {
    public String add(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.add(b2).toString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String sub(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.subtract(b2).toString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String mul(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.multiply(b2).toString();

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

        String result = b1.divide(b2, len.length > 0 ? len[0] : 10, BigDecimal.ROUND_HALF_UP).toString();

        return len.length > 0 ? result : result.replaceAll("(0)+$", "");
    }

    public String pow(Object n, int num, int... len) {
        BigDecimal b1 = new BigDecimal(n.toString());

        String result = b1.pow(num).toString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String max(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.max(b2).toString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String min(Object n1, Object n2, int... len) {
        BigDecimal b1 = new BigDecimal(n1.toString());
        BigDecimal b2 = new BigDecimal(n2.toString());

        String result = b1.min(b2).toString();

        if (len.length == 0) {
            return result;
        } else {
            return this.round(result, len[0]);
        }
    }

    public String round(Object n, int len){
        BigDecimal b1 = new BigDecimal(n.toString()) ;
        BigDecimal b2 = new BigDecimal(1) ;

        return b1.divide(b2, len, BigDecimal.ROUND_HALF_UP).toString() ;
    }
}
