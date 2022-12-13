package com.sox.api.service;

import com.sox.api.utils.CallbackUtils;
import com.sox.api.utils.CastUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;


@Service
public class Db implements Cloneable{
    @Value("${sox.datasource.db_class}")
    private String db_driver;

    @Value("${sox.datasource.location}")
    private String db_location;

    @Value("${sox.datasource.database}")
    private String db_database;

    @Value("${sox.datasource.username}")
    private String db_username;

    @Value("${sox.datasource.password}")
    private String db_password;

    @Value("${sox.datasource.poolsize}")
    private int db_poolsize;

    @Autowired
    private Com com;

    @Autowired
    private Log log;

    public static class Conn {
        public int p_id; // 连接池标识，若为-1表示用完即闭连接
        public Long time;
        public Connection conn;

        public Conn(Connection connection, int... id) {
            p_id = id.length > 0 ? id[0] : -1;
            time = System.currentTimeMillis() / 1000L;
            conn = connection;
        }

        public void setAutoCommit(boolean bool) throws SQLException {
            conn.setAutoCommit(bool);
        }

        public void close() throws SQLException {
            conn.close();
        }

        public Statement createStatement() throws SQLException {
            return conn.createStatement();
        }

        public void commit() throws SQLException {
            conn.commit();
        }
    }

    public final Map<String, Conn> pool = new LinkedHashMap<>();
    public final Map<String, Boolean> pool_use = new LinkedHashMap<>();

    private Conn new_connection(int... id) {
        Conn connection = null;

        try {
            Class.forName(db_driver);
            connection = new Conn(DriverManager.getConnection(db_location, db_username, db_password), id);
            connection.setAutoCommit(false);
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }

        return connection;
    }

    public Conn get_connection() {
        // 以下是连接池状态信息，架构稳定后可删除或注释该段代码
        // int pool_size = 0; // 连接池中存活连接数
        // int pool_used = 0; // 当前连接使用数量

        // for (String p_id : pool_use.keySet()) {
        //     if (pool_use.get(p_id) != null) {
        //         pool_size++;

        //         if (pool_use.get(p_id)) pool_used++;
        //     }
        // }

        // log.msg("current pool size: " + pool_size, 2);
        // log.msg("current pool used: " + pool_used, 2);
        // 以上是连接池状态信息

        for (int i = 0;i < db_poolsize;i++) {
            String p_id = i + "";

            if (pool_use.get(p_id) != null && !pool_use.get(p_id)) {
                // 在连接池中查找空闲连接
                pool_use.put(p_id, true);
            } else if (pool_use.get(p_id) == null) {
                // 在连接池中创建新连接
                pool_use.put(p_id, true);

                pool.put(p_id, this.new_connection(i));
            } else {
                continue;
            }

            return pool.get(p_id);
        }

        return this.new_connection();
    }

    public void put_connection(Conn connection) {
        if (connection.p_id == -1) {
            try {
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            connection.conn = null;
        } else {
            String p_id = connection.p_id + "";

            pool_use.put(p_id, false);

            if (com.time() - connection.time > 3600) {
                pool_use.put(p_id, null);

                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                connection.conn = null;
            }
        }
    }

    private final ThreadLocal<String> o_tbl = new ThreadLocal<>();

    public final ThreadLocal<String> last_sql = ThreadLocal.withInitial(() -> "");
    public final ThreadLocal<String> last_err = ThreadLocal.withInitial(() -> "");
    public final ThreadLocal<Long> affect_rows = ThreadLocal.withInitial(() -> 0L);
    public final ThreadLocal<String> insert_id = ThreadLocal.withInitial(() -> "");

    public final String key_esc = "`";
    public final String val_esc = "\\"; // 注意在 ORACLE 中转义字符为单引号（'）
    public final String key_esc(String str, String... esc) {
        str = str.replace(key_esc, "");

        if (esc.length > 0 && !esc[0].equals("")) {
            for (String e : esc) {
                str = str.replace(e, key_esc + e + key_esc);
            }
        }

        return esc.length > 0 && esc[0].equals("") ? str : key_esc + str + key_esc;
    }

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

    public String sql(String tpl, Object... arg) {
        for (int i = 0;i < arg.length;i++) {
            arg[i] = this.escape(arg[i].toString());
        }

        return arg.length > 0 ? String.format(tpl, arg) : tpl;
    }

    public void query(String sql, CallbackUtils<ResultSet> callback, Conn... connections) {
        log.msg("sql: " + sql, 2);

        last_sql.set(sql);
        last_err.set("");

        Conn ln = connections.length == 0 ? this.get_connection() : connections[0];

        try (Statement sm = ln.createStatement(); ResultSet rs = sm.executeQuery(sql)) {
            ln.commit();

            callback.deal(rs);
        } catch (Exception e) {
            e.printStackTrace();

            ln.time = com.time() - 36000; // 为了释放连接，如果不释放连接，会产生阻塞

            last_err.set(e.toString());
        }

        if (connections.length == 0) this.put_connection(ln);
    }

    public Long alter(String sql, Conn... connections) {
        log.msg("sql: " + sql, 2);

        last_sql.set(sql);
        last_err.set("");
        affect_rows.set(0L);

        long result = 0;
        long log_id = 0;

        Conn ln = connections.length == 0 ? this.get_connection() : connections[0];

        try (Statement sm = ln.createStatement()) {
            result = sm.executeUpdate(sql);

            ln.commit();

            if (sql.indexOf("INSERT INTO") == 0 && insert_id.get().equals("@@")) {
                ResultSet rs = sm.executeQuery("SELECT @@IDENTITY");

                ln.commit();

                while (rs.next()) {
                    log_id = rs.getLong(1);

                    insert_id.set(log_id + "");
                }

                rs.close();
            }
        } catch (Exception e) {
            e.printStackTrace();

            ln.time = com.time() - 36000; // 为了释放连接，如果不释放连接，会产生阻塞

            last_err.set(e.toString());
        }

        if (insert_id.get().equals("@@")) insert_id.set("");

        if (connections.length == 0) this.put_connection(ln);

        affect_rows.set(result);

        if (sql.indexOf("INSERT INTO") == 0 && !insert_id.get().equals("")) {
            return log_id > 0 ? log_id : result;
        } else {
            return result;
        }
    }

    public String limit(String sql, long... set) {
        long length = set.length > 0 ? set[0] : 0;
        long offset = set.length > 1 ? set[1] : 0;

        // sql标准定义表集合是无序集合，因此，在某些版本MySQL中，子查询中临时表无序
        // 但子查询中出现limit指令后，临时表又神奇的有序了
        // 所以使用以下兼容查询方法
        if (offset + length > 0) {
            if (sql.toLowerCase().contains(" limit ")) {
                sql = "SELECT * FROM (" + sql + ") `__T` LIMIT " + offset + "," + length;
            } else {
                sql += " LIMIT " + offset + "," + length;
            }
        }

        return sql;
    }

    public String unite(String unite) {
        String sql = "";

        String[] unite_arr = unite.split(";");

        for (String unite_str : unite_arr) {
            String[] join_arr = unite_str.split(",");

            if (join_arr.length < 2) continue;

            if (join_arr.length > 2) {
                sql += join_arr[2].toUpperCase() + " ";
            }

            sql += "JOIN " +  this.key_esc(join_arr[0]) + " ON " + this.key_esc(join_arr[1].replace(" ", ""), ".", "=") + " ";
        }

        return sql;
    }

    public String single(String sql) {
        this.restore_table();

        String[] result = {""};

        this.query(sql, rs -> {
            while (rs.next()) {
                result[0] = rs.getString(1) == null ? "" : rs.getString(1);
            }
        });

        return result[0];
    }

    public List<Map<String, String>> result(String sql, long... set) {
        this.restore_table();

        ArrayList<Map<String, String>> list = new ArrayList<>();

        sql = this.limit(sql, set);

        this.query(sql, rs -> {
            ResultSetMetaData md = rs.getMetaData();

            while (rs.next()) {
                Map<String, String> objectMap = new LinkedHashMap<>();

                for(int i = 1;i <= md.getColumnCount();i++) {
                    objectMap.put(md.getColumnLabel(i).toLowerCase(), rs.getString(i) == null ? "" : rs.getString(i));
                }

                list.add(objectMap);
            }
        });

        return list;
    }

    public List<Map<String, String>> result(String sql, String limit) {
        long[] set = {0, 0};

        if (!limit.equals("")) {
            String[] limit_arr = limit.split(",");

            if (limit_arr.length > 0) set[0] = Long.parseLong(limit_arr[0]);
            if (limit_arr.length > 1) set[1] = Long.parseLong(limit_arr[1]);
        }

        return this.result(sql, set);
    }

    public Long action(String sql, Conn... connections) {
        this.restore_table();

        return this.alter(sql, connections);
    }

    public String fields(String field) {
        field = field.replace("`", "");
        field = field.replace("'", "");
        field = field.replace("\"","");

        if (field.equals("")) return "*";

        if (field.toLowerCase().contains(" as ")) field = field.replaceAll("(?i) as ", " AS ");

        field = this.key_esc(field, ".", " AS ", ",");

        if (field.toLowerCase().contains(key_esc + "distinct")) field = field.replaceAll("(?i)" + key_esc + "distinct ", "DISTINCT " + key_esc);

        return field.replace(this.key_esc("*"), "*");
    }

    public String escape(String str){
        if (str == null) return "";

        if (!str.equals("")) {
            String[] fbs = {val_esc, "'"};

            for (String fb : fbs) {
                if (str.contains(fb)) {
                    str = str.replace(fb, val_esc + fb);
                }
            }
        }

        return str;
    }

    public String where(Map<String, Object> map) {
        // map必须使用LinkedHashMap以维持插入顺序
        if (map == null) return "";

        String sql = "";
        boolean need_logic = false;

        for (String key : map.keySet()) {
            String val;
            Object obj = map.get(key);

            val = obj == null ? "" : obj.toString();

            if (key.startsWith("#")) continue;

            if (sql.equals("")) sql = "WHERE ";

            if (key.startsWith("^")) {
                if (val.equals("and (") || val.equals("or (")) {
                    val = need_logic ? val.toUpperCase() : "(";

                    need_logic = false;
                }

                sql += val + " ";

                continue;
            }

            int pos = key.indexOf("#");

            String logic;
            String lgc;

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

            // 这里 key 允许更灵活的实现，可能在某些情况下或有注入隐患，尽量避免直接使用用户输入作为 key
            String chr = !key.contains(key_esc) ? key_esc : "";
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
                    val = this.escape(val);

                    if (logic.contains("like")) {
                        key = key.replace(" = ", "");

                        if (!key.startsWith(key_esc + "*"))
                        {
                            val = val.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");

                            String like = (key.startsWith(key_esc + "%") ? "%" : "_") + (key.endsWith("%" + key_esc) ? "%" : "_");

                            key = key.replace("%", "");

                            switch (like) {
                                case "_%":
                                    val = val + '%';
                                    break;
                                case "%_":
                                    val = "%" + val;
                                    break;
                                default:
                                    val = "%" + val + "%";
                                    break;
                            }
                        } else {
                            key = key.replace("*", "");
                        }
                    }

                    val = "'" + val + "'";
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
                } else if(obj instanceof String[]) {
                    val = "";

                    for (String str : (String[]) obj) {
                        val += str + ",";
                    }

                    if(!val.equals("")) val = val.substring(0, val.length() - 1);
                }

                if (val.equals("")) continue;

                val = "('" + this.escape(val).replace(",", "','") + "')";
            }

            switch (logic) {
                case "or":
                    lgc = "OR";
                    break;
                case "like":
                    lgc = "AND";

                    if (key.contains(",")) key = "CONCAT_WS(' '," + this.key_esc(key.substring(1, key.length() - 1), ",", ".") +")";

                    key += " LIKE ";
                    break;
                case "or_like":
                    lgc = "OR";

                    if (key.contains(",")) key = "CONCAT_WS(' '," + this.key_esc(key.substring(1, key.length() - 1), ",", ".") + ")";

                    key += " LIKE ";
                    break;
                case "not_like":
                    lgc = "AND";

                    if (key.contains(",")) key = "CONCAT_WS(' '," + this.key_esc(key.substring(1, key.length() - 1), ",", ".") + ")";

                    key += " NOT LIKE ";
                    break;
                case "or_not_like":
                    lgc = "OR";

                    if (key.contains(",")) key = "CONCAT_WS(' '," + this.key_esc(key.substring(1, key.length() - 1), ",", ".") + ")";

                    key += " NOT LIKE ";
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

        return sql.equals("WHERE ") ? "" : sql;
    }

    public String where (String... fv) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            map.put(fv[i], fv[i + 1]);
        }

        return this.where(map);
    }

    public Long count(Map<String, Object> map) {
        String sql = this.sql("SELECT COUNT(*) FROM %s ", this.key_esc(this.table()));

        String unite = map.getOrDefault("#unite","").toString();

        if (!unite.equals("")) sql += this.unite(unite);

        String where = this.where(map);

        sql += where.equals("") ? "FORCE INDEX(PRIMARY)" : where;

        return Long.parseLong(this.single(sql));
    }

    public Long count(String... fv) {
        Map<String, Object> where = new LinkedHashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            where.put(fv[i], fv[i + 1]);
        }

        return this.count(where);
    }

    public List<Map<String, Object>> total(Map<String, Object> map) {
        long count = this.count(map);

        List<Map<String, Object>> list = new ArrayList<>();

        Map<String, Object> count_map = new LinkedHashMap<>();

        count_map.put("count", count);

        list.add(count_map);

        return list;
    }

    public List<Map<String, String>> read() {
        return this.read(new LinkedHashMap<>());
    }

    public List<Map<String, String>> read(Map<String, Object> map) {
        String sql;

        String field = map.getOrDefault("#field","").toString();
        String unite = map.getOrDefault("#unite","").toString();
        String order = map.getOrDefault("#order","").toString();

        if (map.get("#limit") != null && map.get("#limit") instanceof Api.Line) {
            Api.Line line = CastUtils.cast(map.get("#limit"));

            map.put("#limit", line.size + "," + ((line.page - 1) * line.size));
        }

        String limit = map.getOrDefault("#limit","").toString();

        sql = this.sql("SELECT %s FROM %s ", this.fields(field), this.key_esc(this.table()));

        if (!unite.equals("")) sql += this.unite(unite);

        sql += this.where(map);

        if (!order.equals("")) {
            if (!order.startsWith("field(")) {
                String order_str = "";

                if (!order.equals("?")) {
                    for (String order_seg : order.split(";")) {
                        String[] order_arr = order_seg.split(",");

                        if (order_arr.length < 2) continue;

                        if (!order_str.equals("")) order_str += ",";

                        if (order_arr[0].startsWith("*")) {
                            order_str += "(" + this.key_esc(order_arr[0].substring(1), ".") + " + 0) " + order_arr[1].toUpperCase();
                        } else {
                            order_str += this.key_esc(order_arr[0], ".") + " " + order_arr[1].toUpperCase();
                        }
                    }
                } else {
                    sql += "RAND()";
                }

                if(!order_str.equals("")) sql += "ORDER BY " + order_str + " ";
            } else {
                String in_str = "";

                String[] in_arr = order.substring(6, order.length() - 1).split(",");

                for (int i = 0;i < in_arr.length;i++) {
                    in_str += i > 0 ? ",'" + this.escape(in_arr[i]) + "'" : this.key_esc(in_arr[i], ".");
                }

                sql += "ORDER BY FIELD(" + in_str + ") ";
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
        Map<String, Object> where = new LinkedHashMap<>();

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

        return list.size() == 0 ? new LinkedHashMap<>() : list.get(0);
    }

    public Map<String, String> find(String field, String... fv) {
        Map<String, Object> map = new LinkedHashMap<>();

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

    public String field(String field, Map<String, Object> where, String... def) {
        where.put("#field", field);

        Map<String, String> item = this.find(where);

        for (String col : item.keySet()) {
            return item.get(col);
        }

        return def.length > 0 ? def[0] : "";
    }

    public String field(String field, String... fv) {
        Map<String, Object> map = new LinkedHashMap<>();

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

        return this.field(field, map, map.getOrDefault("#value", "").toString());
    }

    public Long create(Map<String, String> data, Conn... connections) {
        insert_id.set("@@");

        String sql = this.sql("INSERT INTO %s ", this.key_esc(this.table()));

        String field = "";
        String value = "";

        Set<String> keys = data.keySet();

        for (String key : keys) {
            field += this.key_esc(key) + ",";
            value += "'" + this.escape(data.get(key)) + "',";
        }

        sql += "(" + field.substring(0, field.length() - 1) + ") VALUES (" + value.substring(0, value.length() - 1) + ")";

        return this.action(sql, connections);
    }

    public Long create(String... fv) {
        Map<String, String> data = new LinkedHashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            data.put(fv[i], fv[i + 1]);
        }

        return data.size() > 0 ? this.create(data) : 0;
    }

    public Long create(List<Map<String, String>> list) {
        insert_id.set("");

        String sql = this.sql("INSERT IGNORE INTO %s ", this.key_esc(this.table()));

        String field = "";
        String value = "";

        Set<String> keys = list.get(0).keySet();

        for (String key : keys) {
            field += this.key_esc(key) + ",";
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

    public Long update(Map<String, Object> where, Map<String, String> data) {
        if (data.size() == 0) {
            this.restore_table();

            return 0L;
        }

        String sql = this.sql("UPDATE %s SET ", this.key_esc(this.table()));

        Set<String> keys = data.keySet();

        for (String key : keys) {
            sql += this.key_esc(key) + " = '" + this.escape(data.get(key)) + "',";
        }

        sql = sql.substring(0, sql.length() - 1) + " ";

        sql += this.where(where);

        return this.action(sql);
    }

    public Long update(String id, Map<String, String> data) {
        Map<String, Object> where = new LinkedHashMap<>();

        where.put("id", id);

        return this.update(where, data);
    }

    public Long update(String field, Object value, Map<String, String> data) {
        Map<String, Object> where = new LinkedHashMap<>();

        where.put(field, value);

        return this.update(where, data);
    }

    public Long update(Map<String, Object> where, String field, String... fv) {
        Map<String, String> data = new LinkedHashMap<>();

        data.put(field, fv[0]);

        for (int i = 1;i < fv.length;i += 2) {
            data.put(fv[i], fv[i + 1]);
        }

        return this.update(where, data);
    }

    public Long update(Map<String, String> data, String... fv) {
        Map<String, Object> where = new LinkedHashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            where.put(fv[i], fv[i + 1]);
        }

        return this.update(where, data);
    }

    public Long update(String id, String... fv) {
        Map<String, Object> where = new LinkedHashMap<>();

        if (fv.length % 2 == 0) {
            where.put("id", id);
        } else {
            where.put(id, fv[0]);
        }

        Map<String, String> data = new LinkedHashMap<>();

        for (int i = fv.length % 2;i < fv.length;i += 2) {
            data.put(fv[i], fv[i + 1]);
        }

        return data.size() > 0 ? this.update(where, data) : 0;
    }

    public Long update(int where_num, String... fv) {
        Map<String, Object> where = new LinkedHashMap<>();
        Map<String, String> data  = new LinkedHashMap<>();

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

    public Long delete(Map<String, Object> where) {
        String sql = this.sql("DELETE FROM %s ", this.key_esc(this.table()));

        sql += this.where(where);

        return this.action(sql);
    }

    public Long delete(String... id) {
        Map<String, Object> map = new LinkedHashMap<>();

        if (id.length == 1) {
            map.put("id", id[0]);
        } else {
            for (int i = 0;i < id.length;i += 2) {
                map.put(id[i], id[i + 1]);
            }
        }

        return this.delete(map);
    }

    public Long increase(Map<String, Object> where, Map<String, String> delta, Map<String, String> data) {
        if (delta.size() == 0) return 0L;

        String sql = this.sql("UPDATE %s SET ", this.key_esc(this.table()));

        for (String key : delta.keySet()) {
            sql += this.key_esc(key) + "=" + this.key_esc(key) + (delta.get(key).startsWith("+") || delta.get(key).startsWith("-") ? delta.get(key) : "+" + delta.get(key)) + ", ";
        }

        if (data.size() > 0) {
            for (String field : data.keySet()) {
                sql += this.key_esc(field) + "='" + this.escape(data.get(field)) + "', ";
            }
        }

        sql = sql.substring(0, sql.length() - 2) + " " + this.where(where);

        return this.action(sql);
    }

    public Long increase(Map<String, Object> where, Map<String, String> delta) {
        return this.increase(where, delta, new LinkedHashMap<>());
    }

    public Long increase(int where_num, String... fv) {
        int count_num = 0;

        Map<String, Object> where = new LinkedHashMap<>();
        Map<String, String> delta = new LinkedHashMap<>();


        for (int i = 0;i < fv.length;i += 2) {
            if (count_num < where_num) {
                where.put(fv[i], fv[i + 1]);

                count_num++;
            } else {
                delta.put(fv[i], fv[i + 1]);
            }
        }

        return this.increase(where, delta);
    }

    public Long increase(int where_num, int delta_num, String... fv) {
        int count_num = 0;
        int total_num = 0;

        Map<String, Object> where = new LinkedHashMap<>();
        Map<String, String> delta = new LinkedHashMap<>();
        Map<String, String> data  = new LinkedHashMap<>();

        for (int i = 0;i < fv.length;i += 2) {
            if (count_num < where_num) {
                where.put(fv[i], fv[i + 1]);

                count_num++;
            } else if (total_num < delta_num) {
                delta.put(fv[i], fv[i + 1]);

                total_num++;
            } else {
                data.put(fv[i], fv[i + 1]);
            }
        }

        return this.increase(where, delta, data);
    }

    public List<String> cols(String... table) {
        String current_table = table.length == 0 ? this.table() : table[0];

        if (table.length == 0) this.restore_table();

        List<String> list = new ArrayList<>();

        String sql = this.sql("SHOW COLUMNS FROM %s", key_esc(current_table));

        this.query(sql, rs -> {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        });

        return list;
    }

    public List<Map<String, String>> cols_map(String... table) {
        String current_table = table.length == 0 ? this.table() : table[0];

        if (table.length == 0) this.restore_table();

        String sql = this.sql("SELECT column_name,column_comment,column_default FROM information_schema.columns WHERE table_schema='%s' AND table_name='%s'", db_database, current_table);

        return this.result(sql);
    }

    public Map<String, String> col_dict(String... table) {
        Map<String, String> dict = new LinkedHashMap<>();

        List<Map<String, String>> cols = this.cols_map(table);

        for (Map<String, String> item : cols) {
            String[] comment = item.get("column_comment").split("\\|");

            dict.put(item.get("column_name").toLowerCase(), comment[0]);
        }

        return dict;
    }

    public List<String> tbls() {
        List<String> list = new ArrayList<>();

        String sql = this.sql("SHOW TABLES");

        this.query(sql, rs -> {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        });

        return list;
    }

    public void add_partition(String table, String field, String title, String value) {
        String p_check_sql = this.sql("SELECT * FROM INFORMATION_SCHEMA.PARTITIONS WHERE TABLE_SCHEMA='%s' AND TABLE_NAME='%s' ORDER BY PARTITION_DESCRIPTION DESC", db_database, table);

        List<Map<String, String>> p_list = this.result(p_check_sql);

        // 检测分区是否存在
        if (p_list.size() > 0) {
            for (Map<String, String> p_item : p_list) {
                if (p_item.get("partition_name").equals(title)) return;
            }

            String p_add_sql = this.sql("ALTER TABLE `%s` add PARTITION (PARTITION `%s` VALUES IN ('%s') ENGINE = InnoDB)", table, title, value);

            this.alter(p_add_sql);
        } else {
            // 设置并创建分区
            String p_set_sql = this.sql("ALTER TABLE `%s` PARTITION BY LIST COLUMNS(`" + field + "`) (PARTITION `%s` VALUES IN ('%s') ENGINE = InnoDB)", table, title, value);

            this.alter(p_set_sql);
        }
    }

    public void del_partition(String table, String title) {
        String p_del_sql = this.sql("ALTER TABLE `%s` DROP PARTITION `%s`", table, title);

        this.alter(p_del_sql);
    }

    @Override
    public Db clone() {
        Db db = null;

        try{
            db = (Db) super.clone();
        }catch(CloneNotSupportedException e) {
            e.printStackTrace();

            db = new Db();
        }

        return db;
    }
}
