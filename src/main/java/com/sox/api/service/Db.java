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

        List<?> resultList = nativeQuery.getResultList();

        em.clear();

        return resultList.get(0) != null ? resultList.get(0).toString() : "";
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

        List<?> resultList = nativeQuery.getResultList();

        em.clear();

        if (resultList != null) {
            for (Object resultItem : resultList) {
                Map<String, String> objectMap = new HashMap<>();

                if (resultItem instanceof Object[]) {
                    Object[] objectList = (Object[])resultItem;

                    for(int i = 0;i < col.size();i++) {
                        objectMap.put(col.get(i), objectList[i] != null ? objectList[i].toString() : "");
                    }
                } else {
                    objectMap.put(col.get(0), resultItem != null ? resultItem.toString() : "");
                }

                list.add(objectMap);
            }
        }

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

        // 为了手动开启事务，必须新建一个entityManager实例作为事务管理器，否则报错共享实例不能手动开启事务
        if(tm == null) tm = em.getEntityManagerFactory().createEntityManager();

        Query nativeQuery = tm.createNativeQuery(sql);

        try {
            tm.getTransaction().begin();
        } catch (Exception e) {
            e.printStackTrace();

            System.out.println("Maybe the tm object lost db connection, So rebuild tm");

            // 关闭无效事务管理器并销毁
            tm.close();
            tm = null;

            tm = em.getEntityManagerFactory().createEntityManager();

            nativeQuery = tm.createNativeQuery(sql);

            tm.getTransaction().begin();
        }

        try {
            result = nativeQuery.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }

        tm.getTransaction().commit();

        if (sql.indexOf("INSERT INTO") == 0 && m_count == 2) {
            nativeQuery = tm.createNativeQuery("SELECT @@IDENTITY AS 'Identity'");

            List<?> id = nativeQuery.getResultList();

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

            val = obj == null ? "" : obj.toString();

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
                if (obj instanceof ArrayList<?>) {
                    val = "";

                    for (Object str : (List<?>) obj) {
                        val += str.toString() + ",";
                    }

                    if(!val.equals("")) val = val.substring(0, val.length() - 1);
                }

                val = "('" + val.replace(",", "','") + "')";
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

    public int count(String... fv) {
        Map<String, Object> where = new HashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            where.put(fv[i], fv[i + 1]);
        }

        return this.count(where);
    }

    public List<Map<String, Object>> total(Map<String, Object> map) {
        int count = this.count(map);

        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> count_map = new HashMap<>();

        count_map.put("count", count);

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
            List<Object[]> cols = this.cols(this.table());

            field = "";

            for (Object[] col : cols) {
                field += col[0].toString() + ",";
            }

            field = field.substring(0, field.length() - 1);
        } else if (field.contains("*")) {
            String[] field_arr = field.split(",");

            field = "";

            for (String field_str : field_arr) {
                if (field_str.contains("*")) {
                    String[] field_str_arr = field_str.split("\\.");

                    List<Object[]> cols = this.cols(field_str_arr[0]);

                    for (Object[] col : cols) {
                        field += (field.equals("") ? "" : ",") + field_str_arr[0] + "." + col[0].toString();
                    }
                } else {
                    field += (field.equals("") ? "" : ",") + field_str;
                }
            }
        }

        field = field.replace(".","`.`");
        field = field.replaceAll("(?i) as ","` AS `");
        field = field.replace(",", "`,`");
        field = "`" + field + "`";

        // field = field.replace("`*`","*");

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

    public List<Map<String, String>> read(String field, String... fv) {
        Map<String, Object> where = new HashMap<>();

        where.put("#field", field);

        if (fv.length % 2 == 0) {
            for (int i = 0;i < fv.length;i += 2) {
                where.put(fv[i], fv[i + 1]);
            }
        } else {
            where.put("#order", fv[0]);

            for (int i = 1;i < fv.length;i += 2) {
                where.put(fv[i], fv[i + 1]);
            }
        }

        return this.read(where);
    }

    public Map<String, String> find(Map<String, Object> map) {
        map.putIfAbsent("#limit", "1,0");

        List<Map<String, String>> list = this.read(map);

        return list.size() == 0 ? new HashMap<>() : list.get(0);
    }

    public Map<String, String> find(String field, String... fv) {
        Map<String, Object> map = new HashMap<>();

        map.put("#field", field);

        if (fv.length == 1) {
            if (!fv[0].contains(",")) {
                map.put("id", fv[0]);
            } else {
                map.put("#order", fv[0]);
            }
        } else {
            if (fv.length % 2 == 0) {
                for (int i = 0;i < fv.length;i += 2) {
                    map.put(fv[i], fv[i + 1]);
                }
            } else {
                map.put("#order", fv[0]);

                for (int i = 1;i < fv.length;i += 2) {
                    map.put(fv[i], fv[i + 1]);
                }
            }
        }

        return this.find(map);
    }

    public String field(String field, Map<String, Object> where) {
        where.put("#field", field);

        Map<String, String> item = this.find(where);

        for (String col : item.keySet()) {
            return item.get(col);
        }

        return "";
    }

    public String field(String field, String... fv) {
        Map<String, Object> map = new HashMap<>();

        if (fv.length == 1) {
            if (!fv[0].contains(",")) {
                map.put("id", fv[0]);
            } else {
                map.put("#order", fv[0]);
            }
        } else {
            if (fv.length % 2 == 0) {
                for (int i = 0;i < fv.length;i += 2) {
                    map.put(fv[i], fv[i + 1]);
                }
            } else {
                map.put("#order", fv[0]);

                for (int i = 1;i < fv.length;i += 2) {
                    map.put(fv[i], fv[i + 1]);
                }
            }
        }

        return this.field(field, map);
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

    public int create(String... fv) {
        Map<String, String> data = new HashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            data.put(fv[i], fv[i + 1]);
        }

        return data.size() > 0 ? this.create(data) : 0;
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

    public int update(String field, Object value, Map<String, String> data) {
        Map<String, Object> where = new HashMap<>();

        where.put(field, value);

        return this.update(where, data);
    }

    public int update(String id, String... fv) {
        Map<String, Object> where = new HashMap<>();

        if (fv.length % 2 == 0) {
            where.put("id", id);
        } else {
            where.put(id, fv[0]);
        }

        Map<String, String> data = new HashMap<>();

        for (int i = fv.length % 2;i < fv.length;i += 2) {
            data.put(fv[i], fv[i + 1]);
        }

        return data.size() > 0 ? this.update(where, data) : 0;
    }

    public int update(int where_num, String... fv) {
        Map<String, Object> where = new HashMap<>();
        Map<String, String> data  = new HashMap<>();

        int count_num = 0;

        for (int i = 0;i < fv.length;i += 2) {
            if(count_num < where_num) {
                where.put(fv[i], fv[i + 1]);

                count_num++;
            } else {
                data.put(fv[i], fv[i + 1]);
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

    public List<Object[]> cols(String... table) {
        List<Object[]> list = new ArrayList<>();

        String sql = "SHOW COLUMNS FROM ";

        if (table.length == 0) {
            sql += this.table();

            this.restore_table();
        } else {
            sql += table[0];
        }

        Query nativeQuery = em.createNativeQuery(sql);

        List<?> resultList = nativeQuery.getResultList();

        em.clear();

        if (resultList != null) {
            for (Object resultItem : resultList) {
                if (resultItem instanceof Object[]) {
                    list.add((Object[]) resultItem);
                }
            }
        }

        return list;
    }

    public List<String> tbls() {
        List<String> list = new ArrayList<>();

        String sql = "SHOW TABLES";

        Query nativeQuery = em.createNativeQuery(sql);

        List<?> resultList = nativeQuery.getResultList();

        em.clear();

        if (resultList != null) {
            for (Object resultItem : resultList) {
                if (resultItem instanceof String) {
                    list.add((String) resultItem);
                }
            }
        }

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
