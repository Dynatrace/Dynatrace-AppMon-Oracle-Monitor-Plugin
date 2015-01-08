package com.dynatrace.diagnostics.plugins;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.sql.*;
import java.util.logging.Level;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;

import jxl.Image;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import com.dynatrace.diagnostics.pdk.Status.StatusCode;
import com.dynatrace.diagnostics.plugins.domain.LocksMetrics;
import com.dynatrace.diagnostics.plugins.domain.ObjectStatus;
import com.dynatrace.diagnostics.plugins.domain.SqlMetrics;
import com.dynatrace.diagnostics.plugins.domain.TablespacesMetrics;
import com.dynatrace.diagnostics.plugins.utils.HelperUtils;

public class OraclePlugin {
	
	static public final String DEFAULT_ENCODING = System.getProperty("file.encoding","UTF-8");
	static public final String SGA_STATEMENT = "select FREE_BUFFER_WAIT, WRITE_COMPLETE_WAIT, BUFFER_BUSY_WAIT, DB_BLOCK_CHANGE, DB_BLOCK_GETS, CONSISTENT_GETS, PHYSICAL_READS, PHYSICAL_WRITES from v$buffer_pool_statistics";
	static public final String BUFFER_RATIO_BUFFER_CACHE_STATEMENT = "SELECT ROUND ( (congets.VALUE + dbgets.VALUE - physreads.VALUE)  * 100    / (congets.VALUE + dbgets.VALUE),  2   ) VALUE  FROM v$sysstat congets, v$sysstat dbgets, v$sysstat physreads  WHERE congets.NAME = 'consistent gets'  AND dbgets.NAME = 'db block gets'  AND physreads.NAME = 'physical reads'  ";
	static public final String BUFFER_RATIO_EXEC_NOPARSE_STATEMENT = "SELECT DECODE (SIGN (ROUND ( (ec.VALUE - pc.VALUE)  * 100  / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  ),  -1, 0,  ROUND ( (ec.VALUE - pc.VALUE)  * 100    / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  )  VALUE FROM v$sysstat ec, v$sysstat pc  WHERE ec.NAME = 'execute count'  AND pc.NAME IN ('parse count', 'parse count (total)')  ";
	static public final String BUFFER_RATIO_MEMORY_SORT_STATEMENT = "SELECT ROUND ( ms.VALUE  / DECODE ((ds.VALUE + ms.VALUE), 0, 1, (ds.VALUE + ms.VALUE))  * 100,    2  ) VALUE FROM v$sysstat ds, v$sysstat ms  WHERE ms.NAME = 'sorts (memory)' AND ds.NAME = 'sorts (disk)'  ";
	static public final String BUFFER_RATIO_SQLAREA_STATEMENT = "SELECT ROUND (gethitratio * 100, 2) VALUE FROM v$librarycache  WHERE namespace = 'SQL AREA'";
	static public final String SESSION_STATEMENT = "select SESSIONS_MAX, SESSIONS_CURRENT, SESSIONS_HIGHWATER, USERS_MAX from v$license";
	static public final String INSERT_TOP_SQL = "INSERT INTO TOP_SQL (TIMESLICE, SQL_ID, SRC_DB_NAME_ID, EXECUTIONS, ELAPSED_TIME, CPU_TIME, DISK_READS, DIRECT_WRITES, BUFFER_GETS, ROWS_PROCESSED, PARSE_CALLS, FIRST_LOAD_TIME, LAST_LOAD_TIME, CHILD_NUMBER) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	static public final String INSERT_TOP_SQL_FULLTEXT_ORACLE = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, SYSDATE, SYSDATE)";
	static public final String INSERT_TOP_SQL_FULLTEXT_SQL_SERVER = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, GETDATE(), GETDATE())";
	static public final String INSERT_TOP_SQL_FULLTEXT_POSTGRESQL = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	static public final String INSERT_TOP_SQL_EXPLAIN_PLAN = "INSERT INTO TOP_SQL_EXPLAIN_PLAN (SQL_ID, TIMESLICE, CHILD_NUMBER, SRC_DB_NAME_ID, EXPLAIN_PLAN_ID) VALUES (?, ?, ?, ?, ?)";
	static public final String INSERT_SRC_DB_NAME_ORACLE = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, SYSDATE, SYSDATE)";
	static public final String INSERT_SRC_DB_NAME_SQL_SERVER = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, GETDATE(), GETDATE())";
	static public final String INSERT_SRC_DB_NAME_POSTGRESQL = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	static public final String INSERT_T_EXPLAIN_PLAN_ORACLE = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)";
	static public final String INSERT_T_EXPLAIN_PLAN_SQL_SERVER = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
	static public final String INSERT_T_EXPLAIN_PLAN_POSTGRESQL = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	static public final String INSERT_LOCKS = "INSERT INTO T_LOCKS (TIMESLICE, SRC_DB_NAME_ID, SID_SERIAL, ORA_USER, OBJECT_NAME, OBJECT_TYPE, LOCK_MODE, STATUS, LAST_DDL) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
	static public final String INSERT_TABLESPACES = "INSERT INTO T_TABLESPACES (TIMESLICE, SRC_DB_NAME_ID, NAME, TOTAL, USED, FREE, PERCENT_USED, PERCENT_FREE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	static public final String CHECK_SID = "select sql_id from top_sql_fulltext where sql_id = ?";
//	static public final String EXISTS_EXPLAIN_PLAN = "select explain_plan_id from t_explain_plan where dbms_lob.compare(sql_explain_plan, to_clob(?)) = 0";
	static public final String GET_NEXT_EXPLAIN_PLAN_ID_ORACLE = "select explain_plan_seq.nextval from dual";
	static public final String GET_NEXT_EXPLAIN_PLAN_ID_SQL_SERVER = "select (next value for dbo.explain_plan_seq)";
	static public final String GET_NEXT_EXPLAIN_PLAN_ID_SQL_POSTGRESQL = "select nextval('explain_plan_seq')"; 
	static public final String GET_NEXT_SRC_DB_NAME_ID_ORACLE = "select src_db_name_seq.nextval from dual";
	static public final String GET_NEXT_SRC_DB_NAME_ID_SQL_SERVER = "select (next value for dbo.src_db_name_seq)";
	static public final String GET_NEXT_SRC_DB_NAME_ID_POSTGRESQL = "select nextval('src_db_name_seq')"; 
	static public final String BUILD_EXPLAIN_PLAN_1 = "EXPLAIN PLAN SET STATEMENT_ID = '";
	static public final String BUILD_EXPLAIN_PLAN_2 = "' FOR ";
	static public final String BUILD_SELECT_EXPLAIN_PLAN_1 = "SELECT PLAN_TABLE_OUTPUT FROM TABLE (DBMS_XPLAN.DISPLAY('PLAN_TABLE', '";
	static public final String BUILD_SELECT_EXPLAIN_PLAN_2 =  "', 'TYPICAL'))";
	static public final String BUILD_DELETE_EXPLAIN_PLAN = "delete from plan_table where statement_id = ?";
	static public final String GET_EXPLAIN_PLAN_VSQL_PLAN = "SELECT * FROM table(DBMS_XPLAN.DISPLAY_CURSOR(?,?))";
	static public final String LINE_SEPARATOR = "\n";
	static public final String PLAN_HASH_VALUE_PATTERN = "Plan hash value:";
	static public final String CHECK_EXPLAIN_PLAN_ID_PLAN_HASH_VALUE = "select explain_plan_id from T_EXPLAIN_PLAN where plan_hash_value = ?";
	static public final String CHECK_EXPLAIN_PLAN_ID_SID_CHILD_NUMBER = "select EXPLAIN_PLAN_ID from T_EXPLAIN_PLAN where sql_id = ? and child_number = ?";
	static public final String UPDATE_EXPLAIN_PLAN_ACCESS_DATE = "update T_EXPLAIN_PLAN set access_date = sysdate where explain_plan_id = ? ";
	static public final String UPDATE_SRC_DB_NAME_ACCESS_DATE_ORACLE = "update T_SRC_DB_NAME set access_date = sysdate where SRC_DB_NAME_ID = ? ";
	static public final String UPDATE_SRC_DB_NAME_ACCESS_DATE_SQL_SERVER = "update T_SRC_DB_NAME set access_date = GETDATE() where SRC_DB_NAME_ID = ? ";
	static public final String UPDATE_SRC_DB_NAME_ACCESS_DATE_POSTGRESQL = "update T_SRC_DB_NAME set access_date = LOCALTIMESTAMP where SRC_DB_NAME_ID = ? ";
	static public final String GET_TIMESTAMP_ORACLE = "select sysdate from dual";
	static public final String GET_TIMESTAMP_SQL_SERVER = "select GETDATE()";
	static public final String GET_TIMESTAMP_POSTGRESQL = "select LOCALTIMESTAMP";
	static public final String UPDATE_TOP_SQL_FULLTEXT_ORACLE = "update top_sql_fulltext set access_date = sysdate where sql_id = ?";
	static public final String UPDATE_TOP_SQL_FULLTEXT_SQL_SERVER = "update top_sql_fulltext set access_date = GETDATE() where sql_id = ?";
	static public final String UPDATE_TOP_SQL_FULLTEXT_POSTGRESQL = "update top_sql_fulltext set access_date = LOCALTIMESTAMP where sql_id = ?";
	// XML tags for the Slow SQL statements
	public static final String GET_SLOW_SQL_SQL_STATEMENT_1 = "SELECT * FROM"
	    	+ " (SELECT hash_value sql_id, elapsed_time, cpu_time, sql_fulltext sql_fulltext, child_number, disk_reads, direct_writes, executions,"
	    	+ " first_load_time, last_load_time, parse_calls, buffer_gets, rows_processed FROM v$sql ORDER BY ";
	public static final String GET_SLOW_SQL_SQL_STATEMENT_2 = " DESC) WHERE ROWNUM <= ";
	
	public static final String GET_LOCKS_SQL_STATEMENT = "SELECT l.session_id||','||v.serial# sid_serial,"
		       + "   l.ORACLE_USERNAME ora_user,"
		       + "   o.object_name," 
		       + "   o.object_type," 
		       + "   DECODE(l.locked_mode,"
		       + "   0, 'None',"
		       + "   1, 'Null',"
		       + "   2, 'Row-S (SS)',"
		       + "   3, 'Row-X (SX)',"
		       + "   4, 'Share',"
		       + "   5, 'S/Row-X (SSX)',"
		       + "   6, 'Exclusive'," 
		       + "   TO_CHAR(l.locked_mode)"
		       + "   ) lock_mode,"
		       + "   o.status," 
		       + "   to_char(o.last_ddl_time,'mm.dd.yyyy hh24:mi:ss') last_ddl"
		       + " FROM dba_objects o, gv$locked_object l, v$session v"
		       + " WHERE o.object_id = l.object_id"
		       + " and l.SESSION_ID=v.sid"
		       + " order by 2,3";
	public final static String GET_TABLESPACE_STATS_SQL_STATEMENT = "SELECT"
			+ " t.tablespace											AS \"NAME\","
			+ " t.totalspace                                            AS \"TOTAL\","
			+ " ROUND((t.totalspace-fs.freespace),2)                    AS \"USED\","
			+ " fs.freespace                                            AS \"FREE\","
			+ " ROUND(((t.totalspace-fs.freespace)/t.totalspace)*100,2) AS \"PERCENT_USED\","
			+ " ROUND((fs.freespace /t.totalspace)*100,2)               AS \"PERCENT_FREE\""
			+ " FROM"
			+ " (SELECT ROUND(SUM(d.bytes)) AS totalspace,"
			+ " d.tablespace_name TABLESPACE"
			+ " FROM dba_data_files d"
			+ " GROUP BY d.tablespace_name"
			+ ") t,"
			+ " (SELECT ROUND(SUM(f.bytes)) AS freespace,"
			+ " f.tablespace_name TABLESPACE"
			+ " FROM dba_free_space f"
			+ " GROUP BY f.tablespace_name"
			+ ") fs"
			+ " WHERE t.tablespace=fs.tablespace"
			+ " ORDER BY t.tablespace";
	public final static Map<String, Double> LOCK_MODE_MAP = Collections.unmodifiableMap(new HashMap<String, Double>() {
		private static final long serialVersionUID = -4867569384183231001L;
	{
		put("None", 0.);
		put("Null", 1.);
		put("Row-S (SS)", 2.);
		put("Row-X (SX)", 3.);
		put("Share", 4.);
		put("Null", 5.);
		put("Null", 6.);
		}});
	public final static Map<String, Double> STATUS_MAP = Collections.unmodifiableMap(new HashMap<String, Double>() {
		private static final long serialVersionUID = -4867569384183231001L;
	{
		put("VALID", 1.);
		put("INVALID", 2.);
		put("N/A", 3.);
		}});
	
	// initial XML string length
	public static final int XML_INITIAL_LENGTH = 5120; // 5K
		
	// First line of the XML file
	public static final String XML_LINE_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	public static final NumberFormat FORMATTER = new DecimalFormat("#,##0.000");
    public static final NumberFormat FORMATTER_LONG = new DecimalFormat("#,##0");
    public static final NumberFormat FORMATTER_SIZE = new DecimalFormat("#,##0.0##");
    public static final double IN_SECONDS = 10000000.0;
    public static final String XSL_SQL_FILE = "res/OracleSqlSS.xsl";
    public static final String XSL_LOCKS_FILE = "res/OracleLocksSS.xsl";
    public static final String XSL_TABLESPACES_FILE = "res/OracleTablespacesSS.xsl";
    public static final String JS_SORT_JAR_LOCATION = "res/sorttable.js";
    public static final String JS_SORT_NAME = "sorttable.js";
    private byte[] xslSqls;
    private byte[] xslLocks;
    private byte[] xslTablespaces;
		
	public static final String SQLS_OPEN_TAG = "<SQLS>";
	public static final String SQLS_CLOSE_TAG = "</SQLS>";
	public static final String SQL_OPEN_TAG = "<SQL>";
	public static final String SQL_CLOSE_TAG = "</SQL>";
	public static final String HD_OPEN_TAG = "<HD>";
	public static final String HD_CLOSE_TAG = "</HD>";
	public static final String DATE_OPEN_TAG = "<DATE>";
	public static final String DATE_CLOSE_TAG = "</DATE>";
	public static final String SQL_ID_OPEN_TAG = "<SQL_ID>";
	public static final String SQL_ID_CLOSE_TAG = "</SQL_ID>";
	public static final String SEQ_NUMBER_OPEN_TAG = "<SEQ_NUMBER>";
	public static final String SEQ_NUMBER_CLOSE_TAG = "</SEQ_NUMBER>";
	public static final String ELAPSED_TIME_OPEN_TAG = "<ELAPSED_TIME>";
	public static final String ELAPSED_TIME_CLOSE_TAG = "</ELAPSED_TIME>";
	public static final String AVERAGE_ELAPSED_TIME_OPEN_TAG = "<AVERAGE_ELAPSED_TIME>";
	public static final String AVERAGE_ELAPSED_TIME_CLOSE_TAG = "</AVERAGE_ELAPSED_TIME>";
	public static final String CPU_TIME_OPEN_TAG = "<CPU_TIME>";
	public static final String CPU_TIME_CLOSE_TAG = "</CPU_TIME>";
	public static final String AVERAGE_CPU_TIME_OPEN_TAG = "<AVERAGE_CPU_TIME>";
	public static final String AVERAGE_CPU_TIME_CLOSE_TAG = "</AVERAGE_CPU_TIME>";
	public static final String SQL_FULLTEXT_OPEN_TAG = "<SQL_FULLTEXT>";
	public static final String SQL_FULLTEXT_CLOSE_TAG = "</SQL_FULLTEXT>";
	public static final String CHILD_NUMBER_OPEN_TAG = "<CHILD_NUMBER>";
	public static final String CHILD_NUMBER_CLOSE_TAG = "</CHILD_NUMBER>";
	public static final String DISK_READS_OPEN_TAG = "<DISK_READS>";
	public static final String DISK_READS_CLOSE_TAG = "</DISK_READS>";
	public static final String EXECUTIONS_OPEN_TAG = "<EXECUTIONS>";
	public static final String EXECUTIONS_CLOSE_TAG = "</EXECUTIONS>";
	public static final String FIRST_LOAD_TIME_OPEN_TAG = "<FIRST_LOAD_TIME>";
	public static final String FIRST_LOAD_TIME_CLOSE_TAG = "</FIRST_LOAD_TIME>";
	public static final String LAST_LOAD_TIME_OPEN_TAG = "<LAST_LOAD_TIME>";
	public static final String LAST_LOAD_TIME_CLOSE_TAG = "</LAST_LOAD_TIME>";
	public static final String PARSE_CALLS_OPEN_TAG = "<PARSE_CALLS>";
	public static final String PARSE_CALLS_CLOSE_TAG = "</PARSE_CALLS>";
	public static final String DIRECT_WRITES_OPEN_TAG = "<DIRECT_WRITES>";
	public static final String DIRECT_WRITES_CLOSE_TAG = "</DIRECT_WRITES>";
	public static final String BUFFER_GETS_OPEN_TAG = "<BUFFER_GETS>";
	public static final String BUFFER_GETS_CLOSE_TAG = "</BUFFER_GETS>";
	public static final String ROWS_PROCESSED_OPEN_TAG = "<ROWS_PROCESSED>";
	public static final String ROWS_PROCESSED_CLOSE_TAG = "</ROWS_PROCESSED>";
	
	// XML tags for the LOCKS SQL statement
	public static final String LOCKS_OPEN_TAG = "<LOCKS>";
	public static final String LOCKS_CLOSE_TAG = "</LOCKS>";
	public static final String LOCK_OPEN_TAG = "<LOCK>";
	public static final String LOCK_CLOSE_TAG = "</LOCK>";
	public static final String SID_SERIAL_OPEN_TAG = "<SID_SERIAL>";
	public static final String SID_SERIAL_CLOSE_TAG = "</SID_SERIAL>";
	public static final String ORA_USER_OPEN_TAG = "<ORA_USER>";
	public static final String ORA_USER_CLOSE_TAG = "</ORA_USER>";
	public static final String OBJECT_NAME_OPEN_TAG = "<OBJECT_NAME>";
	public static final String OBJECT_NAME_CLOSE_TAG = "</OBJECT_NAME>";
	public static final String OBJECT_TYPE_OPEN_TAG = "<OBJECT_TYPE>";
	public static final String OBJECT_TYPE_CLOSE_TAG = "</OBJECT_TYPE>";
	public static final String LOCK_MODE_OPEN_TAG = "<LOCK_MODE>";
	public static final String LOCK_MODE_CLOSE_TAG = "</LOCK_MODE>";
	public static final String STATUS_OPEN_TAG = "<STATUS>";
	public static final String STATUS_CLOSE_TAG = "</STATUS>";
	public static final String LAST_DDL_OPEN_TAG = "<LAST_DDL>";
	public static final String LAST_DDL_CLOSE_TAG = "</LAST_DDL>";
	
	// XML tags for the TABLESPACES SQL statement
	public static final String TABLESPACES_OPEN_TAG = "<TABLESPACES>";
	public static final String TABLESPACES_CLOSE_TAG = "</TABLESPACES>";
	public static final String TABLESPACE_OPEN_TAG = "<TABLESPACE>";
	public static final String TABLESPACE_CLOSE_TAG = "</TABLESPACE>";
	public static final String NAME_OPEN_TAG = "<NAME>";
	public static final String NAME_CLOSE_TAG = "</NAME>";
	public static final String TOTAL_OPEN_TAG = "<TOTAL>";
	public static final String TOTAL_CLOSE_TAG = "</TOTAL>";
	public static final String USED_OPEN_TAG = "<USED>";
	public static final String USED_CLOSE_TAG = "</USED>";
	public static final String FREE_OPEN_TAG = "<FREE>";
	public static final String FREE_CLOSE_TAG = "</FREE>";
	public static final String PERCENT_USED_OPEN_TAG = "<PERCENT_USED>";
	public static final String PERCENT_USED_CLOSE_TAG = "</PERCENT_USED>";
	public static final String PERCENT_FREE_OPEN_TAG = "<PERCENT_FREE>";
	public static final String PERCENT_FREE_CLOSE_TAG = "</PERCENT_FREE>";

    private static String CONFIG_IS_SERVICE_NAME = "isServiceName";
    private static String CONFIG_SERVICE_NAME = "serviceName";
	private static String CONFIG_DB_HOST_NAME = "hostName";
    private static String CONFIG_DB_NAME = "dbName";
    private static String CONFIG_DB_USERNAME = "dbUsername";
    private static String CONFIG_DB_PASSWORD = "dbPassword";
    private static String CONFIG_PORT = "dbPort";
    private static String CONFIG_TOP_SQLS = "topSqls";
    private static String CONFIG_IS_EXPLAIN_PLAN = "isExplainPlan";
    private static String CONFIG_IS_DYNAMIC_MEASURES = "isDynamicMeasures";
    private static String CONFIG_TYPE_OF_SLOWNESS = "typeOfSlowness";
    private static String CONFIG_HTML_FILE_SQLS = "htmlFileSqls";
    private static String CONFIG_HTML_FILE_LOCKS = "htmlFileLocks";
    private static String CONFIG_HTML_FILE_TABLESPACES = "htmlFileTablespaces";
    // History tables
    private static String CONFIG_IS_HISTORY_ON = "isHistoryOn";
    private static String CONFIG_DB_TYPE_HISTORY = "dbTypeHistory";
    // Oracle tables
    private static String CONFIG_IS_SERVICE_NAME_HISTORY = "isServiceNameHistory";
    private static String CONFIG_SERVICE_NAME_HISTORY = "serviceNameHistory";
    private static String CONFIG_DB_HOST_NAME_HISTORY = "hostNameHistory";
    private static String CONFIG_DB_NAME_ORACLE_HISTORY = "dbNameOracleHistory";
    // MS SQLServer
    private static String CONFIG_DB_NAME_SQLSERVER_HISTORY = "dbNameSQLServerHistory";
    // PostgreSql
    private static String CONFIG_DB_NAME_POSTGRESQL_HISTORY = "dbNamePostgreSQLHistory";
    // variables for all DBs for history tables
    private static String CONFIG_DB_USERNAME_HISTORY = "dbUsernameHistory";
    private static String CONFIG_DB_PASSWORD_HISTORY = "dbPasswordHistory";
    private static String CONFIG_PORT_HISTORY = "dbPortHistory";
    
    // db names
    private static String DB_NAME_ORACLE = "Oracle";
    private static String DB_NAME_SQLSERVER = "SQLServer";
    private static String DB_NAME_POSTGRESQL = "PostgreSQL";
    
    private String SGA_METRIC_GROUP = "Oracle SGA";
    private String SESSION_METRIC_GROUP = "Oracle Sessions";
    private String SYSTEM_METRIC_GROUP = "Oracle System";
    private String SQL_METRIC_GROUP = "Oracle SQL";
    private String SQL_SPLIT_NAME = "Sql Name";
    private String LOCKS_METRIC_GROUP = "Oracle Locks";
    private String LOCKS_SPLIT_NAME = "Lock Name";
    private String TABLESPACES_METRIC_GROUP = "Oracle Tablespaces";
    private String TABLESPACES_SPLIT_NAME = "Tablespace Name";
    
    public String FREE_BUFFER_WAIT = "Free Buffer Waits";
    public String WRITE_COMPLETE_WAIT = "Write Complete Waits";
    public String BUFFER_BUSY_WAIT = "Buffer Busy Waits";
    public String DB_BLOCK_CHANGE = "DB Block Changes";
    public String DB_BLOCK_GETS = "DB Block Gets";
    public String CONSISTENT_GETS = "Consistent Gets";
    public String PHYSICAL_READS = "Physical Reads";
    public String PHYSICAL_WRITES = "Physical Writes";
    public String BUFFER_CACHE_HIT_RATIO = "Buffer Cache Hit Ratio";
    public String EXEC_NO_PARSE_RATIO = "Execution Without Parse Ratio";
    public String MEMORY_SORT_RATIO = "Memory Sort Ratio";
    public String SQL_AREA_GET_RATIO = "SQL Area Get Ratio";
    
    static final String[] ORACLE_SQL_METRICS = {"Executions", "Elapsed Time", "Average Elapsed Time", 
    	"CPU Time", "Average CPU Time", "Disk Reads", "Direct Writes", "Buffer Gets", "Rows Processed", "Parse Calls", 
    	"First Load Time", "Last Load Time", "Child Number"};
    static final String[] ORACLE_LOCKS_METRICS = {"LockMode", "Status", "LastDdl"};
    static final String[] ORACLE_TABLESPACES_METRICS = {"Total", "Used", "Free", "Percent Used", "Percent Free"};
 
    public String CONNECTION_TIME = "Connection Time";
    
    public String SESSIONS_MAX = "Maximum Concurrent User Sessions";
    public String SESSIONS_CURRENT = "Current Concurrent User Sessions";
    public String SESSIONS_HIGHWATER = "Highest Concurrent User Sessions";
    public String USERS_MAX = "Maximum Named Users";
    private java.sql.Connection con = null;
    private java.sql.Connection conHistory = null;
    public static final String URL_PREFIX_ORACLE = "jdbc:oracle:thin";
    public static final String URL_PREFIX_SQL_SERVER = "jdbc:sqlserver";
    public static final String URL_PREFIX_POSTGRESQL = "jdbc:postgresql";
    public static final String ORACLE_JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
    public static final String SQL_SERVER_JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String POSTGRESQL_JDBC_DRIVER = "org.postgresql.Driver";
    private  String host;
    private  String dbName;
    private  String userName;
    private  String password;
    private  String port;
    private String dbTypeHistory;
    private  String hostHistory;
    private  String dbNameHistory;
    private  String userNameHistory;
    private  String passwordHistory;
    private  String portHistory;
    private long topSqls;
    private boolean isExplainPlan;
    private boolean isDynamicMeasures;
    private String typeOfSlowness;
    private String htmlFileSqls;
    private String htmlFileLocks;
    private String htmlFileTablespaces;
    private long srcDbNameId = -1;
    private boolean isServiceName;
//    private String serviceName;
    private boolean isServiceNameHistory;
    private String serviceNameHistory;
    private String historyUrl;
    private String oracleUrl;
    private String historyJdbcDriverClass;
    private boolean isHistoryOn;
    public static final int MAX_LENGTH_SQL_TEXT = 100;
	public static final String REPORT_TYPE_URI_HTML = "?type=HTML";
	public static final String REPORT_TYPE_URI_XLS = "?type=XLS";
	public static final String REPORT_TYPE_XLS = "XLS";
	public static final String REPORT_TYPE_HTML = "HTML";
	public static final String REPORT_TYPE_PDF = "PDF";
	public static final String PREPEND_EMBEDDED_IMAGE_SRC = "data:image/png;base64,";
	public static final String HTML_FILE_UPDATED = "_updated";
	public static final String HTML_FILE_SUFFIX = ".html";
	public static final String NA = "n/a";
    private static final Logger log = Logger.getLogger(OraclePlugin.class.getName());
   
    public static String getConnectionUrl(String dbType, String host, String port, String dbName, boolean isService) {
    	log.info("Inside getConnectionUrl method ...");
    	String url = null;
    	if (dbType.equals(DB_NAME_ORACLE)) {
    		// Oracle DBMS
    		if (isService) {
        		url = URL_PREFIX_ORACLE + ":@" + host + ":" + port + "/" + dbName;
        	} else {
        		url = URL_PREFIX_ORACLE + ":@" + host + ":" + port + ":" + dbName;
        	}
    	} else if (dbType.equals(DB_NAME_SQLSERVER)) {
    		// MS SqlServer
    		url = URL_PREFIX_SQL_SERVER + "://" + host + ":" + port + ";DatabaseName=" + dbName + ";";
    	} else if (dbType.equals(DB_NAME_POSTGRESQL)) {
    		// postgreSql
    		url = URL_PREFIX_POSTGRESQL + "://" + host + ":" + port + "/" + dbName;
    	}
    	
    	log.info("getConnectionUrl method: connection string is " + url);
    	return url;
    }
    
    public static String getJdbcDriverClass(String dbType) {
    	if (dbType.equals(DB_NAME_ORACLE)) {
    		return ORACLE_JDBC_DRIVER;
    	} else if (dbType.equals(DB_NAME_SQLSERVER)) {
    		return SQL_SERVER_JDBC_DRIVER;
    	} else if (dbType.equals(DB_NAME_POSTGRESQL)) {
    		return POSTGRESQL_JDBC_DRIVER;
    	}
    	return "";
    }

    public Status setup(PluginEnvironment env) throws Exception {
         log.info("Inside setup method ...");

        //get configuration
        host = env.getConfigString(CONFIG_DB_HOST_NAME);
        isServiceName = env.getConfigBoolean(CONFIG_IS_SERVICE_NAME);
        if (!isServiceName) {
        	dbName = env.getConfigString(CONFIG_DB_NAME);
        } else {
        	dbName = env.getConfigString(CONFIG_SERVICE_NAME);
        }
        userName = env.getConfigString(CONFIG_DB_USERNAME);
        port = (env.getConfigString(CONFIG_PORT) == null) ? "1521" : env.getConfigString(CONFIG_PORT);
        password = env.getConfigPassword(CONFIG_DB_PASSWORD);
        //get configuration for the history database
        isHistoryOn = env.getConfigBoolean(CONFIG_IS_HISTORY_ON); 
        // get type of the database for history tables
        if (isHistoryOn) {
	        dbTypeHistory = env.getConfigString(CONFIG_DB_TYPE_HISTORY);
	        if (dbTypeHistory != null && !(dbTypeHistory = dbTypeHistory.trim()).isEmpty()) {
	        	if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
	        		// Oracle
			        isServiceNameHistory = env.getConfigBoolean(CONFIG_IS_SERVICE_NAME_HISTORY);
			        if (!isServiceNameHistory) {
			        	dbNameHistory = env.getConfigString(CONFIG_DB_NAME_ORACLE_HISTORY);
			        } else {
			        	serviceNameHistory = env.getConfigString(CONFIG_SERVICE_NAME_HISTORY);
			        	dbNameHistory = serviceNameHistory;
			        }
	        	} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
	        		// SqlServer
	        		dbNameHistory = env.getConfigString(CONFIG_DB_NAME_SQLSERVER_HISTORY);
	        		log.info("setup method: dbNameHistory is '" + dbNameHistory + "'");
	        	} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
	        		// PostgreSql
	        		dbNameHistory = env.getConfigString(CONFIG_DB_NAME_POSTGRESQL_HISTORY);
	        	} else {
	        		String msg = "setup method: dbTypeHistory does not contain correct name of the DBMS. dbTypeHistory is '" + dbTypeHistory + "'";
	        		return new Status(Status.StatusCode.ErrorInfrastructure, msg, msg); 
	        	}
	        } else {
	        	String msg = "setup method: dbTypeHistory variable is null or empty";
	        	return new Status(Status.StatusCode.ErrorInfrastructure, msg, msg);
	        }
	        hostHistory = env.getConfigString(CONFIG_DB_HOST_NAME_HISTORY);
	        userNameHistory = env.getConfigString(CONFIG_DB_USERNAME_HISTORY) == null ? "null" : env.getConfigString(CONFIG_DB_USERNAME_HISTORY).trim();
	        portHistory = (env.getConfigString(CONFIG_PORT_HISTORY) == null) ? "1521" : env.getConfigString(CONFIG_PORT_HISTORY).trim();
	        passwordHistory = env.getConfigPassword(CONFIG_DB_PASSWORD_HISTORY);
        }
        xslSqls = getFile(XSL_SQL_FILE).getBytes(DEFAULT_ENCODING);
        xslLocks = getFile(XSL_LOCKS_FILE).getBytes(DEFAULT_ENCODING);
        xslTablespaces = getFile(XSL_TABLESPACES_FILE).getBytes(DEFAULT_ENCODING);
        String sortTable = getFile(JS_SORT_JAR_LOCATION);
	    topSqls = (env.getConfigLong(CONFIG_TOP_SQLS) == null) ? 10 : env.getConfigLong(CONFIG_TOP_SQLS);
	    isExplainPlan = (env.getConfigBoolean(CONFIG_IS_EXPLAIN_PLAN) == null) ? false : env.getConfigBoolean(CONFIG_IS_EXPLAIN_PLAN);
	    isDynamicMeasures = (env.getConfigBoolean(CONFIG_IS_DYNAMIC_MEASURES) == null) ? false : env.getConfigBoolean(CONFIG_IS_DYNAMIC_MEASURES);
	    typeOfSlowness = env.getConfigString(CONFIG_TYPE_OF_SLOWNESS);
	    htmlFileSqls =  env.getConfigString(CONFIG_HTML_FILE_SQLS);
		try {
			if (htmlFileSqls != null && !htmlFileSqls.isEmpty()) {
				String jsFileName = new StringBuilder(
						FilenameUtils.getFullPath(htmlFileSqls)).append(
						JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName),
						sortTable, "UTF-8");
			} else {
				String msg;
				log.severe(msg = "setup method: htmlFileSqls is null or empty");
				return new Status(StatusCode.ErrorInternalConfigurationProblem, msg, msg);
			}
			htmlFileLocks = env.getConfigString(CONFIG_HTML_FILE_LOCKS);
			if (htmlFileLocks != null && !htmlFileLocks.isEmpty()) {
				String jsFileName = new StringBuilder(FilenameUtils.getFullPath(htmlFileLocks)).append(JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName), sortTable, "UTF-8");
			} else {
				String msg;
				log.severe(msg = "setup method: htmlFileLocks is null or empty");
				return new Status(StatusCode.ErrorInternalConfigurationProblem, msg, msg);
			}
			htmlFileTablespaces = env.getConfigString(CONFIG_HTML_FILE_TABLESPACES);
			if (htmlFileTablespaces != null && !htmlFileTablespaces.isEmpty()) {
				String jsFileName = new StringBuilder(FilenameUtils.getFullPath(htmlFileTablespaces)).append(JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName), sortTable, DEFAULT_ENCODING);
			} else {
				String msg;
				log.severe(msg = "setup method: htmlFileTablespaces is null or empty");
				return new Status(StatusCode.ErrorInternalConfigurationProblem, msg, msg);
			}
		} catch (Exception e) {
			log.severe("setup method: exception occurred '" + HelperUtils.getExceptionAsString(e) + "'");
		}
		
		if (log.isLoggable(Level.INFO)) {
        	log.info("setup method: isServiceName is '" + isServiceName + "'");
     		log.info("setup method: " + (isServiceName ? "serviceName is '" : "dbName is '") + dbName + "'");
        	log.info("setup method: host is '" + host + "'");
           	log.info("setup method: port is '" + port + "'");
        	log.info("setup method: userName is '" + userName + "'");
         	log.info("setup method: password is '" + password + "'");
         	if (isHistoryOn) {
	         	log.info("setup method: dbTypeHistory is '" + dbTypeHistory + "'");
	          	if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
	         		log.info("setup method: isServiceNameHistory is '" + isServiceNameHistory + "'");
	         		log.info("setup method: " + (isServiceNameHistory ? "serviceNameHistory is '" : "dbNameHistory is '") + dbNameHistory + "'");
	         	} else {
	         		log.info("setup method: dbNameHistory is '" + dbNameHistory + "'");
	         	}
	         	log.info("setup method: hostHistory is '" + hostHistory + "'");
	           	log.info("setup method: portHistory is '" + portHistory + "'");
	        	log.info("setup method: userNameHistory is '" + userNameHistory + "'");
	         	log.info("setup method: passwordHistory is '" + passwordHistory + "'");
         	}
			log.info("setup method: xslSqls is '" + new String(xslSqls) + "'");
			log.info("setup method: xslLocks is '" + new String(xslLocks) + "'");
			log.info("setup method: topSqls is '" + topSqls + "'");
			log.info("setup method: typeOfSlowness is '" + typeOfSlowness + "'");
			log.info("setup method: htmlFileSqls is '" + htmlFileSqls + "'");
			log.info("setup method: htmlFileLocks is '" + htmlFileLocks + "'");
        }
		
        Status stat;
	    // get connection to the monitored database
	    try {
	        log.info("setup method: Connecting to Oracle ...");
	       	oracleUrl = getConnectionUrl(DB_NAME_ORACLE, host, port, dbName, isServiceName);
	        log.info("setup method: Connection string is ... " + oracleUrl);
	        log.info("setup method: Opening database connection ...");
	        Class.forName(ORACLE_JDBC_DRIVER);
	        con = java.sql.DriverManager.getConnection(oracleUrl, userName, password);
	        stat = new Status();
	    } catch (ClassNotFoundException e) {
	    	log.log(Level.SEVERE, e.getMessage(), e);
	      	return getErrorStatus(e);
	    } catch (SQLException e) {
	       	log.log(Level.SEVERE, e.getMessage(), e);
	        return getErrorStatus(e);
	    } finally {
	      	// do nothing here
	    }
	        
	    // get connection to the history database
	    if (isHistoryOn) {
	        try {
	            log.info("setup method: Connecting to the History Database...");
	            historyUrl = getConnectionUrl(dbTypeHistory, hostHistory, portHistory, dbNameHistory, isServiceNameHistory);
	            log.info("setup method: Connection string for History Database is ... " + historyUrl);
	            log.info("setup method: Opening database connection for History Database...");
	            historyJdbcDriverClass = getJdbcDriverClass(dbTypeHistory);
	            Class.forName(historyJdbcDriverClass); 
	            conHistory = java.sql.DriverManager.getConnection(historyUrl, userNameHistory, passwordHistory);
	            stat = new Status();
	        } catch (ClassNotFoundException e) {
	        	log.log(Level.SEVERE, e.getMessage(), e);
	        	return getErrorStatus(e);
	         } catch (SQLException e) {
	        	log.log(Level.SEVERE, e.getMessage(), e);
	            return getErrorStatus(e);
	        } finally {
	        	// do nothing here
	        }
	        
	        // check if dbName exists in the history database
	        srcDbNameId = getSrcDbNameId(conHistory, dbName);
		}
        
        return stat;
    }
    
    private long getSrcDbNameId(Connection con, String dbName) throws Exception {
    	// check if dbName is in the T_SRC_DB_NAME table
    	long id; 
    	PreparedStatement ps = null;
    	id = checkSrcDbNameId(con, dbName);
    	if (id < 0) {
    		// insert src_db_name in the T_src_db_name table
			String nextQuery = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				nextQuery = GET_NEXT_SRC_DB_NAME_ID_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				nextQuery = GET_NEXT_SRC_DB_NAME_ID_SQL_SERVER;
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				nextQuery = GET_NEXT_SRC_DB_NAME_ID_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '"	+ dbTypeHistory);
				throwRuntimeException(msg);
			}
    		id = getNextSeqId(con, nextQuery);
    		if (id >= 0) {
	    		try {
			    	con.setAutoCommit(true);
					String insertQuery = null;
					if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
						insertQuery = INSERT_SRC_DB_NAME_ORACLE;
					} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
						insertQuery = INSERT_SRC_DB_NAME_SQL_SERVER;
					} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
						insertQuery = INSERT_SRC_DB_NAME_POSTGRESQL;
					} else {
						String msg;
						log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory);
						throwRuntimeException(msg);
					}
					ps = con.prepareStatement(insertQuery);
					ps.setLong(1, id);
					ps.setString(2,  dbName);
					ps.executeUpdate();
					ps.clearParameters();
	    		} catch (Exception e) {
	    			log.severe("execute method: '" + HelperUtils.getExceptionAsString(e) + "'");
	    			throw e;
	    		} finally {
	    			try {
	    				if (ps != null)
	    					ps.close();
	    			} catch (SQLException e) {
	    				// do nothing
	    			}
	    		}
    		} else {
    			throw new Exception("getSrcDbNameId method: getNextSeqId method returned negative number '" + id + "' for the src_db_name_seq");
    		}
    	}
    	
    	return id;
    	 
    }
    
    private long checkSrcDbNameId(Connection con, String dbName) throws SQLException {
    	log.info("Entering checkSrcDbNameId method...");
		long id = Long.MIN_VALUE;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// check if dbName is in the T_SRC_DB_NAME table already
			ps = con.prepareStatement("select src_db_name_id from t_src_db_name where src_db_name = ?");
			ps.setString(1,  dbName);
			rs = ps.executeQuery();
			while (rs.next()) {
				id = rs.getLong(1);
			}
			if (id > 0) {
				log.info("checkSrcDbNameId method: dbName '" + dbName + "' is found. srcDbNameId value is " + id);
				// update access date
				try {
					if (rs != null)
						rs.close();
				} catch (Exception e) {
					// do nothing
				}
				try {
					if (ps != null)
						ps.close();
				} catch (SQLException e) {
					// do nothing
				}
				String updateQuery = null;
				if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
					updateQuery = UPDATE_SRC_DB_NAME_ACCESS_DATE_ORACLE;
				} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
					updateQuery = UPDATE_SRC_DB_NAME_ACCESS_DATE_SQL_SERVER;
				} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
					updateQuery = UPDATE_SRC_DB_NAME_ACCESS_DATE_POSTGRESQL;
				} else {
					String msg;
					log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory);
					throwRuntimeException(msg);
				}	
				ps = con.prepareStatement(updateQuery);
				ps.setLong(1, id);
				int i = ps.executeUpdate();
				if (i != 1) {
					log.info("checkExplainPlanId method: number of updated records should be 1, but " + i + " records were returned");
				}
			} else {
				log.info("checkSrcDbNameId method: dbName '" + dbName + "' is not found.");
			}
		
		} catch (SQLException e) {
			log.severe("execute method: '" + HelperUtils.getExceptionAsString(e) + "'");
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				// do nothing
			}
		}

		return id;
	}

    synchronized private long getNextSeqId(Connection con, String sql) throws SQLException {
		long id = -1;
		Statement st = null;
		ResultSet rs = null;
		try {
			st = con.createStatement();
			rs = st.executeQuery(sql);

			while (rs.next()) {
				id = rs.getLong(1);
			}
		} catch (SQLException e) {
			log.severe("execute method: '" + HelperUtils.getExceptionAsString(e) + "'");
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// do nothing
			}
		}
		log.info("getNextSeqId method: returning id value " + id);
		return id;
	}
    
    private Status getErrorStatus(Exception e) {
    	Status stat = new Status();
    	stat.setStatusCode(Status.StatusCode.ErrorTargetService);
        stat.setShortMessage(e.getMessage());
        stat.setMessage(e.getMessage());
        stat.setException(e);
        
        return stat;
    }
    
    public Status execute(PluginEnvironment env)  throws Exception {
        Status stat = new Status();

		log.info("Inside execute method ...");
		// Oracle RDBMS metrics
		try {
			populateSGAInfo((MonitorEnvironment)env);
			populateSystemInfo((MonitorEnvironment)env);
			populateSessionInfo((MonitorEnvironment)env);
			stat = new Status();
		} catch (SQLException e) {
			String msg = "execute method: '" + HelperUtils.getExceptionAsString(e) + "'";
        	log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		// Top SQL metrics
		Timestamp timestamp = null;
		if (htmlFileSqls != null && !htmlFileSqls.isEmpty()) {
			// generate slow SQL page
			try {
				if (isHistoryOn) {
					if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
						throw new Exception("execute method: current timestamp should not be null");
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateSqlsInfo((MonitorEnvironment)env, con, conHistory, timestamp, srcDbNameId, isHistoryOn);
	        } catch (Exception e) {
	        	String msg = "execute method: '" + HelperUtils.getExceptionAsString(e) + "'";
	        	log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
	        }
		}
		// Locks metrics	
		if (htmlFileLocks != null && !htmlFileLocks.isEmpty()) {
			// generate Oracle locks page
			try {
				if (isHistoryOn) {
					if (timestamp == null) {
						if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
							throw new Exception("execute method: current timestamp should not be null");
						}
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateLocksInfo((MonitorEnvironment)env, con, conHistory, timestamp, isHistoryOn);
	        } catch (Exception e) {
	        	String msg = "execute method: '" + HelperUtils.getExceptionAsString(e) + "'";
	        	log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
	        }
		}
		// Tablespaces metrics
		//TODO add tablespaces here 
		if (htmlFileTablespaces != null && !htmlFileTablespaces.isEmpty()) {
			// generate Oracle tablespaces page
			try {
				if (isHistoryOn) {
					if (timestamp == null) {
						if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
							throw new Exception("execute method: current timestamp should not be null");
						}
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateTablespacesInfo((MonitorEnvironment)env, con, conHistory, timestamp, isHistoryOn);
	        } catch (Exception e) {
	        	String msg = "execute method: '" + HelperUtils.getExceptionAsString(e) + "'";
	        	log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
	        }
		}

    return stat;
  }
    
	private Timestamp getCurrentTimestamp(Connection con) throws Exception {
		Timestamp timestamp = null;
		ResultSet rs = null;
		Statement st = null;
		try {
			st = con.createStatement();
			String sql = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				sql = GET_TIMESTAMP_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				sql = GET_TIMESTAMP_SQL_SERVER;
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				sql = GET_TIMESTAMP_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory);
				throwRuntimeException(msg);
			}	
			rs = st.executeQuery(sql);
			while (rs.next()) {
				timestamp = rs.getTimestamp(1);
			}
		} catch (Exception e) {
			log.severe("getCurrentTimestamp method: '" + HelperUtils.getExceptionAsString(e) + "'");
			throw e;
		} finally {
			try {
				if (rs != null)	rs.close();
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (st != null) st.close();
			} catch (Exception e) {
				// do nothing
			}
		}
		
		return timestamp;

	}

  public void populateSGAInfo(MonitorEnvironment env) throws SQLException {

        ResultSet sgaResult = null;
        Collection<MonitorMeasure> measures;
        Statement st = null;

        log.info("Inside populateSGAInfo method ...");

        try {
        	st = con.createStatement();
            sgaResult = st.executeQuery(SGA_STATEMENT);
            while (sgaResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, FREE_BUFFER_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating FREE_BUFFER_WAIT ... ");
                        measure.setValue(sgaResult.getDouble("FREE_BUFFER_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, WRITE_COMPLETE_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating WRITE_COMPLETE_WAIT ...");
                        measure.setValue(sgaResult.getDouble("WRITE_COMPLETE_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_BUSY_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating BUFFER_BUSY_WAIT ...");
                        measure.setValue(sgaResult.getDouble("BUFFER_BUSY_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_CHANGE)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating DB_BLOCK_CHANGE ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_CHANGE"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating DB_BLOCK_GETS ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, CONSISTENT_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating CONSISTENT_GETS ...");
                        measure.setValue(sgaResult.getDouble("CONSISTENT_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_READS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating PHYSICAL_READS ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_READS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_WRITES)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating PHYSICAL_WRITES ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_WRITES"));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
            
        } finally {
        	try {
	        	if (sgaResult != null) {
	        		sgaResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }
        
        ResultSet ratioResult = null;
        try {
        	st = con.createStatement();
            ratioResult = st.executeQuery(BUFFER_RATIO_BUFFER_CACHE_STATEMENT);
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_CACHE_HIT_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating BUFFER_CACHE_HIT_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	try {
	        	if (ratioResult != null) {
	        		ratioResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }
        
        try {
        	st = con.createStatement();
            ratioResult = st.executeQuery(BUFFER_RATIO_EXEC_NOPARSE_STATEMENT);
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, EXEC_NO_PARSE_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating EXEC_NO_PARSE_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	try {
	        	if (ratioResult != null) {
	        		ratioResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }
        
        
         try {
        	st = con.createStatement();
            ratioResult = st.executeQuery(BUFFER_RATIO_MEMORY_SORT_STATEMENT);
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, MEMORY_SORT_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating MEMORY_SORT_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	try {
	        	if (ratioResult != null) {
	        		ratioResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }
        
        try {
        	st = con.createStatement();
            ratioResult = st.executeQuery(BUFFER_RATIO_SQLAREA_STATEMENT);
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, SQL_AREA_GET_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.info("populateSGAInfo method: Populating SQL_AREA_GET_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	try {
	        	if (ratioResult != null) {
	        		ratioResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }
 
  }
    
  private Connection getConnection(String host, String port, String dbName, String userName, String password) throws SQLException{
    	// close bad connection
		try {
			if (con != null) {
				con.close();
			}
		} catch (SQLException e) {
			// ignore exception
		}
		
		// re-connect now
    	return DriverManager.getConnection(oracleUrl, userName, password);
  }

  public void populateSystemInfo(MonitorEnvironment env) throws SQLException {

        Collection<MonitorMeasure> measures = null;
        double timeBefore = 0;
        double timeAfter = 0;
        double totalConnectionTime = 0;
        Connection timerCon = null;

        log.info("Inside populateSystemInfo method ...");
        
        try {
            log.info("populateSystemInfo method: Connecting to Oracle ...");
            log.info("populateSystemInfo method: Connection string is ... " + oracleUrl);
            log.info("populateSystemInfo method: Opening database connection ...");
            timeBefore = System.currentTimeMillis();
            timerCon = java.sql.DriverManager.getConnection(oracleUrl, userName, password);
            timeAfter = System.currentTimeMillis();
            totalConnectionTime = timeAfter - timeBefore;
            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, CONNECTION_TIME)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.info("populateSystemInfo method: Populating CONNECTION_TIME ... ");
                    measure.setValue(totalConnectionTime);
                }
            }
        } catch (SQLException e) {
        	log.severe("populateSystemInfo method: " + HelperUtils.getExceptionAsString(e));
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	log.info("populateSystemInfo method: Closing database connection ...");
        	try {
	        	if (timerCon != null) {
	        		timerCon.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }

  }

  public void populateSessionInfo(MonitorEnvironment env) throws SQLException {

        ResultSet sessionResult = null;
        Collection<MonitorMeasure> measures;
        Statement st = null;


        log.info("Inside populateSessionInfo method ...");

        try {
        	st = con.createStatement();
            sessionResult = st.executeQuery(SESSION_STATEMENT);
            sessionResult.next();
            //while (sessionResult.next()) {
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.info("populateSessionInfo method: Populating SESSIONS_MAX ... ");
                    measure.setValue(sessionResult.getDouble("SESSIONS_MAX"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_CURRENT)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.info("populateSessionInfo method: Populating SESSIONS_CURRENT ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_CURRENT"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_HIGHWATER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.info("populateSessionInfo method: Populating SESSIONS_HIGHWATER ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_HIGHWATER"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, USERS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.info("populateSessionInfo method: Populating USERS_MAX ...");
                    measure.setValue(sessionResult.getDouble("USERS_MAX"));
                }
            }
        } catch (SQLException e) {
        	log.severe("populateSessionInfo method: " + HelperUtils.getExceptionAsString(e));
            
            // re-connect
            try {
				con = getConnection(host, port, dbName, userName, password);
			} catch (SQLException e1) {
				// re-throw original exception
				throw e;
			}
        } finally {
        	try {
	        	if (sessionResult != null) {
	        		sessionResult.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        	
        	try {
	        	if (st != null) {
	        		st.close();
	        	}
        	} catch(SQLException e){
        		// ignore exception
        	}
        }

  }

  public void teardown(PluginEnvironment env) throws Exception {
        log.info("teardown method: Exiting Oracle Monitor Plugin ... ");
        con.close();
  }
    
  private void populateSqlsInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, long dbNameId, boolean isHistoryOn) throws Exception {

        ResultSet rs = null;
        Statement st = null;
        FileWriterWithEncoding fw = null;
//        List<SqlMetrics> sqls = new ArrayList<SqlMetrics>();

        log.info("Inside populateSqlsInfo method ...");

        try {
        	st = con.createStatement();
        	//build SQL
        	String sql = new StringBuilder(GET_SLOW_SQL_SQL_STATEMENT_1).append(typeOfSlowness).append(GET_SLOW_SQL_SQL_STATEMENT_2).append(topSqls).toString();
        	log.info("populateSqlsInfo method: sql is '" + sql + "'");
            rs = st.executeQuery(sql);
            
            List<SqlMetrics> sqls = getTopSqls(dbNameId, rs);
            // release resources
           	rs.close();
            st.close();
             
            if (isHistoryOn) {
	            if (isExplainPlan) {
	            	getExplainPlansVSqlPlan(con, sqls);
	            }
	            
	            // insert data into TOP_SQL_XXX tables
	            insertTopSqlData(conHistory, sqls, timestamp);
	            insertTopSqlFulltext(conHistory, sqls);
	           	insertExplainPlan(conHistory, sqls, timestamp);
            }
			// build html page and write it someplace on the server
			String sqlsHTML = buildSqlsXML(sqls);
			
			// create dynamic measures for a list of sql statements' measures
			if (isDynamicMeasures) {
				log.info("populateSqlsInfo method: sqls list is '" + Arrays.toString(sqls.toArray()));
				populateSqlMeasures(env, sqls);
			}
			
			log.info(sqlsHTML);
			
			fw = new FileWriterWithEncoding(htmlFileSqls, DEFAULT_ENCODING);
			
			fw.write(sqlsHTML, 0, sqlsHTML.length());
			fw.flush();
			fw.close();
			
        } catch (SQLException e) {
        	log.severe("populateSqlsInfo method: " + HelperUtils.getExceptionAsString(e));
            throw e;
        } finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (fw != null) {
					fw.close();
				}
			} catch (Exception e) {
				// ignore exceptions
			}
        }
  }
  
  private void getExplainPlansVSqlPlan(Connection con, List<SqlMetrics> sqls) throws Exception {
	  PreparedStatement ps = null;
	  ResultSet rs = null;
	  try {
		  StringBuilder sb = null;
		  ps = con.prepareStatement(GET_EXPLAIN_PLAN_VSQL_PLAN);
		  for (SqlMetrics sql : sqls) {
			  try {
				  ps.setString(1,  sql.getSid());
				  ps.setLong(2, sql.getChildNumber().longValue());
				  rs = ps.executeQuery();
				  
				  sb = new StringBuilder();
				  while (rs.next()) {
					  sb.append(rs.getString(1)).append(LINE_SEPARATOR);
				  }
				  
			  } catch (Exception e) {
				  // do nothing
				  log.info("getExplainPlansVSqlPlan method: exception is '" + HelperUtils.getExceptionAsString(e) + "'");
			  }
			  
			  if (sb == null) {
				  sb = new StringBuilder(NA).append(LINE_SEPARATOR); 
			  } else if (sb.toString().isEmpty()) {
				  sb.append(NA).append(LINE_SEPARATOR);
			  }
			  
			  sql.setExplainPlan(sb.toString());
		  }
	  } catch (Exception e) {
		  log.info("getExplainPlansVSqlPlan method: exception is '" + HelperUtils.getExceptionAsString(e) + "'");
		  throw e;
	  }finally {
		  try {
			  if (rs != null) rs.close();
		  } catch (Exception e) {
			  // do nothing
		  }
		  
		  try {
			  if (ps != null) ps.close();
		  } catch (Exception e) {
			  // do nothing
		  }
		  
	  }
  }
  
  private long checkExplainPlanId(Connection con, SqlMetrics sql) {
		log.info("Entering checkExplainPlanId method...");
		long explainPlanId = Long.MIN_VALUE;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Long planHashValue;
		try {
			if ((planHashValue = sql.getPlanHashValue()) != Long.MIN_VALUE) {
				// plan_hash_value is extracted
				ps = con.prepareStatement(CHECK_EXPLAIN_PLAN_ID_PLAN_HASH_VALUE);
				ps.setLong(1,  planHashValue);
				rs = ps.executeQuery();
				while (rs.next()) {
					explainPlanId = rs.getLong(1);
				}
				log.info("checkExplainPlanId method: planHashValue is '" + sql.getPlanHashValue() + ", explainPlanId is " + explainPlanId);
			} else {
				ps = con.prepareStatement(CHECK_EXPLAIN_PLAN_ID_SID_CHILD_NUMBER);
				ps.setString(1, sql.getSid());
				ps.setLong(2, sql.getChildNumber().longValue());
				rs = ps.executeQuery();
	
				while (rs.next()) {
					explainPlanId = rs.getLong(1);
				}
				
				log.info("checkExplainPlanId method: sid is '" + sql.getSid() + "', childNumber is " + sql.getChildNumber() + ", explainPlanId is " + explainPlanId);
			}
			
			if (explainPlanId > 0) {
				// update access date
				try {
					if (rs != null)
						rs.close();
				} catch (Exception e) {
					// do nothing
				}
				try {
					if (ps != null)
						ps.close();
				} catch (SQLException e) {
					// do nothing
				}
				
				ps = con.prepareStatement(UPDATE_EXPLAIN_PLAN_ACCESS_DATE);
				ps.setLong(1,  explainPlanId);
				int i = ps.executeUpdate();
				if (i != 1) {
					log.info("checkExplainPlanId method: number of updated records should be 1, but " + i + " records were returned");
				}
				
			}
		} catch (SQLException e) {
			// do nothing
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException e) {
				// do nothing
			}
		}

		return explainPlanId;
	}

	private void insertExplainPlan(Connection con, List<SqlMetrics> sqls, Timestamp timestamp) throws Exception {
	  
	  PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_TOP_SQL_EXPLAIN_PLAN);
			long id ;
			for (SqlMetrics sql : sqls) {
				  if (sql.getExplainPlan() == null || sql.getExplainPlan().isEmpty()) {
					  sql.setExplainPlan(NA);
				  }
				  // get PLAN_HASH_VALUE
				  sql.setPlanHashValue(extractPlanHashValue(sql.getExplainPlan())); 
				  log.info("insertExplainPlan method: planHashValue is " + sql.getPlanHashValue());
				  
				  // check if T_EXPLAIN_PLAN table has already this explain plan
				  if ((id = checkExplainPlanId(con, sql)) < 0) {
					  // get new id
					  String nextQuery = null;
					  if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
						  nextQuery = GET_NEXT_EXPLAIN_PLAN_ID_ORACLE;
					  } else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
						  nextQuery = GET_NEXT_EXPLAIN_PLAN_ID_SQL_SERVER;
					  } else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
						  nextQuery = GET_NEXT_EXPLAIN_PLAN_ID_SQL_POSTGRESQL;
					  } else {
						  String msg;
						  log.severe(msg = "insertExplainPlan method: incorrect dbTypeHistory '" + dbTypeHistory);
						  throwRuntimeException(msg);
					  }
					  id = getNextSeqId(con, nextQuery);
					  sql.setExplainPlanId(id);
					  insertExplainPlanTable(con, sql);
				  } else {
					  sql.setExplainPlanId(id);
				  }
				  // insert into top_sql_explain_plan table reference on the existing explain plan id
				  ps.setString(1, sql.getSid());
				  ps.setTimestamp(2, timestamp);
				  ps.setLong(3, sql.getChildNumber().longValue());
				  ps.setLong(4,  sql.getSrcDbNameId());
				  ps.setLong(5, id);
				  ps.executeUpdate();
				  ps.clearParameters();
			  }
			  
		} catch (SQLException e) {
			log.severe("insertExplainPlan method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertExplainPlan method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {
				// do nothing	
			}
		}
	}

	private void throwRuntimeException(String msg) {
		throw new RuntimeException(msg);
	}
	
	private Long extractPlanHashValue(String explainPlan) {
		String planHashValueString;
		long returnValue;
		int i;
		if ((i = explainPlan.indexOf(PLAN_HASH_VALUE_PATTERN)) >= 0) { 
			int j;
			if ((j = explainPlan.indexOf("\n", i + PLAN_HASH_VALUE_PATTERN.length() + 1)) > 0) {
				planHashValueString = explainPlan.substring(i + PLAN_HASH_VALUE_PATTERN.length() + 1, j - 1).trim();
				log.info("extractPlanHashValue method: planHashValueString is '" + planHashValueString + "'");
				if (!planHashValueString.isEmpty()) {
					try {
						returnValue = Long.parseLong(planHashValueString);
					} catch (NumberFormatException e) {
						returnValue = Long.MIN_VALUE;
					}
				} else {
					returnValue = Long.MIN_VALUE;
				}
			} else {
				returnValue = Long.MIN_VALUE;
			}
		} else {
			returnValue = Long.MIN_VALUE;
		}
		
		log.info("extractPlanHashValue method: returning returnValue '" + returnValue + "'");
		
		return returnValue;
	}
  
//	synchronized private long getExplainPlanId(Connection con) {
//		long explainPlanId = -1;
//		Statement st = null;
//		ResultSet rs = null;
//		try {
//			st = con.createStatement();
//			rs = st.executeQuery(GET_NEXT_EXPLAIN_PLAN_ID);
//
//			while (rs.next()) {
//				explainPlanId = rs.getLong(1);
//			}
//		} catch (SQLException e) {
//			// do nothing
//		} finally {
//			try {
//				if (rs != null) {
//					rs.close();
//				}
//			} catch (Exception e) {
//				// do nothing
//			}
//			try {
//				if (st != null) {
//					st.close();
//				}
//			} catch (SQLException e) {
//				// do nothing
//			}
//		}
//		log.info("getExplainPlanId method: returning explainPlanId value " + explainPlanId);
//		return explainPlanId;
//	}
  
	private void insertExplainPlanTable(Connection con, SqlMetrics sql) throws Exception {
	  
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			String insertQuery = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				insertQuery = INSERT_T_EXPLAIN_PLAN_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				insertQuery = INSERT_T_EXPLAIN_PLAN_SQL_SERVER;
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				insertQuery = INSERT_T_EXPLAIN_PLAN_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory);
				throwRuntimeException(msg);
			}
			ps = con.prepareStatement(insertQuery);
			ps.setLong(1, sql.getExplainPlanId());
			ps.setLong(2, sql.getPlanHashValue());
			ps.setString(3,  sql.getSid());
			ps.setLong(4,  sql.getSrcDbNameId());
			ps.setLong(5, sql.getChildNumber().longValue());
			ps.setString(6, sql.getExplainPlan());
			ps.executeUpdate();
			ps.clearParameters();
		} catch (SQLException e) {
			log.severe("insertTopSqlFulltext method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertTopSqlFulltext method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {
				// do nothing
			}
		}
		  
	}
  
  private void insertTopSqlData(Connection con, List<SqlMetrics> sqls, Timestamp timestamp) throws Exception {
	  
	PreparedStatement ps = null;
	try {
		con.setAutoCommit(true);
		ps = con.prepareStatement(INSERT_TOP_SQL);
		  for (SqlMetrics sql : sqls) {
			  ps.setTimestamp(1, timestamp);
			  ps.setString(2, sql.getSid());
			  ps.setLong(3,  sql.getSrcDbNameId());
			  ps.setLong(4, sql.getExecutions().longValue());
			  ps.setDouble(5,  sql.getElapsedTime());
			  ps.setDouble(6, sql.getCpuTime());
			  ps.setDouble(7, sql.getDiskReads());
			  ps.setDouble(8, sql.getDirectWrites());
			  ps.setDouble(9, sql.getBufferGets());
			  ps.setDouble(10, sql.getRowsProcessed());
			  ps.setDouble(11,  sql.getParseCalls());
			  ps.setString(12, sql.getFirstLoadTime());
			  ps.setString(13,  sql.getLastLoadTime());
			  ps.setDouble(14,  sql.getChildNumber());		  
			  ps.executeUpdate();
			  ps.clearParameters();
		  }
		  
	} catch (SQLException e) {
		log.severe("insertTopSqlData method: " + HelperUtils.getExceptionAsString(e));
		throw e;
	} catch (Exception e) {
		log.severe("insertTopSqlData method: " + HelperUtils.getExceptionAsString(e));
		throw e;
	} finally {
		try {
			if (ps != null) ps.close();
		} catch (Exception e) {
			// do nothing	
		}
	}
	  
  }
  
	private void insertTopSqlFulltext(Connection con, List<SqlMetrics> sqls)
			throws Exception {

		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			String insertSql = null;
			String updateSql = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				insertSql = INSERT_TOP_SQL_FULLTEXT_ORACLE;
				updateSql = UPDATE_TOP_SQL_FULLTEXT_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				insertSql = INSERT_TOP_SQL_FULLTEXT_SQL_SERVER;
				updateSql = UPDATE_TOP_SQL_FULLTEXT_SQL_SERVER;
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				insertSql = INSERT_TOP_SQL_FULLTEXT_POSTGRESQL;
				updateSql = UPDATE_TOP_SQL_FULLTEXT_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory);
				throwRuntimeException(msg);
			}
			ps = con.prepareStatement(insertSql);
			for (SqlMetrics sql : sqls) {
				if (isSqlSidInserted(con, sql.getSid())) {
					PreparedStatement ps1 = null;
					try {
						ps1 = con.prepareStatement(updateSql);
						ps1.setString(1, sql.getSid());
						int i = ps1.executeUpdate();
						if (i != 1) {
							log.info("insertTopSqlFulltext method: update statement should return 1 record but it returned " + i);
						}
					} catch (SQLException e) {
						// do nothing
					} finally {
						try {
							if (ps1 != null) {
								ps1.close();
							}
						} catch (Exception e) {
							// do nothing
						}
					}

					continue;
				}
				ps.setString(1, sql.getSid());
				ps.setLong(2, sql.getSrcDbNameId());
				ps.setString(3, sql.getSqlFulltext());
				ps.executeUpdate();
				ps.clearParameters();
			}

		} catch (SQLException e) {
			log.severe("insertTopSqlFulltext method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertTopSqlFulltext method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (Exception e) {
				// do nothing
			}
		}

	}
  
	private boolean isSqlSidInserted(Connection con, String sid) {
		boolean result = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(CHECK_SID);
			ps.setString(1, sid);
			rs = ps.executeQuery();

			while (rs.next()) {
				result = true;
			}
		} catch (SQLException e) {
			// do nothing
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (ps != null) {
					ps.close();
				}
			} catch (SQLException e) {
				// do nothing
			}
		}

		return result;
	}
  
  private List<SqlMetrics> getTopSqls(long dbNameId, ResultSet rs) throws SQLException {
	  String s;
	  List<SqlMetrics> list = new ArrayList<SqlMetrics>();
	  while(rs.next()){
		  SqlMetrics sm = new SqlMetrics();
		  sm.setSid(rs.getString("SQL_ID"));
		  sm.setSrcDbNameId(dbNameId);
		  sm.setSqlFulltext(rs.getString("SQL_FULLTEXT"));
		  sm.setExecutions(rs.getDouble("EXECUTIONS"));
		  sm.setElapsedTime(rs.getDouble("ELAPSED_TIME")/IN_SECONDS);
		  // calculate average_elapsed_time
		  if (sm.getExecutions() > 0.) {
			  sm.setAverageElapsedTime((sm.getElapsedTime() / sm.getExecutions()));
		  } else {
			  sm.setAverageElapsedTime(Double.NaN);
		  }
		  sm.setCpuTime(rs.getDouble("CPU_TIME")/IN_SECONDS);
		  // calculate average_cpu_time
		  if (sm.getExecutions() > 0.) {
				sm.setAverageCpuTime((sm.getCpuTime() / sm.getExecutions()));
		  } else {
			  sm.setAverageCpuTime(Double.NaN);
		  }
		  sm.setDiskReads(rs.getDouble("DISK_READS"));
		  sm.setDirectWrites(rs.getDouble("DIRECT_WRITES"));
		  sm.setBufferGets(rs.getDouble("BUFFER_GETS"));
		  sm.setRowsProcessed(rs.getDouble("ROWS_PROCESSED"));
		  sm.setParseCalls(rs.getDouble("PARSE_CALLS"));
		  sm.setFirstLoadTime(s = rs.getString("FIRST_LOAD_TIME"));
		  sm.setFirstLoadTimeDate(HelperUtils.getOracleDate(s));
		  sm.setLastLoadTime(s = rs.getString("LAST_LOAD_TIME"));
		  sm.setLastLoadTimeDate(HelperUtils.getOracleDate(s));
		  sm.setChildNumber(rs.getDouble("CHILD_NUMBER"));
		  list.add(sm);
	  }
	  
	  return list;
	  
  }
    
  private String buildSqlsXML(final List<SqlMetrics> sqls) throws Exception{
	  	log.info("Inside buildSqlsXML method ...");
	  	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");   	
    	StringBuilder xmlSb = new StringBuilder(XML_INITIAL_LENGTH);
    	Map<String, String> keys = new HashMap<String, String>();
    		
    	xmlSb.append(XML_LINE_1).append(SQLS_OPEN_TAG);
    	
    	// Add date and timestamp
    	xmlSb.append(HD_OPEN_TAG).append(DATE_OPEN_TAG)
    		.append(dateFormat.format(new Date())).append(DATE_CLOSE_TAG)
    		.append(HD_CLOSE_TAG);
    	
    	// process rows from the result set
    	int seqNumber = 0;
    	for(SqlMetrics sql : sqls){
    		xmlSb.append(SQL_OPEN_TAG);
    		
    		// SEQ_NUMBER
    		xmlSb.append(SEQ_NUMBER_OPEN_TAG).append(URLEncoder.encode(String.valueOf(++seqNumber), "ISO-8859-1"))
    			.append(SEQ_NUMBER_CLOSE_TAG);
    		
    		// SQL_ID
    		String sid;
			xmlSb.append(SQL_ID_OPEN_TAG).append(sid = sql.getSid())
					.append(SQL_ID_CLOSE_TAG);
			// check if sid is unique
			sid = HelperUtils.getUniqueKey(keys, sid);
			keys.put(sid, "");
			    		
			// SQL_FULLTEXT
			String sqlFullText = sql.getSqlFulltext();
			if (sqlFullText.length() > 1000) {
				sqlFullText = sqlFullText.substring(0, 1000);
			}
			xmlSb.append(SQL_FULLTEXT_OPEN_TAG).append(URLEncoder.encode(sqlFullText, "UTF-8"))
					.append(SQL_FULLTEXT_CLOSE_TAG);

			// EXECUTIONS
			double executions = sql.getExecutions();
			
     		xmlSb.append(EXECUTIONS_OPEN_TAG).append(FORMATTER_LONG.format((long)executions))
     			.append(EXECUTIONS_CLOSE_TAG);

			// ELAPSED_TIME
     		double elapsedTime = sql.getElapsedTime();
			xmlSb.append(ELAPSED_TIME_OPEN_TAG).append(FORMATTER.format(elapsedTime))
					.append(ELAPSED_TIME_CLOSE_TAG);

			// AVERAGE_ELAPSED_TIME 
//			if (executions > 0) {
//				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(FORMATTER.format(elapsedTime/executions/IN_SECONDS))
//						.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
//			} else {
//				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(NA)
//					.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
//			}
			if (!sql.getAverageElapsedTime().isNaN()) {
				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(FORMATTER.format(sql.getAverageElapsedTime()))
						.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
			} else {
				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(NA)
					.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
			}

			// CPU_TIME
			double cpuTime = sql.getCpuTime();
			xmlSb.append(CPU_TIME_OPEN_TAG).append(FORMATTER.format(cpuTime))
					.append(CPU_TIME_CLOSE_TAG);
			
			// AVERAGE_CPU_TIME
//			if (executions > 0) {
//				xmlSb.append(AVERAGE_CPU_TIME_OPEN_TAG).append(FORMATTER.format(cpuTime/executions/IN_SECONDS))
//						.append(AVERAGE_CPU_TIME_CLOSE_TAG);
//			} else {
//				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(NA)
//				.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
//			}
			if (!sql.getAverageCpuTime().isNaN()) {
				xmlSb.append(AVERAGE_CPU_TIME_OPEN_TAG).append(FORMATTER.format(sql.getAverageCpuTime()))
						.append(AVERAGE_CPU_TIME_CLOSE_TAG);
			} else {
				xmlSb.append(AVERAGE_ELAPSED_TIME_OPEN_TAG).append(NA)
				.append(AVERAGE_ELAPSED_TIME_CLOSE_TAG);
			}

			// DISK_READS
	     		xmlSb.append(DISK_READS_OPEN_TAG).append(FORMATTER_LONG.format(sql.getDiskReads()))
	     			.append(DISK_READS_CLOSE_TAG);
	     		
	     	// DIRECT_WRITES Oracle 11g feature
	     		xmlSb.append(DIRECT_WRITES_OPEN_TAG).append(FORMATTER_LONG.format(sql.getDirectWrites()))
	     			.append(DIRECT_WRITES_CLOSE_TAG);
	     		
	     	// BUFFER_GETS
	     		xmlSb.append(BUFFER_GETS_OPEN_TAG).append(FORMATTER_LONG.format(sql.getBufferGets()))
	     			.append(BUFFER_GETS_CLOSE_TAG);
	     		
	     	// ROWS_PROCESSED
	     		xmlSb.append(ROWS_PROCESSED_OPEN_TAG).append(FORMATTER_LONG.format(sql.getRowsProcessed()))
	     			.append(ROWS_PROCESSED_CLOSE_TAG);
	     		
	     	// PARSE_CALLS
	     		xmlSb.append(PARSE_CALLS_OPEN_TAG).append(FORMATTER_LONG.format(sql.getParseCalls()))
	     			.append(PARSE_CALLS_CLOSE_TAG);	
	     		
	     	// FIRST_LOAD_TIME
	     		xmlSb.append(FIRST_LOAD_TIME_OPEN_TAG).append(sql.getFirstLoadTime())
	     			.append(FIRST_LOAD_TIME_CLOSE_TAG);
	     			     		
	     	// LAST_LOAD_TIME
	     		xmlSb.append(LAST_LOAD_TIME_OPEN_TAG).append(sql.getLastLoadTime())
	     			.append(LAST_LOAD_TIME_CLOSE_TAG);
	     			     		
	     	// CHILD_NUMBER
				xmlSb.append(CHILD_NUMBER_OPEN_TAG).append(sql.getChildNumber()).append(CHILD_NUMBER_CLOSE_TAG);

	     		
	     	// add sql close tag
	     	xmlSb.append(SQL_CLOSE_TAG);
	     	
	     	// setup key
	     	if (sid != null && !sid.isEmpty() && sqlFullText != null && !sqlFullText.isEmpty()) {
	     		int len;
	     		if (sqlFullText.length() > MAX_LENGTH_SQL_TEXT) {
	     			len = MAX_LENGTH_SQL_TEXT;
	     		} else {
	     			len = sqlFullText.length();
	     		}
	     		sql.setKey(new StringBuilder(sid).append("_").append(sqlFullText.substring(0, len)).toString());
	     	}
	     	
    	}
    	
    	// add sqls tag
    	xmlSb.append(SQLS_CLOSE_TAG);
    	
    	log.info("buildSqlsXml method: xml sqls file  is '" + xmlSb.toString());
    	
    	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslSqls);
  }

  private String convertXMLToHTML(byte[] xml, byte[] xsl) throws Exception {
	  	log.info("Inside of convertXMLToHTML method...");
	  	
		ByteArrayOutputStream outStream = new ByteArrayOutputStream(
				XML_INITIAL_LENGTH);
	
		try {
	
			TransformerFactory tFactory = TransformerFactory.newInstance();
	
			Transformer transformer = tFactory
					.newTransformer(new javax.xml.transform.stream.StreamSource(
							new ByteArrayInputStream(xsl)));
	
			transformer.transform(new javax.xml.transform.stream.StreamSource(
					new ByteArrayInputStream(xml)),
					new javax.xml.transform.stream.StreamResult(outStream));
			String s = outStream.toString(DEFAULT_ENCODING).replaceAll("%", "%25");
			return URLDecoder.decode(s, DEFAULT_ENCODING);
		} catch (Exception e) {
			log.log(Level.SEVERE, e.getMessage());
			throw e;
		}
	
  }

  public String getFile(String fileName) throws IOException {
		if (log.isLoggable(Level.INFO)) {
			log.info("Entering getFile method: fileName is '" + fileName + "'");
		}
		URL url = this.getClass().getClassLoader().getResource(fileName);
		return IOUtils.toString(url, System.getProperty("file.encoding"));
  }
	
  private void populateLocksInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, boolean isHistoryOn) throws Exception {

        ResultSet rs = null;
        Statement st = null;
        FileWriterWithEncoding fw = null;
//        List<LocksMetrics> locks = new ArrayList<LocksMetrics>();

        log.info("Inside populateLocksInfo method ...");

        try {
        	st = con.createStatement();
        	
            rs = st.executeQuery(GET_LOCKS_SQL_STATEMENT);
            List<LocksMetrics> locksData = getLocks(rs);
            
            rs.close();
            
            if (isHistoryOn) {
	            // insert locks data into T_LOCKS table
	            insertLocksData(conHistory, locksData, timestamp, srcDbNameId);
            }

			// build html page and write it someplace on the server
			String locksHTML = buildLocksXML(locksData);
			
			log.info("populateLocksInfo method: locksHTML is '" + locksHTML + "'");
			
			// create dynamic measures for a list of lock's measures
			if (isDynamicMeasures) {
				log.info("populateLocksInfo method: locks list is '" + Arrays.toString(locksData.toArray()));
				populateLocksMeasures(env, locksData);
			}
			
			fw = new FileWriterWithEncoding(htmlFileLocks, DEFAULT_ENCODING);
			
			fw.write(locksHTML, 0, locksHTML.length());
			fw.flush();
			fw.close();
			
        } catch (SQLException e) {
            throw e;
        } finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {				
				if (fw != null) {
					fw.close();
				}
			} catch (Exception e) {
				// ignore exceptions
			}
        }
  }
  
	private List<LocksMetrics> getLocks(ResultSet rs) throws SQLException {
		List<LocksMetrics> list = new ArrayList<LocksMetrics>();
		Map<String, String> keys = new HashMap<String, String>();
		String s;
		while (rs.next()) {
			LocksMetrics lm = new LocksMetrics();
			lm.setSidSerial(rs.getString("SID_SERIAL"));
			lm.setOraUser(rs.getString("ORA_USER"));
			lm.setObjectName(rs.getString("OBJECT_NAME"));
			lm.setObjectType(rs.getString("OBJECT_TYPE"));
			lm.setLockMode(s = rs.getString("LOCK_MODE"));
			if (LOCK_MODE_MAP.containsKey(s = s.toUpperCase())) {
				lm.setLockModeMeasure(LOCK_MODE_MAP.get(s));
			} else {
				lm.setLockModeMeasure(Double.NaN);
			}
			lm.setStatus(s = rs.getString("STATUS"));
			lm.setStatusMeasure(Double.valueOf(ObjectStatus.valueOf(s.toUpperCase()).ordinal()));
			lm.setLastDdl(rs.getString("LAST_DDL"));
			//TODO set LAST_DDL as double
			lm.setLastDdlMeasure(Double.NaN);
			// calculate key
     		String key = new StringBuilder(lm.getSidSerial()).append("_").append(lm.getOraUser()).append("_").append(lm.getObjectName()).toString();
     		// check if key is unique
			key = HelperUtils.getUniqueKey(keys, key);
			keys.put(key, "");
			lm.setKey(key);

			list.add(lm);
		}

		return list;
	}
	
	private void insertLocksData(Connection con, List<LocksMetrics> locks, Timestamp timestamp, long dbNameId) throws Exception {
		  
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_LOCKS);
			  for (LocksMetrics lock : locks) {
				  ps.setTimestamp(1, timestamp);
				  ps.setLong(2, dbNameId);
				  ps.setString(3, lock.getSidSerial());
				  ps.setString(4, lock.getOraUser());
				  ps.setString(5, lock.getObjectName());
				  ps.setString(6,  lock.getObjectType());
				  ps.setString(7, lock.getLockMode());
				  ps.setString(8, lock.getStatus());
				  ps.setString(9, lock.getLastDdl());
				  ps.executeUpdate();
				  ps.clearParameters();
			  }
			  
		} catch (SQLException e) {
			log.severe("insertLocksData method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertLocksData method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {
				// do nothing
			}
		}
		  
	  }

  private String buildLocksXML(List<LocksMetrics> locks) throws Exception{
    	
	  	log.info("Inside buildLocksXML method...");
	  	
    	SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
    	
    	StringBuilder xmlSb = new StringBuilder(XML_INITIAL_LENGTH);
    	xmlSb.append(XML_LINE_1).append(LOCKS_OPEN_TAG);
    	
    	// Add date and timestamp
    	xmlSb.append(HD_OPEN_TAG).append(DATE_OPEN_TAG)
    		.append(dateFormat.format(new Date())).append(DATE_CLOSE_TAG)
    		.append(HD_CLOSE_TAG);
    	
    	// process rows from the result set
    	
    	for (LocksMetrics lock : locks){
    		xmlSb.append(LOCK_OPEN_TAG);
    		
    		// sid serial
     		xmlSb.append(SID_SERIAL_OPEN_TAG).append(lock.getSidSerial())
     			.append(SID_SERIAL_CLOSE_TAG);

	     	// ora user
	     		xmlSb.append(ORA_USER_OPEN_TAG).append(lock.getOraUser())
	     			.append(ORA_USER_CLOSE_TAG);
	     		
	     	// object name
	     		xmlSb.append(OBJECT_NAME_OPEN_TAG).append(lock.getObjectName())
	     			.append(OBJECT_NAME_CLOSE_TAG);
	     		
	     	// object type
	     		xmlSb.append(OBJECT_TYPE_OPEN_TAG).append(lock.getObjectType())
	     			.append(OBJECT_TYPE_CLOSE_TAG);
	     	
	     	// lock mode
	     		xmlSb.append(LOCK_MODE_OPEN_TAG).append(lock.getLockMode())
	     			.append(LOCK_MODE_CLOSE_TAG);
	     		
	     	// status
	     		xmlSb.append(STATUS_OPEN_TAG).append(lock.getStatus())
	     			.append(STATUS_CLOSE_TAG);
	     		
	     	// last ddl time
	     		xmlSb.append(LAST_DDL_OPEN_TAG).append(lock.getLastDdl())
	     			.append(LAST_DDL_CLOSE_TAG);
	     		
				// add lock close tag
				xmlSb.append(LOCK_CLOSE_TAG);
    	}
    	
    	// add locks tag
    	xmlSb.append(LOCKS_CLOSE_TAG);
    	
    	log.info("xml locks file  is '" + xmlSb.toString());
    	
    	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslLocks);
    	
    	
  }
  
  private void populateTablespacesInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, boolean isHistoryOn) throws Exception {

      ResultSet rs = null;
      Statement st = null;
      FileWriterWithEncoding fw = null;

      log.finer("Inside populateTablespacesInfo method ...");

      try {
      	st = con.createStatement();
      	
        rs = st.executeQuery(GET_TABLESPACE_STATS_SQL_STATEMENT);
        List<TablespacesMetrics> tablespacesData = getTablespaces(rs);
          
        rs.close();
        
        if (isHistoryOn) {
	        // insert tablespace data into T_TABLESPACES table
	        insertTablespacesData(conHistory, tablespacesData, timestamp, srcDbNameId);
        }

        // build html page and write it someplace on the web server
        String tablespacesHTML = buildTablespacesXML(tablespacesData);
			
		log.finer("populateTablespacesInfo method: tablespacesHTML is '" + tablespacesHTML + "'");
			
		// create dynamic measures for a list of lock's measures
		populateTablespacesMeasures(env, tablespacesData); 
			
		fw = new FileWriterWithEncoding(htmlFileTablespaces, DEFAULT_ENCODING);
			
		fw.write(tablespacesHTML, 0, tablespacesHTML.length());
		fw.flush();
		fw.close();
			
      } catch (SQLException e) {
          throw e;
      } finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (Exception e) {
				// do nothing
			}
			try {				
				if (fw != null) {
					fw.close();
				}
			} catch (Exception e) {
				// ignore exceptions
			}
      }
  }
  
  private List<TablespacesMetrics> getTablespaces(ResultSet rs) throws SQLException {
		List<TablespacesMetrics> list = new ArrayList<TablespacesMetrics>();
		Map<String, String> keys = new HashMap<String, String>();
		while (rs.next()) {
			TablespacesMetrics tm = new TablespacesMetrics();
			tm.setName(rs.getString("NAME"));
			tm.setTotal(rs.getDouble("TOTAL"));
			tm.setUsed(rs.getDouble("USED"));
			tm.setFree(rs.getDouble("FREE"));
			tm.setPercentUsed(rs.getDouble("PERCENT_USED"));
			tm.setPercentFree(rs.getDouble("PERCENT_FREE"));
			// calculate key
			String key = tm.getName();
			// check if key is unique
			key = HelperUtils.getUniqueKey(keys, key);
			keys.put(key, "");
			tm.setKey(key);

			list.add(tm);
		}

		return list;
  }
  
  private void insertTablespacesData(Connection con, List<TablespacesMetrics> tablespaces, Timestamp timestamp, long dbNameId) throws Exception {
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_TABLESPACES);
			for (TablespacesMetrics tablespace : tablespaces) {
				ps.setTimestamp(1, timestamp);
				ps.setLong(2, dbNameId);
				ps.setString(3, tablespace.getName());
				ps.setDouble(4, tablespace.getTotal());
				ps.setDouble(5, tablespace.getUsed());
				ps.setDouble(6, tablespace.getFree());
				ps.setDouble(7, tablespace.getPercentUsed());
				ps.setDouble(8, tablespace.getPercentFree());
				ps.executeUpdate();
				ps.clearParameters();
			}
		} catch (SQLException e) {
			log.severe("insertTablespacesData method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertTablespacesData method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (Exception e) {
				// do nothing
			}
		}
		  
  }
  
  private String buildTablespacesXML(List<TablespacesMetrics> tablespaces) throws Exception{
  	
	log.finer("Inside buildTablespacesXML method...");
	  	
  	SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
  	
  	StringBuilder xmlSb = new StringBuilder(XML_INITIAL_LENGTH);
  	xmlSb.append(XML_LINE_1).append(TABLESPACES_OPEN_TAG);
  	
  	// Add date and timestamp
  	xmlSb.append(HD_OPEN_TAG).append(DATE_OPEN_TAG)
  		.append(dateFormat.format(new Date())).append(DATE_CLOSE_TAG)
  		.append(HD_CLOSE_TAG);
  	
  	// process rows from the result set
  	
  	for (TablespacesMetrics tablespace : tablespaces){
  		xmlSb.append(TABLESPACE_OPEN_TAG);
  		
  		// name
   		xmlSb.append(NAME_OPEN_TAG).append(tablespace.getName())
   			.append(NAME_CLOSE_TAG);

	    // total
	    xmlSb.append(TOTAL_OPEN_TAG).append(FORMATTER_SIZE.format(tablespace.getTotal()/(1024*1024))) // in MB
	     	.append(TOTAL_CLOSE_TAG);
	     		
	    // used
	    xmlSb.append(USED_OPEN_TAG).append(FORMATTER_SIZE.format(tablespace.getUsed()/(1024*1024))) // in MB
	    	.append(USED_CLOSE_TAG);
	     		
	    // free
	    xmlSb.append(FREE_OPEN_TAG).append(FORMATTER_SIZE.format(tablespace.getFree()/(1024*1024))) // in MB
	    	.append(FREE_CLOSE_TAG);
	     	
	    // percent_used
	    xmlSb.append(PERCENT_USED_OPEN_TAG).append(tablespace.getPercentUsed()).append("%")
	     	.append(PERCENT_USED_CLOSE_TAG);
	     		
	    // percent_free
	    xmlSb.append(PERCENT_FREE_OPEN_TAG).append(tablespace.getPercentFree()).append("%")
	    	.append(PERCENT_FREE_CLOSE_TAG);
	     		
		// add lock close tag
		xmlSb.append(TABLESPACE_CLOSE_TAG);
  	}
  	
  	// add tablespaces tag
  	xmlSb.append(TABLESPACES_CLOSE_TAG);
  	
  	log.finer("xml tablespaces file  is '" + xmlSb.toString());
  	
  	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslTablespaces);
  	
  	
  }
  
  private void populateTablespacesMeasures(MonitorEnvironment env, List<TablespacesMetrics> tablespaces) {
	  log.finer("Inside of populateTablespacesMeasures method...");
	  
	  for (int i = 0; i < ORACLE_TABLESPACES_METRICS.length; i++) {
		log.finer("populateTablespacesMeasures method: metric # " + i + ", metric name is '" + ORACLE_TABLESPACES_METRICS[i] + "'");
		for (TablespacesMetrics tablespace : tablespaces) {
			log.finer("populateTablespacesMeasures method: tablespace key is '" + tablespace.getKey() + "'");
			Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(TABLESPACES_METRIC_GROUP, ORACLE_TABLESPACES_METRICS[i]);
			for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
				log.finer("populateTablespacesMeasures method: inside of the subscribedMonitorMeasure loop: tablespace key is '" + tablespace.getKey() + "'");
				MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, TABLESPACES_SPLIT_NAME, tablespace.getKey());
				switch (i) {
					case 0:
						// Total
						dynamicMeasure.setValue(tablespace.getTotal());
						break;
					case 1:
						// Used
						dynamicMeasure.setValue(tablespace.getUsed());
						break;
					case 2:
						// Free
						dynamicMeasure.setValue(tablespace.getFree());
						break;
					case 3:
						// Percent Used
						dynamicMeasure.setValue(tablespace.getPercentUsed());
						break;
					case 4:
						// Percent Free
						dynamicMeasure.setValue(tablespace.getPercentFree());
						break;
					default:
						log.severe("populateTablespacesMeasures method: index " + i + " is unknown. Index skipped");
				}		
			}
		}
	  }
  }
  
  private void populateSqlMeasures(MonitorEnvironment env, List<SqlMetrics> sqls) {
	  log.info("Inside of populateSqlMeasures method...");
	  
	  for (int i = 0; i < ORACLE_SQL_METRICS.length; i++) {
		  log.info("populateSqlMeasures method: metric # " + i + ", metric name is '" + ORACLE_SQL_METRICS[i] + "'");
		  for (SqlMetrics sql : sqls) {
			  log.info("populateSqlMeasures method: sql's sid is '" + sql.getSid() + "', key is '" + sql.getKey() + "'");
			  Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(SQL_METRIC_GROUP, ORACLE_SQL_METRICS[i]);
				for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
					MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, SQL_SPLIT_NAME, sql.getKey());
					switch (i) {
					case 0:
						// Executions
						dynamicMeasure.setValue(sql.getExecutions());
						break;
					case 1:
						// Elapsed Time
						dynamicMeasure.setValue(sql.getElapsedTime());
						break;
					case 2:
						// Average Elapsed Time
//						Double d = Double.NaN;
//						if (sql.getExecutions() > 0.) {
//							d = sql.getElapsedTime()/sql.getExecutions()/IN_SECONDS;
//						}
//						dynamicMeasure.setValue(d);
						dynamicMeasure.setValue(sql.getAverageElapsedTime());
						break;
					case 3:
						// CPU Time
						dynamicMeasure.setValue(sql.getCpuTime());
						break;
					case 4:
						// Average CPU Time
//						d = Double.NaN;
//						if (sql.getExecutions() > 0.) {
//							d = sql.getCpuTime()/sql.getExecutions()/IN_SECONDS;
//						}
//						dynamicMeasure.setValue(d);
						dynamicMeasure.setValue(sql.getAverageCpuTime());
						break;
					case 5:
						// Disk Reads
						dynamicMeasure.setValue(sql.getDiskReads());
						break;
					case 6:
						// Direct Writes
						dynamicMeasure.setValue(sql.getDirectWrites());
						break;
					case 7:
						// Buffer Gets
						dynamicMeasure.setValue(sql.getBufferGets());
						break;
					case 8:
						// Rows Processed
						dynamicMeasure.setValue(sql.getRowsProcessed());
						break;
					case 9:
						// Parse Calls
						dynamicMeasure.setValue(sql.getParseCalls());
						break;
					case 10:
						// First Load Time
						Date d;
						dynamicMeasure.setValue((d = sql.getFirstLoadTimeDate()) == null ? Double.NaN : HelperUtils.getLongOracleDateTime(d));
						break;
					case 11:
						// Last Load Time
						dynamicMeasure.setValue((d = sql.getLastLoadTimeDate()) == null ? Double.NaN : HelperUtils.getLongOracleDateTime(d));
						break;
					case 12:
						// Child Number
						dynamicMeasure.setValue(sql.getChildNumber());
						break;
					default:
						log.severe("populateSqlMeasures method: index " + i + " is unknown. Index skipped");
					}		
				}
		  }
	  }
  }
	  
  private void populateLocksMeasures(MonitorEnvironment env, List<LocksMetrics> locks) {
	  log.info("Inside of populateLocksMeasures method...");
	  
		  for (int i = 0; i < ORACLE_LOCKS_METRICS.length; i++) {
			  log.info("populateLocksMeasures method: metric # " + i + ", metric name is '" + ORACLE_LOCKS_METRICS[i] + "'");
			  for (LocksMetrics lock : locks) {
				  log.info("populateLocksMeasures method: lock's key is '" + lock.getKey() + "'");
				  Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(LOCKS_METRIC_GROUP, ORACLE_LOCKS_METRICS[i]);
					for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
						MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, LOCKS_SPLIT_NAME, lock.getKey());
						switch (i) {
						case 0:
							// lock mode
							dynamicMeasure.setValue(lock.getLockModeMeasure());
							break;
						case 1:
							// Status
							dynamicMeasure.setValue(lock.getStatusMeasure());
							break;
						case 2:
							// last ddl
							dynamicMeasure.setValue(lock.getLastDdlMeasure());
							break;
						default:
							log.severe("populateLocksMeasures method: index " + i + " is unknown. Index skipped");
						}		
					}
			  }
		  }
  }
  
  public static String replaceImagesIntoHtmlPage(List<String> list, String page) {
	if (log.isLoggable(Level.INFO)) {
		log.info("Entering replaceImagesIntoHtmlPage method");
	}
	Document doc = Jsoup.parse(page);
    Elements srces = doc.select("[src]");

    int i = 0;
    for (Element src : srces) {
        if (src.tagName().equals("img")) {
        	if (i >=list.size()) {
          		log.warning("replaceImagesIntoHtmlPage method: image '" + (i + 1) + "' on the HTML page exceeds size '" + list.size() + "' of extracted images from the Excel spreadsheet.");
          		continue;
          	}
          	src.attr("src", list.get(i++));
        }
        else {
       	// skip non-img tags where src attribute is present
          	continue;
        }
    }
//      if (log.isLoggable(Level.INFO)) {
//           log.info("replaceImagesIntoHtmlPage method: modified page is '" + doc.html() + "'");
//		}
     
    return doc.html();
  }
  
  public static List<String> getImagesFromExcelReport(Workbook wb, URL footerUrl) throws IOException {
		if (log.isLoggable(Level.INFO)) {
			log.info("Entering getImagesFromExcelReport method");
		}
		List<String> list = new ArrayList<String>();
		for (int i = 0; i < wb.getNumberOfSheets(); i++) {
			Sheet s = wb.getSheet(i);
			for (int j = 0; j < s.getNumberOfImages(); j++) {
				Image c = s.getDrawing(j);
				list.add(new StringBuilder(PREPEND_EMBEDDED_IMAGE_SRC).append(new String(Base64.encodeBase64(c.getImageData()), Charset.defaultCharset())).toString());
			}
		}
		
		// add footer images which Excel does not have
		list.add(new StringBuilder(PREPEND_EMBEDDED_IMAGE_SRC).append(getImageAsString(footerUrl)).toString());

		return list; 
  }
  
  public static String getImageAsString(URL url) throws IOException {
		if (log.isLoggable(Level.INFO)) {
			log.info("Entering getImageAsString method");
		}
		// get images for HTML img tag as data image png file base64 encoded. Examples, "res/header.png", "res/footer.png"
//		return new String(Base64.encodeBase64(IOUtils.toByteArray(this.getClass().getClassLoader().getResource(fileName))));
		return new String(Base64.encodeBase64(IOUtils.toByteArray(url)), Charset.defaultCharset());

  }
  
  public static File getNewHtmlPageFile(String page, String dashboard) throws IOException {
		if (log.isLoggable(Level.INFO)) {
			log.info("Entering getNewHtmlPageFile method");
		}
		File file = File.createTempFile(new StringBuilder(dashboard).append(HTML_FILE_UPDATED).toString(), HTML_FILE_SUFFIX);
		file.deleteOnExit();
		if (log.isLoggable(Level.INFO)) {
			log.info("getNewHtmlPageFile method: canonical path to file is '" + file.getCanonicalPath() + "'");
//			log.info("getNewHtmlPageFile method: page is '" + page + "'");
		}
		FileUtils.writeStringToFile(file, page);
		return file;
  }
	
  
}