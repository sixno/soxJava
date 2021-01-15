package com.sox.api.service;

import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Db implements Cloneable{
    @PersistenceContext
    protected EntityManager em;

    private final ThreadLocal<String> o_tbl = new ThreadLocal<>();

    public String table = "";

    public Db table(String tbl) {
        this.o_tbl.set(tbl);

        return this;
    }

    public String table() {
        if (this.o_tbl.get() == null) {
            return this.table;
        } else {
            return this.o_tbl.get();
        }
    }

    public void restore_table() {
        if (this.o_tbl.get() != null) this.o_tbl.remove();
    }

    public String single(String sql) {
        this.restore_table();

        Query nativeQuery = em.createNativeQuery(sql);

        List<Object> resultList = nativeQuery.getResultList();

        String result = resultList.get(0).toString();

        em.clear();

        return result;
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

        int int_id = 0;

        Pattern p = Pattern.compile("( \\()");
        Matcher m = p.matcher(sql);

        int m_count = 0;

        while(m.find()){
            m_count++;
        }

        Query nativeQuery = em.createNativeQuery(sql);

        int result = nativeQuery.executeUpdate();

        if (sql.indexOf("INSERT INTO") == 0 && m_count == 2) {
            nativeQuery = em.createNativeQuery("SELECT @@IDENTITY AS 'Identity'");

            List<Object> id = nativeQuery.getResultList();

            int_id = Integer.parseInt(id.get(0).toString());
        }

        em.clear();

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

        sql += this.where(map);

        String result = this.single(sql);

        return Integer.parseInt(result);
    }

    public List<Map<String, String>> read(Map<String, Object> map) {
        String sql = "";

        String field = map.getOrDefault("#field","").toString();
        String unite = map.getOrDefault("#unite","").toString();
        String order = map.getOrDefault("#order","").toString();
        String limit = map.getOrDefault("#limit","").toString();

        if (!field.equals(""))
        {
            field = field.replace(".","`.`");
            field = field.replaceAll("(?i) as ","` AS `");
            field = field.replace(",", "`,`");
            field = "`" + field + "`";

            field = field.replace("`*`","*");

            field = field.replaceAll("(?i)`distinct ", "DISTINCT `");

            sql = "SELECT " + field + " FROM `" + this.table() + "` ";
        } else {
            sql = "SELECT * FROM `" + this.table() + "` ";
        }

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
            order = order.replace(" ", "");
            order = order.replaceAll("(?i)asc", "ASC");
            order = order.replaceAll("(?i)desc", "DESC");
            order = order.replace(".", "`.`");
            order = order.replace(",", "` ");
            order = order.replace(";", "`,");

            sql += "ORDER BY `" + order + " ";

            sql = sql.replaceAll("(?i)`rand\\(\\)", "RAND()");
            sql = sql.replaceAll("(?i)rand\\(\\)`", "RAND()");
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
        if (map.get("#limit") == null) map.put("#limit", "1,0");

        List list = this.read(map);

        return list.size() == 0 ? null : (Map<String, String>) list.get(0);
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

    public int create(List<Map> list) {
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
        String sql = "UPDATE `" + this.table() + "` SET ";

        Set<String> keys = data.keySet();

        for (String key : keys) {
            sql += "`" + key + "` = '" + this.escape(data.get(key)) + "',";
        }

        sql = sql.substring(0, sql.length() - 1) + " ";

        sql += this.where(where);

        int result = this.action(sql);

        return result;
    }

    public int update(String id,Map<String, String> data) {
        Map<String, Object> where = new HashMap<>();

        where.put("id", id);

        return this.update(where, data);
    }

    public int update(String id, String field, String... data_val) {
        Map<String, Object> where = new HashMap<>();

        where.put("id", id);

        Map<String, String> data = new HashMap<>();

        String[] data_key = field.split(",");

        for (int i = 0;i < data_key.length;i++) {
            if (i < data_val.length) {
                data.put(data_key[i], data_val[i]);
            } else {
                data.put(data_key[i], "");
            }
        }

        return this.update(where, data);
    }

    public int delete() {
        return 0;
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

    public void cols() {
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
