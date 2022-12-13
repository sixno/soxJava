package com.sox.api.service;

import com.sox.api.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Check {
    @Autowired
    private Db db;

    public Map<String, String> err_tp = new LinkedHashMap<String, String>(){{
        put("required",     "%s为必填项");
        put("min_length",   "%s至少%s位");
        put("max_length",   "%s最多%s位");
        put("exact_length", "%s必须%s位");
        put("is_unique",    "%s已存在");
    }};

    public final ThreadLocal<Map<String, String>> data = ThreadLocal.withInitial(LinkedHashMap::new);

    public final ThreadLocal<String> error = ThreadLocal.withInitial(() -> "");

    public final ThreadLocal<Boolean> result = ThreadLocal.withInitial(() -> true);

    public final ThreadLocal<Map<String, String>> errors = ThreadLocal.withInitial(LinkedHashMap::new);

    public void reset() {
        result.set(true);

        errors.set(new LinkedHashMap<>());

        error.set("");

        data.set(new LinkedHashMap<>());
    }

    public void reset(Map<String, String> input) {
        this.reset();

        data.set(input);
    }

    public boolean required(String value, String... other) {
        boolean current = !value.equals("");

        if (!current && other.length > 0) {
            this.set_error("required", other);

            result.set(false);
        }

        return current;
    }

    public boolean min_length(String value, Integer num, String... other) {
        if (other.length > 0 && value.equals("")) return true;

        boolean current = value.length() >= num;

        if (other.length == 1) other[0] += "," + num;

        if (!current && other.length > 0) {
            this.set_error("min_length", other);

            result.set(false);
        }

        return current;
    }

    public boolean max_length(String value, Integer num, String... other) {
        if (other.length > 0 && value.equals("")) return true;

        boolean current = value.length() <= num;

        if (other.length == 1) other[0] += "," + num;

        if (!current && other.length > 0) {
            this.set_error("max_length", other);

            result.set(false);
        }

        return current;
    }

    public boolean exact_length(String value, Integer num, String... other) {
        if (other.length > 0 && value.equals("")) return true;

        boolean current = value.length() == num;

        if (other.length == 1) other[0] += "," + num;

        if (!current && other.length > 0) {
            this.set_error("exact_length", other);

            result.set(false);
        }

        return current;
    }

    public boolean is_unique(String value, String table, String field, String... other) {
        if (other.length > 0 && value.equals("")) return true;

        String[] which = field.split("#");

        Map<String, Object> map = new LinkedHashMap<>();

        map.put(which[0], value);

        if (which.length > 1) map.put("id !=", which[1]);

        boolean current = !(db.table(table).count(map) > 0) || value.equals("");

        if (!current && other.length > 0) {
            this.set_error("is_unique", other);

            result.set(false);
        }

        return current;
    }

    public boolean numeric(String value, String... other) {
        Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)?");

        Matcher matcher = pattern.matcher(value);

        boolean current = matcher.matches();

        if (!current && other.length > 0) {
            this.set_error("numeric", other);

            result.set(false);
        }

        return current;
    }

    public boolean integer(String value, String... other) {
        Pattern pattern = Pattern.compile("-?[0-9]+");

        Matcher matcher = pattern.matcher(value);

        boolean current = matcher.matches();

        if (!current && other.length > 0) {
            this.set_error("integer", other);

            result.set(false);
        }

        return current;
    }

    public boolean decimal(String value, String... other) {
        Pattern pattern = Pattern.compile("-?[0-9]+(\\.[0-9]+)+");

        Matcher matcher = pattern.matcher(value);

        boolean current = matcher.matches();

        if (!current && other.length > 0) {
            this.set_error("decimal", other);

            result.set(false);
        }

        return current;
    }

    public boolean is_natural(String value, String... other) {
        Pattern pattern = Pattern.compile("[0-9]+");

        Matcher matcher = pattern.matcher(value);

        boolean current = matcher.matches();

        if (!current && other.length > 0) {
            this.set_error("is_natural", other);

            result.set(false);
        }

        return current;
    }

    public boolean is_natural_no_zero(String value, String... other) {
        value = value.replaceAll("^(0+)", "");

        Pattern pattern = Pattern.compile("[1-9]+[0-9]*");

        Matcher matcher = pattern.matcher(value);

        boolean current = matcher.matches();

        if (!current && other.length > 0) {
            this.set_error("is_natural_no_zero", other);

            result.set(false);
        }

        return current;
    }

    private void set_error(String rule_name, String[] other) {
        if (other.length == 1) {
            Object[] field = other[0].split(",");
            String   value = String.format(err_tp.get(rule_name), field);

            if (error.get().equals("")) error.set(value);

            errors.get().putIfAbsent(field[0].toString(), value);
        } else if (other.length == 2) {
            if (error.get().equals("")) error.set(other[1]);

            errors.get().putIfAbsent(other[0], other[1]);
        }
    }

    public void validate(String key, String field, String rules, String... ext) {
        String def = "";

        Map<String, String> tpl = new LinkedHashMap<>();

        String[] rule_arr = rules.split("\\|");

        if (ext.length > 0)
        {
            if (ext.length % 2 == 1) {
                def = ext[0].equals("@@NULL") ? null : ext[0];

                for (int i = 1;i < ext.length;i += 2)
                {
                    tpl.put(ext[i], ext[i + 1]);
                }
            } else {
                def = "";

                for (int i = 0;i < ext.length;i += 2)
                {
                    tpl.put(ext[i], ext[i + 1]);
                }
            }
        }

        String value = key.equals("") ? def : data.get().getOrDefault(key, def);

        if (value == null) return ;

        for (String rule : rule_arr) {
            String[] arg = rule.split(",");

            List<String> other_arr = new ArrayList<>();

            other_arr.add(field);

            if (tpl.get(rule) != null) {
                other_arr.add(tpl.get(rule));
            }

            String[] other = other_arr.toArray(new String[0]);

            try {
                switch (arg[0]) {
                    case "required":
                        CastUtils.call(this, arg[0], value, other);
                        break;

                    case "min_length":
                    case "max_length":
                    case "exact_length":
                        CastUtils.call(this, arg[0], value, Integer.parseInt(arg[1]), other);
                        break;

                    case "is_unique":
                        CastUtils.call(this, arg[0], value, arg[1], arg[2], other);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
