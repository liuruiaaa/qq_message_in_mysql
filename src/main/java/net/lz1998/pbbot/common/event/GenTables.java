package net.lz1998.pbbot.common.event;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.extra.spring.SpringUtil;

import com.mysql.cj.jdbc.ConnectionImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.lz1998.pbbot.common.exception.SystemException;
import net.lz1998.pbbot.model.pojo.BasePojo;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import javax.activation.UnsupportedDataTypeException;
import javax.sql.DataSource;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

/**
 * 成都思致科技有限公司
 *
 * @author Changge Zhang
 * @date 2022/5/24 11:08
 * =========================================================================
 * 变更履历：
 * -------------------------------------------------------------------------
 * 变更编号     变更时间    变更人   变更原因    变更内容
 * <p>
 * -------------------------------------------------------------------------
 */
@Component
@Slf4j
public class GenTables {
    private Connection connection;

    public void genTable(){
        String scanPackageName = "net.lz1998.pbbot.model.pojo";
        List<Class<?>> excludeClasses = CollectionUtil.newArrayList(BasePojo.class);
        List<Class<?>> classes = loadCCBModelClass(scanPackageName);
        classes.removeIf(c -> excludeClasses.contains(c));
        try {
            for (Class<?> clazz : classes) {
                genTable(clazz);
            }
        } catch (Exception e) {
            log.error("数据库表生成失败");
        }finally {
            try {
                if(connection != null && !connection.isClosed()){
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        log.error("连接关闭失败{}",e);
                    }
                }
            } catch (SQLException e) {
                log.error("连接或已关闭{}",e);
            }
        }
    }

    private void genTable(Class<?> clazz) {
        try {
            getConnection();
            String tableName = getTableName(clazz);
            boolean exists = existsTable(tableName);
            if(!exists){
                createTable(clazz,tableName);
            }else{
                genColumns(clazz,tableName);
            }
        } catch (SQLException e) {
            log.error("数据库连接失败：{}",e);
        }
    }

    private void genColumns(Class<?> clazz, String tableName) {
        List<Field> fields = loadFields(clazz);
        Field field;
        String beforeColumn = null;
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            String dbType = getColumnType(field.getType());
            if("serialVersionUID".equals(field.getName()) || !StringUtils.hasText(dbType)) continue;
            String columnName = camelCaseToUnderScore(field.getName());
            boolean existsColumn = existsColumns(columnName,tableName);
            if(!existsColumn){
                addColumn(tableName,columnName,dbType,getDefaultValue(dbType),beforeColumn);
            }
            beforeColumn = columnName;
        }
    }

    private List<Field> loadFields(Class<?> clazz) {
        List<Field> fields = getFields(clazz);
        Field idField = fields.stream().filter(f -> f.getName().equals("keyId")).findFirst().orElse(null);
        fields.remove(idField);
        fields.add(0,idField);
        Predicate<Field> predicate = f -> f != null && !f.getName().equals("keyId");
        List<Field> sortFields = Arrays.stream(BasePojo.class.getDeclaredFields()).filter(predicate).collect(Collectors.toList());
        List<String> fieldsNames = Arrays.stream(BasePojo.class.getDeclaredFields()).filter(predicate).map(f -> f.getName()).collect(Collectors.toList());
        fields.removeIf(f -> fieldsNames.contains(f.getName()));
        fields.addAll(sortFields);
        return fields;
    }

    private boolean addColumn(String tableName, String columnName, String dbType, String defaultValue,String beforeColumn) {
        StringBuffer sql = new StringBuffer("ALTER TABLE `").append(tableName).append("` ADD COLUMN `").append(columnName).append("` ")
                .append(dbType).append(" DEFAULT ").append(defaultValue);
        if(StringUtils.hasText(beforeColumn)){
            sql.append(" AFTER `").append(beforeColumn).append("` ");
        }
        sql.append(";");
        return executeDMLSql(sql.toString());
    }

    private boolean executeDMLSql(String createTableSql) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(createTableSql);
        } catch (SQLException e) {
            log.error("SQL解析失败：{}，{}",createTableSql,e);
            throw new SystemException(e,"SQL解析失败:%s",createTableSql);
        }
        try {
            return preparedStatement.execute();
        } catch (SQLException e) {
            log.error("SQL 执行失败：{},{}",createTableSql,e);
            throw new SystemException(e,"SQL 执行失败：%s",createTableSql);
        }
    }

    private boolean existsColumns(String columnName, String tableName) {
        String sql = "SELECT COUNT(1) FROM information_schema.COLUMNS where TABLE_SCHEMA= 'mirai_msg' AND TABLE_NAME=? AND COLUMN_NAME=? ;";
        PreparedStatement preparedStatement = executeDCL(tableName,sql);
        try {
            preparedStatement.setString(2,columnName);
        } catch (SQLException e) {
            log.error("参数设置失败：{},{}",columnName,e);
            throw new SystemException(e,"参数设置失败:%s",columnName);
        }
        ResultSet rs = null;
        try{
            rs = preparedStatement.executeQuery();
            if(rs.next()){
                return rs.getLong(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            log.error("SQL 执行失败：{},{}",sql,e);
            throw new SystemException(e,"SQL 执行失败：%s",sql);
        }
    }

    private PreparedStatement executeDCL(String tableName, String sql) {
        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            log.error("SQL解析失败：{}，{}",sql,e);
            throw new SystemException(e,"SQL解析失败:%s",sql);
        }
        try {
            preparedStatement.setString(1,tableName);
        } catch (SQLException e) {
            log.error("参数设置失败：{},{}",tableName,e);
            throw new SystemException(e,"参数设置失败:%s",tableName);
        }
        return preparedStatement;
    }

    private boolean createTable(Class<?> clazz, String tableName) {
        String createTableSql = genCreateTableSql(clazz,tableName);
        return executeDMLSql(createTableSql);
    }

    private String genCreateTableSql(Class<?> clazz, String tableName) {
        StringBuffer stringBuffer = new StringBuffer("CREATE TABLE IF NOT EXISTS `").append(tableName).append("` (");
        List<Field> fields = loadFields(clazz);
        Field field;
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            String name = field.getName();
            String dbType = getColumnType(field.getType());
            if(dbType == null || "serialVersionUID".equals(name)) continue;
            String columnName = camelCaseToUnderScore(name);
            String defaultValue = getDefaultValue(dbType);
            stringBuffer.append("\r\n\t`").append(columnName).append("` ").append(dbType).append(" NOT NULL ");
            if(name.equals("modifyTime")){
                stringBuffer.append(" DEFAULT ").append(defaultValue).append(" ON UPDATE CURRENT_TIMESTAMP ");
            }else if(name.equals("keyId")){
                stringBuffer.append(" PRIMARY KEY AUTO_INCREMENT ");
            }else{
                stringBuffer.append(" DEFAULT ").append(defaultValue);
            }
            if(i < fields.size() - 1){
                stringBuffer.append(" ,");
            }
        }
        return stringBuffer.append("\r\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 ROW_FORMAT=DYNAMIC").toString();
    }

    private List<Field> getFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        List<Field> fieldList = new ArrayList<>(Arrays.asList(fields));
        Class<?> parentClass = clazz.getSuperclass();
        if(parentClass != null){
            fieldList.addAll(getFields(parentClass));
        }
        return fieldList;
    }

    private String getDefaultValue(String dbType) {
        String defaultValue = "";
        int ind = dbType.indexOf("(");
        if(ind != -1){
            dbType = dbType.substring(0,ind);
        }
        switch (dbType){
            case "tinyint":
            case "smallint":
            case "int":
            case "bigint":
            case "decimal":
                defaultValue = "0";
                break;
            case "date":
            case "datetime":
            case "timestamp":
            case "time":
                defaultValue = "CURRENT_TIMESTAMP";
                break;
            case "varchar":
                defaultValue = "''";
                break;
            case "char":
                defaultValue = "' '";
        }
        return defaultValue;
    }

    private String getColumnType(Class<?> type) {
        if(Byte.class.equals(type) || byte.class.equals(type)) return "tinyint";
        if(Short.class.equals(type) || short.class.equals(type)) return "smallint";
        if(BigInteger.class.equals(type) ) return "bigint(19)";
        if(Integer.class.equals(type) || int.class.equals(type)) return "int(11)";
        if(Long.class.equals(type) || long.class.equals(type)) return "bigint(19)";
        if(Float.class.equals(type) || float.class.equals(type)
                || Double.class.equals(type) || double.class.equals(type)
                || BigDecimal.class.equals(type)) return "decimal(20,6)";
        if(String.class.equals(type)) return "varchar(255)";
        if(Character.class.equals(type) || char.class.equals(type)) return "char(1)";
        if(Date.class.equals(type) || LocalDateTime.class.equals(type)) return "datetime";
        if(LocalDate.class.equals(type)) return "date";
        if(LocalTime.class.equals(type)) return "time";
        if(DateTime.class.equals(type)) return "datetime";
        if(Timestamp.class.equals(type)) return "timestamp";
        return null;
    }

    private boolean existsTable(String tableName) {
        String queryTable = "select count(1) from information_schema.TABLES where TABLE_NAME=? AND TABLE_SCHEMA='mirai_msg';";
        PreparedStatement pst = executeDCL(tableName, queryTable);
        try {
            ResultSet rs = pst.executeQuery();
            if(rs.next()){
                return rs.getLong(1) > 0;
            }
            return false;
        } catch (SQLException e) {
            log.error("SQL 执行失败：{},{}",queryTable,e);
            throw new SystemException(e,"SQL 执行失败：%s",queryTable);
        }
    }

    private String getTableName(Class<?> clazz) {
        String className = clazz.getSimpleName();
        return pascalCaseToUnderScore(className);
    }

    private String camelCaseToUnderScore(String className) {
        char[] classBytes = className.toCharArray();
        StringBuffer characters = new StringBuffer();
        for (char classByte : classBytes) {
            if(Character.isUpperCase(classByte)){
                characters.append('_');
                characters.append((char)(classByte + 32));
            }else{
                characters.append(classByte);
            }
        }
        return characters.toString();
    }

    private String pascalCaseToUnderScore(String className) {
        char firstLetter = Character.isUpperCase(className.charAt(0)) ? (char)(className.charAt(0) + 32) : className.charAt(0);
        String newClassName = firstLetter + className.substring(1);
        return camelCaseToUnderScore(newClassName);
    }

    private List<Class<?>> loadCCBModelClass(String scanPackageName) {
        List<Class<?>> classes = new ArrayList<>();
        try {
            String packagePath = scanPackageName.replaceAll("\\.","/");
            Enumeration<URL> urls = this.getClass().getClassLoader().getResources(packagePath);
            while(urls.hasMoreElements()){
                URL url = urls.nextElement();
                String protocol = url.getProtocol();
                if("file".equalsIgnoreCase(protocol)){
                    String filePath = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.displayName());
                    registClassByFiles(scanPackageName,filePath,classes);
                }else if("jar".equalsIgnoreCase(protocol)){
                    JarFile jarFile = ((JarURLConnection)url.openConnection()).getJarFile();
                    registClassByJar(scanPackageName,packagePath,jarFile,classes);
                }
            }
        } catch (IOException e) {
            log.error("Model 注册失败：{}", e);
        }
        return classes;
    }

    private void registClassByJar(String scanPackageName, String packagePath, JarFile jarFile, List<Class<?>> classes) {
        Enumeration<JarEntry> jarEntries = jarFile.entries();
        while(jarEntries.hasMoreElements()){
            JarEntry jarEntry = jarEntries.nextElement();
            String name = jarEntry.getName();
            if(name.charAt(0) == '/'){
                name = name.substring(1);
            }
            if(name.startsWith(packagePath)){
                int ind = name.lastIndexOf("/");
                if(ind != -1){
                    scanPackageName = name.substring(0,ind).replace('/','.');
                    if (name.endsWith(".class") && !jarEntry.isDirectory()) {
                        String className = name.substring(scanPackageName.length() + 1,name.length() - ".class".length());
                        String fullClassName = scanPackageName + "." + className;
                        try {
                            classes.add(this.getClass().getClassLoader().loadClass(fullClassName));
                            log.info("注册POJO完成：{}",fullClassName);
                        } catch (ClassNotFoundException e) {
                            log.error("类加载失败: {}",fullClassName);
                        }
                    }
                }
            }
        }
    }

    private void registClassByFiles(String scanPackageName, String filePath, List<Class<?>> classes) {
        File dir = new File(filePath);
        if(!dir.exists() || !dir.isDirectory()){
            return;
        }
        File[] files = dir.listFiles(pathname -> pathname.isDirectory() || (pathname.isFile() && pathname.getName().endsWith("class")));
        for (File file : files) {
            if(file.isDirectory()){
                registClassByFiles(scanPackageName + "." + file.getName(),file.getAbsolutePath(),classes);
            }else{
                String className = file.getName().substring(0,file.getName().length() - ".class".length());
                String fullClassName = scanPackageName + "." + className;
                try {
                    classes.add(this.getClass().getClassLoader().loadClass(fullClassName));
                    log.info("注册POJO完成：{}",fullClassName);
                } catch (ClassNotFoundException e) {
                    log.error("类加载失败: {}",fullClassName);
                }
            }
        }
    }

    public void getConnection() throws SQLException {
        connection = genConnection();
        if(connection.isClosed()){
            ((ConnectionImpl)connection).handleReconnect();
        }
        // connection.setAutoCommit(false);
    }

    public Connection genConnection(){
        try {
            DataSource dataSource = SpringUtil.getBean(DataSource.class);
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.error("数据库连接池获取失败：{}",e);
            throw new SystemException(e,"数据库连接池获取失败");
        }
    }

    public <T extends BasePojo>int[] saveBatch(T... tableData){
        if(tableData == null || tableData.length <= 0){
            return null;
        }
        List<T> data = new ArrayList<>(Arrays.asList(tableData));
        return saveBatchList(data);
    }

    public <T extends BasePojo>int save(T tableData){
        if(tableData == null){
            return 0;
        }
        List<T> data = new ArrayList<>(Arrays.asList(tableData));
        return saveBatchList(data)[0];
    }

    public <T extends BasePojo>int updaetByKeyId(T tableData) throws SQLException, UnsupportedDataTypeException {
        if(tableData == null){
            return 0;
        }
        return updaetById(tableData);
    }


    public <T extends BasePojo>int[] saveBatchList(List<T> tableData){
        if(tableData == null || tableData.isEmpty()){
            return null;
        }
        int pageSize = 200;
        int totalPage = PageUtil.totalPage(tableData.size(),pageSize);
        List<T> subData;
        int[] rows = new int[tableData.size()];
        int[] effectRows;
        int dest = 0;
        try {
            getConnection();
            //connection.setAutoCommit(false);
            for (int i = 0; i < totalPage; i++) {
                int[] page = PageUtil.transToStartEnd(i,pageSize);
                subData = tableData.subList(page[0],page[1] > tableData.size() ? tableData.size() : page[1]);
                effectRows = saveBatch(subData);
                System.arraycopy(effectRows,0,rows,dest,effectRows.length);
                dest += effectRows.length;
            }
            //connection.commit();
        } catch (SQLException e) {
            log.error("SQL执行失败：{}",e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.error("事务回滚失败:{}",e);
            }
            throw new SystemException(e,"数据保存失败");
        }catch(Exception e){
            log.error("数据新增失败:{}",e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                log.error("事务回滚失败:{}",e);
            }
            throw new SystemException(e,"数据保存失败");
        }

        try {
            connection.setAutoCommit(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return rows;
    }

    private <T extends BasePojo> int updaetById(T subData) throws SQLException, UnsupportedDataTypeException {
        Class<?> clazz = subData.getClass();
        String tableName = getTableName(clazz);
        List<Column> columns = getColumnByUpdate(clazz,subData);
        if(CollUtil.isEmpty(columns)) return 0;
        List<Param> params = new ArrayList<>();
        //===这里完成组装
        String updateSQL = genUpdateSQLById(tableName,columns,subData,params,subData.getKeyId());
        printlnSql("======================>  "+updateSQL,params);
        List<Long> keyIds = new ArrayList<>();
        //===这里是执行
        int[] effectRows = executeInsert(updateSQL,params,keyIds);
        return effectRows[0];
    }

    public  <T extends BasePojo> int deleteById (T subData) throws SQLException, UnsupportedDataTypeException {
        Class<?> clazz = subData.getClass();
        String tableName = getTableName(clazz);
        List<Column> columns = getColumnByUpdate(clazz,subData);
        if(CollUtil.isEmpty(columns)) return 0;
        List<Param> params = new ArrayList<>();
        //===这里完成组装
        String updateSQL = gendeleteSQLById(tableName,subData.getKeyId());
        printlnSql("======================>  "+updateSQL,params);
        List<Long> keyIds = new ArrayList<>();
        //===这里是执行
        int[] effectRows = executeInsert(updateSQL,params,keyIds);
        return effectRows[0];
    }


    private <T extends BasePojo> int[] saveBatch(List<T> subData) throws SQLException, UnsupportedDataTypeException {
        Class<?> clazz = subData.get(0).getClass();
        String tableName = getTableName(clazz);
        List<Column> columns = getColumn(clazz);
        if(CollUtil.isEmpty(columns)) return null;
        List<Param> params = new ArrayList<>();
        //===这里完成组装
        String insertSQL = genInsertSQL(tableName,columns,subData,params);
        //printlnSql("======================>  "+insertSQL,params);
        List<Long> keyIds = new ArrayList<>();
        //===这里是执行
        int[] effectRows = executeInsert(insertSQL,params,keyIds);
        if (keyIds != null && !keyIds.isEmpty()) {
            T data;
            for (int i = 0; i < subData.size(); i++) {
                data = subData.get(i);
                data.setKeyId(keyIds.get(i));
            }
        }
        return effectRows;
    }

    private int[] executeInsert(String insertSQL, List<Param> params, List<Long> keyIds) throws SQLException {

        PreparedStatement preparedStatement = connection.prepareStatement(insertSQL,Statement.RETURN_GENERATED_KEYS);
        if(CollUtil.isNotEmpty(params)){
            fillParams(preparedStatement,params);
        }
        int effectRows = preparedStatement.executeUpdate();
        ResultSet resultSet = preparedStatement.getGeneratedKeys();
        while(resultSet.next()){
            long id = resultSet.getLong(1);
            keyIds.add(id);
        }
        return new int[]{effectRows};
    }

    private void fillParams(PreparedStatement ps, List<Param> params) throws SQLException {
        Param param;
        for (int i = 0; i < params.size(); i++) {
            param = params.get(i);
            if (param.value == null) {
                ps.setObject(i + 1,null, param.getDbType());
            }else if (param.value instanceof Byte || param.value.getClass().equals(byte.class)) {
                ps.setByte(i + 1,((Byte) param.value).byteValue());
            }else if (param.value instanceof Short || param.value.getClass().equals(short.class)) {
                ps.setShort(i + 1,((Short) param.value).shortValue());
            }else if (param.value instanceof BigInteger || param.value.getClass().equals(BigInteger.class)) {
                ps.setLong(i + 1,((BigInteger) param.value).longValue());
            }else if (param.value instanceof Integer || param.value.getClass().equals(Integer.class)) {
                ps.setInt(i + 1,((Integer) param.value).intValue());
            }else if (param.value instanceof Long || param.value.getClass().equals(long.class)) {
                ps.setLong(i + 1,((Long) param.value).longValue());
            }else if (param.value instanceof Float || param.value.getClass().equals(float.class)) {
                ps.setFloat(i + 1,((Float) param.value).floatValue());
            }else if (param.value instanceof Double || param.value.getClass().equals(double.class)) {
                ps.setDouble(i + 1,((Double) param.value).doubleValue());
            }else if (param.value instanceof Boolean || param.value.getClass().equals(boolean.class)) {
                ps.setBoolean(i + 1,((Boolean) param.value).booleanValue());
            }else if (param.value instanceof Character || param.value.getClass().equals(char.class)) {
                ps.setObject(i + 1,((Character) param.value).charValue(),Types.CHAR);
            }else if (param.value instanceof CharSequence) {
                ps.setString(i + 1,param.value.toString());
            }else if (param.value instanceof BigDecimal) {
                ps.setBigDecimal(i + 1,(BigDecimal) param.value);
//            }else if (param.value instanceof DateTime) {
//                ps.setDate(i + 1,new DateTime(((DateTime) param.value).getTime()));
            }else if (param.value instanceof java.sql.Timestamp) {
                ps.setTimestamp(i + 1,(java.sql.Timestamp) param.value);
            }else if (param.value instanceof LocalDate) {
                ps.setDate(i + 1,java.sql.Date.valueOf((LocalDate) param.value));
            }else if (param.value instanceof LocalTime) {
                ps.setTime(i + 1,Time.valueOf((LocalTime) param.value));
            }else if (param.value instanceof LocalDateTime) {
                ps.setTimestamp(i + 1,Timestamp.valueOf((LocalDateTime) param.value));
            }else if (param.value instanceof java.sql.Date) {
                ps.setDate(i + 1,(java.sql.Date) param.value);
            }else if (param.value instanceof java.sql.Time) {
                ps.setTime(i + 1,(java.sql.Time) param.value);
            }else if (param.value instanceof Date) {
                ps.setDate(i + 1,new java.sql.Date(((Date) param.value).getTime()));
            }else{
                ps.setObject(i + 1,null, param.getDbType());
            }
        }
    }

    /**
     * DELETE FROM table_name  WHERE some_column=some_value;
     * @param tableName
     * @param keyId
     * @param <T>
     * @param <O>
     * @return
     * @throws UnsupportedDataTypeException
     */
    private <T extends BasePojo,O> String gendeleteSQLById(String tableName, Long keyId) throws UnsupportedDataTypeException {
        StringBuffer stringBuffer = new StringBuffer("DELETE FROM `mirai_msg`.`").append(tableName).append("` ");
        stringBuffer.append(" WHERE  `key_id` = ");
        stringBuffer.append(keyId);
        stringBuffer.append(";");
        System.out.println(stringBuffer.toString());
        return stringBuffer.toString();
    }

    private <T extends BasePojo,O> String genUpdateSQLById(String tableName, List<Column> columns, T subData, List<Param> params,Long keyId) throws UnsupportedDataTypeException {
        StringBuffer stringBuffer = new StringBuffer("UPDATE `mirai_msg`.`").append(tableName).append("` SET ");
        for (int i=0;i<columns.size();i++) {
            Column column = columns.get(i);
            if(column.getName().equals("key_id")){
                continue;
            }
            stringBuffer.append(" `").append(column.name).append("`").append("= ? ");

            if(i < columns.size() - 1){
                stringBuffer.append(",");
            }
            Param param = new Param(getColumnVal(column, subData), getSqlType(column.dbType));
            if(!ObjectUtils.isEmpty(param.getValue())){
                params.add(param);
            }

        }
        stringBuffer.append(" WHERE  `key_id` = ");
        stringBuffer.append(keyId);
        stringBuffer.append(";");

        return stringBuffer.toString();
    }

    /**
     * 打印sql
     * @param sqlString
     * @param params
     */
    private void  printlnSql(String sqlString, List<Param> params){
        for (int i = 0; i <params.size() ; i++) {
            sqlString = sqlString.replaceFirst("\\?",params.get(i).getValue().toString());
        }
        System.out.println(sqlString);
    }

    /**
     * INSERT INTO `mirai_msg`.`repayment_plan_query`(`key_id`, `modify_time`) VALUES (215202328822, 'S30');
     * UPDATE `mirai_msg`.`repayment_plan_query` SET `pt_code` = 'S30',  `add_time` = '2022-06-23 00:00:00' WHERE `key_id` = 215202328821;
     */
    private <T extends BasePojo,O> String genInsertSQL(String tableName, List<Column> columns, List<T> subData, List<Param> params) throws UnsupportedDataTypeException {
        StringBuffer stringBuffer = new StringBuffer("INSERT INTO `mirai_msg`.`").append(tableName).append("` (");
        for (Column column : columns) {
            if(column.getName().equals("key_id")){
                continue;
            }
            stringBuffer.append(" `").append(column.name).append("`,");
        }
        stringBuffer = new StringBuffer(stringBuffer.substring(0,stringBuffer.length() - 1));
        stringBuffer.append(") VALUES");
        Column column;
        T data;
        for (int oi = 0; oi < subData.size(); oi++) {
            data = subData.get(oi);
            data.setModifyTime( new Timestamp(new Date().getTime())); //这个字段放开了，需要加上默认的字段
            data.setAddTime(new Timestamp(new Date().getTime()));     //这个字段放开了，需要加上默认的字段
            stringBuffer.append(" (");
            for (int i = 0; i < columns.size(); i++) {
                column = columns.get(i);
                if(column.getName().equals("key_id")){
                    continue;
                }
                stringBuffer.append(" ").append("?");
                if(i < columns.size() - 1){
                    stringBuffer.append(",");
                }
                params.add(new Param(getColumnVal(column,data),getSqlType(column.dbType)));
            }
            stringBuffer.append(")");
            if(oi < subData.size() - 1){
                stringBuffer.append(",");
            }
        }
        stringBuffer.append(";");
        return stringBuffer.toString();
    }




    private int getSqlType(String dbType) throws UnsupportedDataTypeException {
        int ind = dbType.indexOf("(");
        if(ind != -1){
            dbType = dbType.substring(0,ind);
        }
        switch (dbType){
            case "tinyint":
                return Types.TINYINT;
            case "smallint":
                return Types.SMALLINT;
            case "int":
                return Types.INTEGER;
            case "bigint":
                return Types.BIGINT;
            case "decimal":
                return Types.DECIMAL;
            case "date":
                return Types.DATE;
            case "datetime":
                return Types.TIMESTAMP;
            case "timestamp":
                return Types.TIMESTAMP;
            case "time":
                return Types.TIME;
            case "varchar":
                return Types.VARCHAR;
            case "char":
                return Types.CHAR;
            default:
                throw new UnsupportedDataTypeException("暂时不支持改数据类型");
        }
    }

    private <T extends BasePojo,R> R getColumnVal(Column column, T data) {
        Method readMethod = column.getReadMethod();
        if(readMethod == null || readMethod.getModifiers() != Modifier.PUBLIC){
            return convertDefaultValue(column.getJavaType(),column.getDefaultVal());
        }
        try {
            R r = (R)readMethod.getReturnType().cast(readMethod.invoke(data));
            if(r == null){
                return convertDefaultValue(readMethod.getReturnType(),column.getDefaultVal());
            }
            return r;
        } catch (InvocationTargetException e) {
            return convertDefaultValue(column.getJavaType(),column.getDefaultVal());
        } catch (IllegalAccessException e) {
            return convertDefaultValue(column.getJavaType(),column.getDefaultVal());
        }
    }

    private <V>V convertDefaultValue(Class<?> javaType, String defaultVal) {
        if(CharSequence.class.isAssignableFrom(javaType)){
            return (V)(defaultVal.equals("''") ? "" : defaultVal);
        }else if(Character.class.equals(javaType) || char.class.equals(javaType)){
            return (V)(Character)defaultVal.charAt(0);
        }else if(Byte.class.equals(javaType) || byte.class.equals(javaType)){
            return (V)Byte.valueOf(defaultVal);
        }else if(Integer.class.equals(javaType) || int.class.equals(javaType)){
            return (V)Integer.valueOf(defaultVal);
        }else if(Short.class.equals(javaType) || short.class.equals(javaType)){
            return (V)Short.valueOf(defaultVal);
        }else if(Long.class.equals(javaType) || long.class.equals(javaType)){
            return (V)Long.valueOf(defaultVal);
        }else if(Double.class.equals(javaType) || double.class.equals(javaType)
                || float.class.equals(javaType) || float.class.equals(javaType)
                || BigDecimal.class.equals(javaType)){
            return (V)new BigDecimal(defaultVal);
        }else if(Date.class.equals(javaType)){
            if(defaultVal.equals("CURRENT_TIMESTAMP")){
                return (V)new Date();
            }else{
                return (V)DateUtil.parse(defaultVal);
            }
        }else if(LocalDate.class.equals(javaType)){
            if(defaultVal.equals("CURRENT_TIMESTAMP")){
                return (V)LocalDate.now();
            }else{
                return (V)LocalDate.parse(defaultVal);
            }
        }else if(LocalTime.class.equals(javaType)){
            if(defaultVal.equals("CURRENT_TIMESTAMP")){
                return (V)LocalTime.now();
            }else{
                return (V)LocalTime.parse(defaultVal);
            }
        }else if(LocalDateTime.class.equals(javaType)){
            if(defaultVal.equals("CURRENT_TIMESTAMP")){
                return (V)LocalDateTime.now();
            }else{
                return (V)LocalDateTime.parse(defaultVal,DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        }
        return null;
    }
    /**
     * 对象转Map
     * @param object
     * @return
     * @throws IllegalAccessException
     */
    public static Map beanToMap(Object object) throws IllegalAccessException {
        Map<String, Object> map = new HashMap<String, Object>();
        //Field[] fields = object.getClass().getDeclaredFields();
        List<Field> fields = getAllField(object);
        for (Field field : fields) {
            field.setAccessible(true);
            map.put(field.getName(), field.get(object));
        }
        return map;
    }

    /**
     * map转对象
     * @param map
     * @param beanClass
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T> T mapToBean(Map map, Class<T> beanClass) throws Exception {
        T object = beanClass.newInstance();
        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            int mod = field.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isFinal(mod)) {
                continue;
            }
            field.setAccessible(true);
            if (map.containsKey(field.getName())) {
                field.set(object, map.get(field.getName()));
            }
        }
        return object;
    }

    private static List<Field> getAllField(Object bean) {
        Class clazz = bean.getClass();
        List<Field> fields = new ArrayList<>();
        while (clazz != null) {
            fields.addAll(new ArrayList<>(Arrays.asList(clazz.getDeclaredFields())));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }


    @SneakyThrows
    private  <T extends BasePojo>    List<Column> getColumnByUpdate(Class<?> clazz, T subData) {
        List<Field> fields = loadFields(clazz);
        Field field;
        List<Column> maps = new ArrayList<>();
        PropertyDescriptor descriptor = null;
        Map map = beanToMap(subData);
        for (int i = 0; i < fields.size(); i++) {

            field = fields.get(i);

            //==没有值得就不放入到map里面去了
            if(ObjectUtils.isEmpty(map.get(field.getName()))){
                continue;
            }
//            List<Field> allField = getAllField(subData);
//            if(!allField.contains(field.getName())){
//                continue;
//            }
            String dbType = getColumnType(field.getType());
            if("serialVersionUID".equals(field.getName()) || !StringUtils.hasText(dbType)) continue;
            String columnName = camelCaseToUnderScore(field.getName());
            String defaultValue = getDefaultValue(dbType);
            try {
                descriptor = new PropertyDescriptor(field.getName(),clazz);
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
            maps.add(new Column(columnName,dbType,field.getType(),defaultValue,descriptor == null ? null : descriptor.getReadMethod(), descriptor == null ? null : descriptor.getWriteMethod()));
        }
        return maps;
    }

    private List<Column> getColumn(Class<?> clazz) {
        List<Field> fields = loadFields(clazz);
        Field field;
        List<Column> maps = new ArrayList<>();
        PropertyDescriptor descriptor = null;
        for (int i = 0; i < fields.size(); i++) {
            field = fields.get(i);
            String dbType = getColumnType(field.getType());
            if("serialVersionUID".equals(field.getName()) || !StringUtils.hasText(dbType)) continue;
            String columnName = camelCaseToUnderScore(field.getName());
            String defaultValue = getDefaultValue(dbType);
            try {
                descriptor = new PropertyDescriptor(field.getName(),clazz);
            } catch (IntrospectionException e) {
                e.printStackTrace();
            }
            maps.add(new Column(columnName,dbType,field.getType(),defaultValue,descriptor == null ? null : descriptor.getReadMethod(), descriptor == null ? null : descriptor.getWriteMethod()));
        }
        return maps;
    }

    public <T extends BasePojo>T queryOne(T condition) {
        try {
            getConnection();
            Class<T> clazz = (Class<T>) condition.getClass();
            List<Column> columns = getColumn(clazz);
            List<Param> params = buildCondition(condition,columns);
            StringBuffer stringBuffer = new StringBuffer("SELECT * FROM `").append(getTableName(clazz)).append("` ");
            if (CollUtil.isNotEmpty(params)) {
                stringBuffer.append(" WHERE ");
                Param param;
                for (int i = 0; i < params.size(); i++) {
                    param = params.get(i);
                    if (i > 0) {
                        stringBuffer.append(" AND ");
                    }
                    stringBuffer.append("`").append(param.columnName).append("`").append("=").append("?");
                }
            }
            stringBuffer.append(" ORDER BY add_time DESC LIMIT 1;");
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuffer.toString());
            printlnSql("===============>  "+stringBuffer.toString(),params);
            if (CollUtil.isNotEmpty(params)) {
                fillParams(preparedStatement,params);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            T data;
            ResultSetMetaData metaData = resultSet.getMetaData();
            while(resultSet.next()){
                data = clazz.newInstance();
                resolveData(data,resultSet,columns,metaData);
                results.add(data);
            }
            return CollUtil.isNotEmpty(results) ? results.get(0) : null;
        } catch (InvocationTargetException e) {
            log.error("获取失败，目标方法不存在：{}",e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("获取失败，目标方法不可访问：{}",e);
            return null;
        } catch (UnsupportedDataTypeException e) {
            log.error("获取失败，参数类型不支持：{}",e);
            return null;
        } catch (SQLException e) {
            log.error("SQL处理失败:{}",e);
            return null;
        } catch (InstantiationException e) {
            log.error("查询对象实例化失败:{}",e);
            return null;
        }
    }

    public <T extends BasePojo>List<T> queryMore(T condition) {
        try {
            getConnection();
            Class<T> clazz = (Class<T>) condition.getClass();
            List<Column> columns = getColumn(clazz);
            List<Param> params = buildCondition(condition,columns);
            StringBuffer stringBuffer = new StringBuffer("SELECT * FROM `").append(getTableName(clazz)).append("` ");

            if (CollUtil.isNotEmpty(params)) {
                stringBuffer.append(" WHERE ");
                Param param;
                for (int i = 0; i < params.size(); i++) {
                    param = params.get(i);
                    if (i > 0) {
                        stringBuffer.append(" AND ");
                    }
                    stringBuffer.append("`").append(param.columnName).append("`").append("=").append("?");
                }
            }

            stringBuffer.append(" ORDER BY add_time DESC;");
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuffer.toString());
            printlnSql("===============>  "+stringBuffer.toString(),params);
            if (CollUtil.isNotEmpty(params)) {
                fillParams(preparedStatement,params);
            }
            ResultSet resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            T data;
            ResultSetMetaData metaData = resultSet.getMetaData();
            while(resultSet.next()){
                data = clazz.newInstance();
                resolveData(data,resultSet,columns,metaData);
                results.add(data);
            }
            return results;
        } catch (InvocationTargetException e) {
            log.error("获取失败，目标方法不存在：{}",e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("获取失败，目标方法不可访问：{}",e);
            return null;
        } catch (UnsupportedDataTypeException e) {
            log.error("获取失败，参数类型不支持：{}",e);
            return null;
        } catch (SQLException e) {
            log.error("SQL处理失败:{}",e);
            return null;
        } catch (InstantiationException e) {
            log.error("查询对象实例化失败:{}",e);
            return null;
        }
    }

    /**
     * 直接  queryBySqlAndWhere(DTO,"where aaa > 1")
     * @param condition
     * @param stringBufferSql
     * @param <T>
     * @return
     */
    public <T extends BasePojo>List<T> queryBySqlAndWhere(T condition,String stringBufferSql ) {
        try {
            getConnection();
            Class<T> clazz = (Class<T>) condition.getClass();
            List<Column> columns = getColumn(clazz);
            List<Param> params = buildCondition(condition,columns);
            StringBuffer stringBuffer = new StringBuffer("SELECT * FROM `").append(getTableName(clazz)).append("` ");
            stringBuffer.append(stringBufferSql+";");
            printlnSql("===============>  "+stringBuffer.toString(),params);
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuffer.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            T data;
            ResultSetMetaData metaData = resultSet.getMetaData();
            while(resultSet.next()){
                data = clazz.newInstance();
                resolveData(data,resultSet,columns,metaData);
                results.add(data);
            }
            return results;

        } catch (InvocationTargetException e) {
            log.error("获取失败，目标方法不存在：{}",e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("获取失败，目标方法不可访问：{}",e);
            return null;
        } catch (UnsupportedDataTypeException e) {
            log.error("获取失败，参数类型不支持：{}",e);
            return null;
        } catch (SQLException e) {
            log.error("SQL处理失败:{}",e);
            return null;
        } catch (InstantiationException e) {
            log.error("查询对象实例化失败:{}",e);
            return null;
        }
    }


    /**
     * 直接  queryBySqlAndWhere(DTO,"where aaa > 1"," limit 0,1")
     * @param condition
     * @param stringBufferSql
     * @param <T>
     * @return
     */
    public <T extends BasePojo>List<T> queryBySqlAndWhere(T condition,String stringBufferSql ,String limit) {
        try {
            getConnection();
            Class<T> clazz = (Class<T>) condition.getClass();
            List<Column> columns = getColumn(clazz);
            List<Param> params = buildCondition(condition,columns);
            StringBuffer stringBuffer = new StringBuffer("SELECT * FROM `").append(getTableName(clazz)).append("` ");
            stringBuffer.append(stringBufferSql);
            stringBuffer.append(" ORDER BY add_time DESC ");
            stringBuffer.append(limit+" ;");
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuffer.toString());
            printlnSql("===============>  "+stringBuffer.toString(),params);
            ResultSet resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            T data;
            ResultSetMetaData metaData = resultSet.getMetaData();
            while(resultSet.next()){
                data = clazz.newInstance();
                resolveData(data,resultSet,columns,metaData);
                results.add(data);
            }
            return results;

        } catch (InvocationTargetException e) {
            log.error("获取失败，目标方法不存在：{}",e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("获取失败，目标方法不可访问：{}",e);
            return null;
        } catch (UnsupportedDataTypeException e) {
            log.error("获取失败，参数类型不支持：{}",e);
            return null;
        } catch (SQLException e) {
            log.error("SQL处理失败:{}",e);
            return null;
        } catch (InstantiationException e) {
            log.error("查询对象实例化失败:{}",e);
            return null;
        }
    }


    public <T extends BasePojo>List<T> queryAll(T condition) {
        try {
            getConnection();
            Class<T> clazz = (Class<T>) condition.getClass();
            List<Column> columns = getColumn(clazz);
            List<Param> params = buildCondition(condition,columns);
            StringBuffer stringBuffer = new StringBuffer("SELECT * FROM `").append(getTableName(clazz)).append("` ");

            stringBuffer.append(" ORDER BY add_time DESC;");
            PreparedStatement preparedStatement = connection.prepareStatement(stringBuffer.toString());

            ResultSet resultSet = preparedStatement.executeQuery();
            List<T> results = new ArrayList<>();
            T data;
            ResultSetMetaData metaData = resultSet.getMetaData();
            while(resultSet.next()){
                data = clazz.newInstance();
                resolveData(data,resultSet,columns,metaData);
                results.add(data);
            }
            return results;
        } catch (InvocationTargetException e) {
            log.error("获取失败，目标方法不存在：{}",e);
            return null;
        } catch (IllegalAccessException e) {
            log.error("获取失败，目标方法不可访问：{}",e);
            return null;
        } catch (UnsupportedDataTypeException e) {
            log.error("获取失败，参数类型不支持：{}",e);
            return null;
        } catch (SQLException e) {
            log.error("SQL处理失败:{}",e);
            return null;
        } catch (InstantiationException e) {
            log.error("查询对象实例化失败:{}",e);
            return null;
        }
    }

    private <T extends BasePojo,O> void resolveData(T data, ResultSet resultSet, List<Column> columns, ResultSetMetaData metaData) throws SQLException, InvocationTargetException, IllegalAccessException {
        int columnCount = metaData.getColumnCount();
        Column column;
        O val;
        //  List<Column> basePojoColumns = getColumn(BasePojo.class);

        for (int i = 0; i < columnCount; i++) {
            String columnName = metaData.getColumnName(i + 1);
            column = columns.stream().filter(c -> columnName.equals(c.getName())).findFirst().orElse(null);
            if(column==null){
                // column = basePojoColumns.stream().filter(c -> columnName.equals(c.getName())).findFirst().orElse(null);
            }
            if(column == null) continue;
            val = getSqlColumnVal(column,resultSet,i + 1);
            column.getWriteMethod().invoke(data,val);
        }
    }

    private <O> O getSqlColumnVal(Column column, ResultSet resultSet, int columnIndex) throws SQLException {
        return (O)Convert.convert(column.getJavaType(),resultSet.getObject(columnIndex));
    }

    private <T extends BasePojo> List<Param> buildCondition(T condition, List<Column> columns) throws InvocationTargetException, IllegalAccessException, UnsupportedDataTypeException {
        List<Param> params = new ArrayList<>();
        for (Column column : columns) {
            if (column.readMethod.getModifiers() == Modifier.PUBLIC) {
                Object val = column.readMethod.invoke(condition);
                if(val != null){
                    params.add(new Param(getSqlType(column.dbType),val,column.getName()));
                }
            }
        }
        return params;
    }



    class Column {
        private String name;
        private String dbType;
        private Class<?> javaType;
        private String defaultVal;
        private Method readMethod;
        private Method writeMethod;

        public String getName() {
            return name;
        }

        public String getDbType() {
            return dbType;
        }

        public Class<?> getJavaType() {
            return javaType;
        }

        public String getDefaultVal() {
            return defaultVal;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setDbType(String dbType) {
            this.dbType = dbType;
        }

        public void setJavaType(Class<?> javaType) {
            this.javaType = javaType;
        }

        public void setDefaultVal(String defaultVal) {
            this.defaultVal = defaultVal;
        }

        public Column(String name, String dbType, Class<?> javaType, String defaultVal, Method readMethod, Method writeMethod) {
            this.name = name;
            this.dbType = dbType;
            this.javaType = javaType;
            this.defaultVal = defaultVal;
            this.readMethod = readMethod;
            this.writeMethod = writeMethod;
        }

        public Column() {
        }

        public Method getReadMethod() {
            return readMethod;
        }

        public void setReadMethod(Method readMethod) {
            this.readMethod = readMethod;
        }

        public Method getWriteMethod() {
            return writeMethod;
        }

        public void setWriteMethod(Method writeMethod) {
            this.writeMethod = writeMethod;
        }
    }

    class Param<V>{
        private int dbType;
        private V value;
        private String columnName;

        public Param(V value,int dbType) {
            this.value = value;
            this.dbType = dbType;
        }

        public Param(int dbType, V value, String columnName) {
            this.dbType = dbType;
            this.value = value;
            this.columnName = columnName;
        }

        public Param(V value, String columnName) {
            this.value = value;
            this.columnName = columnName;
        }

        public Param() {
        }

        public V getValue() {
            return value;
        }

        public int getDbType() {
            return dbType;
        }
    }
}
