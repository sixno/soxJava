package com.sox.api.service;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Db implements Cloneable{
    @PersistenceContext
    protected EntityManager em;

    protected EntityManager tm;

    private final ThreadLocal<String> o_tbl = new ThreadLocal<>();

    public String table = "";

    public Db table(String tbl) {
        o_tbl.set(tbl);

        return this;
    }

    public String table() {
        if (o_tbl.get() == null) {
            return table;
        } else {
            return o_tbl.get();
        }
    }

    public void restore_table() {
        o_tbl.remove();
    }

    public String single(String sql) {
        this.restore_table();

        Query nativeQuery = em.createNativeQuery(sql);

        List<Object> resultList = nativeQuery.getResultList();

        String result = resultList.get(0) != null ? resultList.get(0).toString() : "";

        em.clear();

        return result;
    }

    public List<Map<String, String>> single_col(String sql) {
        this.restore_table();

        String col = "";
        ArrayList<Map<String, String>> list = new ArrayList<>();

        Pattern p = Pattern.compile("(?i)select (.*?) from");
        Matcher m = p.matcher(sql);

        if (m.find()) {
            String col_str = m.group(1).trim();

            String[] col_arr = col_str.split(",");

            for (String s : col_arr) {
                String[] col_arr_i = s.split(" |\\.");

                col = col_arr_i[col_arr_i.length - 1].replace("`", "");
            }
        }

        Query nativeQuery = em.createNativeQuery(sql);

        List<Object> resultList = nativeQuery.getResultList();

        if (resultList != null) {
            for (Object resultItem : resultList) {
                Map<String, String> objectMap = new HashMap<>();

                objectMap.put(col, resultItem.toString());

                list.add(objectMap);
            }
        }

        return list;
    }

    public List<Map<String, String>> result(String sql) {
        this.restore_table();

        ArrayList<String> col = new ArrayList<>();
        ArrayList<Map<String, String>> list = new ArrayList<>();

        Pattern p = Pattern.compile("(?i)select (.*?) from");
        Matcher m = p.matcher(sql);

        if (m.find()) {
            String col_str = m.group(1).trim();

            String[] col_arr = col_str.split(",");

            for (String s : col_arr) {
                String[] col_arr_i = s.split(" |\\.");

                col.add(col_arr_i[col_arr_i.length - 1].replace("`", ""));
            }
        }

        if (col.size() == 1) return this.single_col(sql);

        Query nativeQuery = em.createNativeQuery(sql);

        List<Object[]> resultList = nativeQuery.getResultList();

        if (resultList != null) {
            for (Object[] resultItem : resultList) {
                Map<String, String> objectMap = new HashMap<>();

                for(int i = 0;i < col.size();i++) {
                    objectMap.put(col.get(i), resultItem[i].toString());
                }

                list.add(objectMap);
            }
        }

        em.clear();

        return list;
    }

    public int action(String sql) {
        this.restore_table();

        int result = 0;
        int int_id = 0;

        Pattern p = Pattern.compile("( \\()");
        Matcher m = p.matcher(sql);

        int m_count = 0;

        while(m.find()){
            m_count++;
        }

        // 为了手动开启事务，必须新建一个entityManager实例，否则报错共享实例不能手动开启事务
        if(tm == null) tm = em.getEntityManagerFactory().createEntityManager();

        Query nativeQuery = tm.createNativeQuery(sql);

        tm.getTransaction().begin();

        try {
            result = nativeQuery.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        tm.getTransaction().commit();

        if (sql.indexOf("INSERT INTO") == 0 && m_count == 2) {
            nativeQuery = tm.createNativeQuery("SELECT @@IDENTITY AS 'Identity'");

            List<Object> id = nativeQuery.getResultList();

            int_id = Integer.parseInt(id.get(0).toString());
        }

        tm.clear();

        if (sql.indexOf("INSERT INTO") == 0 && m_count == 2) {
            return result > 0 ? (int_id > 0 ? int_id : result) : result;
        } else {
            return result;
        }
    }

    public String escape(String str) {
        str = str.replace("'", "\\'");
        str = str.replace("\\","\\\\");
        str = str.replace("\"","\\\"");
        str = str.replace("\r","\\\r");
        str = str.replace("\n","\\\n");

        return str;
    }

    public String where(Map<String, Object> map) {
        if (map == null) return "";

        String sql = "";
        boolean need_logic = false;

        for (String key : map.keySet()) {
            String val = "";
            Object obj = map.get(key);
            String typ = obj.getClass().getName();

            typ = typ.substring(typ.lastIndexOf(".") + 1);

            val = obj.toString();

            String front_char = key.substring(0,1);

            if (front_char.equals("#")) continue;

            if (sql.equals("")) sql = "WHERE ";

            if (front_char.equals("^")) {
                if (val.equals("and (") || val.equals("or (")) {
                    val = need_logic ? val.toUpperCase() : "(";

                    need_logic = false;
                }

                sql += val + " ";

                continue;
            }

            int pos = key.indexOf("#");

            String logic = "";
            String lgc = "";

            if (pos == -1) {
                logic = "and";
            } else {
                logic = key.substring(0,pos);

                pos = key.lastIndexOf("#");
                key = key.substring(pos + 1);
            }

            key = key.replace(" ", "");
            key = key.replace("'", "");
            key = key.replace("\"","");

            String chr = !key.contains("`") ? "`" : "";
            int chl = chr.length();

            key = chr + (!key.contains(".") ? key : (!chr.equals("") ? key.replace(".",chr + "." + chr) : key)) + chr;

            switch (key.substring(key.length() - 1 - chl, key.length() - chl)) {
                case "=":
                    switch (key.substring(key.length() - 2 -chl, key.length() - chl)) {
                        case "<=":
                            key = key.substring(0, key.length() - 2 - chl) + chr + " <= ";
                            break;
                        case ">=":
                            key = key.substring(0, key.length() - 2 - chl) + chr + " >= ";
                            break;
                        case "!=":
                            key = key.substring(0, key.length() - 2 - chl) + chr + " != ";
                            break;
                        default:
                            key = key.substring(0, key.length() - 1 - chl) + chr + " = ";
                            break;
                    }
                    break;
                case "<":
                    key = key.substring(0, key.length() - 1 - chl) + chr + " < ";
                    break;
                case ">":
                    key = key.substring(0, key.length() - 1 - chl) + chr + " > ";
                    break;
                default:
                    key = key.substring(0, key.length() - chl) + chr + " = ";
                    break;
            }

            if (!logic.contains("in")) {
                if (obj != null) {
                    if (logic.contains("like")) {
                        String like = (key.startsWith("%", 1) ? "%" : "_") + (key.substring(key.length()-5).equals("%") ? "%" : "_");

                        switch (like) {
                            case "_%":
                                key = key.substring(0,key.length() - 5) + key.substring(key.length() - 4);
                                val = val.replace("%","\\%") + '%';
                                break;
                            case "%_":
                                key = key.substring(0, 1) + key.substring(2);
                                val = "%" + val.replace("%","\\%");
                                break;
                            default:
                                val = "%" + val.replace("%","\\%") + "%";
                                break;
                        }
                    }

                    val = "'" + this.escape(val) + "'";
                } else {
                    if (!key.contains("!=")) {
                        key = key.replace(" = "," IS ");
                    } else {
                        key = key.replace(" != "," IS NOT ");
                    }

                    val = "NULL";
                }
            } else {
                if (typ.equals("List")) {
                    val = "";
                } else {
                    val = "('" + val.replace(",", "','") + "')";
                }
            }

            switch (logic) {
                case "or":
                    lgc = "OR";
                    break;
                case "like":
                    lgc = "AND";

                    if (key.contains(",")) {
                        key = "CONCAT_WS(' '," + key.replace(",","`,`");
                        key = key.replace(" =", ") =");
                    }

                    key = key.replace(" = ", " LIKE ");
                    break;
                case "or_like":
                    lgc = "OR";

                    if (key.contains(",")) {
                        key = "CONCAT_WS(' '," + key.replace(",","`,`");
                        key = key.replace(" =", ") =");
                    }

                    key = key.replace(" = ", " LIKE ");
                    break;
                case "not_like":
                    lgc = "AND";

                    if (key.contains(",")) {
                        key = "CONCAT_WS(' '," + key.replace(",","`,`");
                        key = key.replace(" =", ") =");
                    }

                    key = key.replace(" = ", " NOT LIKE ");
                    break;
                case "or_not_like":
                    lgc = "OR";

                    if (key.contains(",")) {
                        key = "CONCAT_WS(' '," + key.replace(",","`,`");
                        key = key.replace(" =", ") =");
                    }

                    key = key.replace(" = ", " NOT LIKE ");
                    break;
                case "in":
                    lgc = "AND";

                    if (val.contains(",")) {
                        key = key.replace(" = ", " IN ");
                    } else {
                        val = val.substring(1,val.length() - 1);
                    }
                    break;
                case "or_in":
                    lgc = "OR";

                    if (val.contains(",")) {
                        key = key.replace(" = ", " IN ");
                    } else {
                        val = val.substring(1,val.length() - 1);
                    }
                    break;
                case "not_in":
                    lgc = "AND";

                    if (val.contains(",")) {
                        key = key.replace(" = ", " NOT IN ");
                    } else {
                        key = key.replace(" = ", " != ");
                        val = val.substring(1,val.length() - 1);
                    }
                    break;
                case "or_not_in":
                    lgc = "OR";

                    if (val.contains(",")) {
                        key = key.replace(" = ", " NOT IN ");
                    } else {
                        key = key.replace(" = ", " != ");
                        val = val.substring(1,val.length() - 1);
                    }
                    break;
                default:
                    lgc = "AND";
                    break;
            }

            if (need_logic) {
                sql += lgc + " " + key + val;
            } else {
                need_logic = true;

                sql += key + val;
            }

            sql += " ";
        }

        return sql;
    }

    public String where(String sql_where, String... str_arr) {
        StringBuilder sql = new StringBuilder("WHERE ");

        String[] where = sql_where.split("\\?");

        for (int i = 0;i < where.length;i++) {
            if (i < str_arr.length) {
                sql.append(String.format(where[i] + "%s", this.escape(str_arr[i])));
            } else {
                sql.append(where[i]);
            }
        }

        return sql.toString();
    }

    public String sql(String sql, String... str) {
        String[] sql_arr = sql.split("\\?");

        StringBuilder sqlBuilder = new StringBuilder();

        for (int i = 0; i < sql_arr.length; i++) {
            if (i < str.length) {
                sqlBuilder.append(String.format(sql_arr[i] + "%s", "'" + this.escape(str[i]) + "'"));
            } else {
                sqlBuilder.append(sql_arr[i]);
            }
        }

        return sqlBuilder.toString();
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM `" + this.table() + "` FORCE INDEX(PRIMARY)";

        String result = this.single(sql);

        return Integer.parseInt(result);
    }

    public int count(Map<String, Object> map) {
        String sql = "SELECT COUNT(*) FROM `" + this.table() + "` ";

        String unite = map.getOrDefault("#unite","").toString();

        if (!unite.equals(""))
        {
            String[] unite_arr = unite.split(";");

            for (String unite_str : unite_arr) {
                String[] join_arr = unite_str.split(",");

                join_arr[1] = join_arr[1].replace(" ", "");
                join_arr[1] = join_arr[1].replace(".", "`.`");
                join_arr[1] = join_arr[1].replace("=", "`=`");

                if (join_arr.length == 3) {
                    sql += join_arr[2].toUpperCase() + " JOIN `" + join_arr[0] + "` ON `" + join_arr[1] + "` ";
                } else {
                    sql += "JOIN `" + join_arr[0] + "` ON `" + join_arr[1] + "` ";
                }
            }
        }

        String where = this.where(map);

        sql += where.equals("") ? "FORCE INDEX(PRIMARY)" : where;

        String result = this.single(sql);

        return Integer.parseInt(result);
    }

    public int count(String where_field, String where_value, String... more) {
        Map<String, Object> where = new HashMap<>();

        where.put(where_field, where_value);

        for (int i = 0;i < more.length;i += 2) {
            where.put(more[i], more[i + 1]);
        }

        return this.count(where);
    }

    public List<Map<String, Object>> list_count(Map<String, Object> map) {
        int count = this.count(map);

        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> count_map = new HashMap<>();

        count_map.put("count", count);

        list.add(count_map);

        return list;
    }

    public List<Map<String, String>> count_list(Map<String, Object> map) {
        int count = this.count(map);

        List<Map<String, String>> list = new ArrayList<>();

        Map<String, String> count_map = new HashMap<>();

        count_map.put("count", new BigDecimal(count + "").toString());

        list.add(count_map);

        return list;
    }

    public List<Map<String, String>> read(Map<String, Object> map) {
        String sql = "";

        String field = map.getOrDefault("#field","").toString();
        String unite = map.getOrDefault("#unite","").toString();
        String order = map.getOrDefault("#order","").toString();
        String limit = map.getOrDefault("#limit","").toString();

        if (field.equals("") || field.equals("*")) {
            List<Object[]> cols = this.cols(false);

            field = "";

            for (Object[] col : cols) {
                field += col[0].toString() + ",";
            }

            field = field.substring(0, field.length() - 1);
        }

        field = field.replace(".","`.`");
        field = field.replaceAll("(?i) as ","` AS `");
        field = field.replace(",", "`,`");
        field = "`" + field + "`";

        field = field.replace("`*`","*");

        field = field.replaceAll("(?i)`distinct ", "DISTINCT `");

        sql = "SELECT " + field + " FROM `" + this.table() + "` ";

        if (!unite.equals("")) {
            String[] unite_arr = unite.split(";");

            for (String unite_str : unite_arr) {
                String[] join_arr = unite_str.split(",");

                join_arr[1] = join_arr[1].replace(" ", "");
                join_arr[1] = join_arr[1].replace(".", "`.`");
                join_arr[1] = join_arr[1].replace("=", "`=`");

                if (join_arr.length == 3) {
                    sql += join_arr[2].toUpperCase() + " JOIN `" + join_arr[0] + "` ON `" + join_arr[1] + "` ";
                } else {
                    sql += "JOIN `" + join_arr[0] + "` ON `" + join_arr[1] + "` ";
                }
            }
        }

        sql += this.where(map);

        if (!order.equals("")) {
            if (!order.startsWith("field(")) {
                order = order.replace(" ", "");
                order = order.replaceAll("(?i)asc", "ASC");
                order = order.replaceAll("(?i)desc", "DESC");
                order = order.replace(".", "`.`");
                order = order.replace(",", "` ");
                order = order.replace(";", "`,");

                sql += "ORDER BY `" + order + " ";

                sql = sql.replaceAll("(?i)`rand\\(\\)", "RAND()");
                sql = sql.replaceAll("(?i)rand\\(\\)`", "RAND()");
            } else {
                sql += "ORDER BY FIELD" + order.substring(5) + " ";
            }
        }

        if (!limit.equals("")) {
            String[] limit_arr = limit.split(",");

            if (limit_arr.length == 2) {
                sql += "LIMIT " + limit_arr[1] + "," + limit_arr[0];
            } else {
                sql += "LIMIT 0," + limit_arr[0];
            }
        }

        return this.result(sql);
    }

    public List<Map<String, String>> read(String field) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);

        return this.read(where);
    }

    public List<Map<String, String>> read(String field, int length) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);
        where.put("#limit", length + ",0");

        return this.read(where);
    }

    public List<Map<String, String>> read(String field, int length, int offset) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);
        where.put("#limit", length + "," + offset);

        return this.read(where);
    }

    public List<Map<String, String>> read(String field, String order) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);
        where.put("#order", order);

        return this.read(where);
    }

    public List<Map<String, String>> read(String field, String order, int length) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);
        where.put("#limit", length + ",0");
        where.put("#order", order);

        return this.read(where);
    }

    public List<Map<String, String>> read(String field, String order, int length, int offset) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);
        where.put("#limit", length + "," + offset);
        where.put("#order", order);

        return this.read(where);
    }

    public Map<String, String> find(Map<String, Object> map) {
        map.putIfAbsent("#limit", "1,0");

        List<Map<String, String>> list = this.read(map);

        return list.size() == 0 ? new HashMap<>() : list.get(0);
    }

    public Map<String, String> find(String field, String id) {
        Map<String, Object> map = new HashMap<>();

        map.put("#field", field);
        map.put("id", id);

        return this.find(map);
    }

    public Map<String, String> find(String field, String key, String val) {
        Map<String, Object> map = new HashMap<>();

        map.put("#field", field);
        map.put(key, val);

        return this.find(map);
    }

    public String find(String field) {
        Map<String, Object> map = new HashMap<>();

        String[] arr = field.split("#");

        map.put("#field", arr[0]);

        if (!arr[1].contains(":")) {
            map.put("id", arr[1]);
        } else {
            String[] arr_1 = arr[1].split(":");

            map.put(arr_1[0], arr_1[1]);
        }

        Map<String, String> item = this.find(map);

        for (String col : item.keySet()) {
            return item.get(col);
        }

        return "";
    }

    public int create(Map<String, String> data) {
        String sql = "INSERT INTO `" + this.table() + "` ";

        String field = "";
        String value = "";

        Set<String> keys = data.keySet();

        for (String key : keys) {
            field += "`" + key + "`,";
            value += "'" + this.escape(data.get(key)) + "',";
        }

        sql += "(" + field.substring(0, field.length() - 1) + ") VALUES (" + value.substring(0, value.length() - 1) + ")";

        return this.action(sql);
    }

    public int create(List<Map<String, String>> list) {
        String sql = "INSERT IGNORE INTO `" + this.table() + "` ";

        String field = "";
        String value = "";

        Set<String> keys = list.get(0).keySet();

        for (String key : keys) {
            field += "`" + key + "`,";
        }

        for (Map<String, String> data : list) {
            String tmp = "";

            for (String key : keys) {
                tmp += "'" + this.escape(data.get(key)) + "',";
            }

            value += "(" + tmp.substring(0, tmp.length() - 1) + "), ";
        }

        sql += "(" + field.substring(0, field.length() - 1) + ") VALUES " + value.substring(0, value.length() - 2);

        return this.action(sql);
    }

    public int update(Map<String, Object> where, Map<String, String> data) {
        if (data.size() == 0) {
            this.restore_table();

            return 0;
        }

        String sql = "UPDATE `" + this.table() + "` SET ";

        Set<String> keys = data.keySet();

        for (String key : keys) {
            sql += "`" + key + "` = '" + this.escape(data.get(key)) + "',";
        }

        sql = sql.substring(0, sql.length() - 1) + " ";

        sql += this.where(where);

        return this.action(sql);
    }

    public int update(String id, Map<String, String> data) {
        Map<String, Object> where = new HashMap<>();

        where.put("id", id);

        return this.update(where, data);
    }

    public int update(String id, String dv, Map<String, String> data) {
        Map<String, Object> where = new HashMap<>();

        where.put(id, dv);

        return this.update(where, data);
    }

    public int update(Map<String, String> data, String... wv) {
        Map<String, Object> where = new HashMap<>();

        for (int i = 0;i < wv.length;i += 2) {
            where.put(wv[i], wv[i + 1]);
        }

        return this.update(where, data);
    }

    public int update(String id, String... dv) {
        Map<String, Object> where = new HashMap<>();

        if (dv.length % 2 == 0) {
            where.put("id", id);
        } else {
            where.put(id, dv[0]);
        }

        Map<String, String> data = new HashMap<>();

        for (int i = dv.length % 2;i < dv.length;i += 2) {
            data.put(dv[i], dv[i + 1]);
        }

        return this.update(where, data);
    }

    public int update(int id, String... dv) {
        Map<String, Object> where = new HashMap<>();
        Map<String, String> data  = new HashMap<>();

        int where_num = 0;

        for (int i = 0;i < dv.length;i += 2) {
            if(where_num < id) {
                where.put(dv[i], dv[i + 1]);

                where_num++;
            } else {
                data.put(dv[i], dv[i + 1]);
            }
        }

        return this.update(where, data);
    }

    public int delete(Map<String, Object> where) {
        String sql = "DELETE FROM `" + this.table() + "` ";

        sql += this.where(where);

        return this.action(sql);
    }

    public int delete(String id) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", id);

        return this.delete(map);
    }

    public int increase(Map<String, Object> where, Map<String, String> item, Map<String, String> data) {
        return 0;
    }

    public int increase(Map<String, Object> where, String item, int step) {
        return 0;
    }

    public int increase(Map<String, Object> where, String item, int step, Map<String, String> data) {
        return 0;
    }

    public List<Object[]> cols(boolean... restore_table) {
        String sql = "SHOW COLUMNS FROM " + this.table();

        if (restore_table.length == 0) {
            this.restore_table();
        }

        Query nativeQuery = em.createNativeQuery(sql);

        List<Object[]> list = nativeQuery.getResultList();

        em.clear();

        return list;
    }

    @Override
    public Db clone() {
        Db db = null;

        try{
            db = (Db) super.clone();
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return db;
    }
}
