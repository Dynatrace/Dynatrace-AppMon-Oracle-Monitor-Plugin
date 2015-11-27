package com.dynatrace.diagnostics.plugins.utils;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface OraclePluginConstants {
	
	public static final String DEFAULT_ENCODING = "UTF-8"; // was System.getProperty("file.encoding","UTF-8");
	public static final String EMPTY_STRING = "";
	public static final String SGA_STATEMENT = "select FREE_BUFFER_WAIT, WRITE_COMPLETE_WAIT, BUFFER_BUSY_WAIT, DB_BLOCK_CHANGE, DB_BLOCK_GETS, CONSISTENT_GETS, PHYSICAL_READS, PHYSICAL_WRITES from v$buffer_pool_statistics";
	public static final String BUFFER_RATIO_BUFFER_CACHE_STATEMENT = "SELECT ROUND ( (congets.VALUE + dbgets.VALUE - physreads.VALUE)  * 100    / (congets.VALUE + dbgets.VALUE),  2   ) VALUE  FROM v$sysstat congets, v$sysstat dbgets, v$sysstat physreads  WHERE congets.NAME = 'consistent gets'  AND dbgets.NAME = 'db block gets'  AND physreads.NAME = 'physical reads'  ";
	public static final String BUFFER_RATIO_EXEC_NOPARSE_STATEMENT = "SELECT DECODE (SIGN (ROUND ( (ec.VALUE - pc.VALUE)  * 100  / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  ),  -1, 0,  ROUND ( (ec.VALUE - pc.VALUE)  * 100    / DECODE (ec.VALUE, 0, 1, ec.VALUE),  2  )  )  VALUE FROM v$sysstat ec, v$sysstat pc  WHERE ec.NAME = 'execute count'  AND pc.NAME IN ('parse count', 'parse count (total)')  ";
	public static final String BUFFER_RATIO_MEMORY_SORT_STATEMENT = "SELECT ROUND ( ms.VALUE  / DECODE ((ds.VALUE + ms.VALUE), 0, 1, (ds.VALUE + ms.VALUE))  * 100,    2  ) VALUE FROM v$sysstat ds, v$sysstat ms  WHERE ms.NAME = 'sorts (memory)' AND ds.NAME = 'sorts (disk)'  ";
	public static final String BUFFER_RATIO_SQLAREA_STATEMENT = "SELECT ROUND (gethitratio * 100, 2) VALUE FROM v$librarycache  WHERE namespace = 'SQL AREA'";
	public static final String LIBRARY_CACHE_HIT_RATIOS_STATEMENT = "SELECT NAMESPACE, ROUND(PINHITRATIO * 100.0, 2) PINHITRATIO, ROUND(GETHITRATIO * 100.0, 2) GETHITRATIO FROM V$LIBRARYCACHE WHERE NAMESPACE IN ('SQL AREA','TABLE/PROCEDURE', 'BODY', 'TRIGGER')";
	public static final String DICTIONARY_CACHE_HIT_RATIO_STATEMENT = "SELECT ROUND((SUM(GETS - GETMISSES - FIXED)) / SUM(GETS) * 100, 2) FROM V$ROWCACHE";
	public static final String LIBRARY_CACHE_GET_PIN_HIT_RATIO_STATEMENT = "select round(sum(gethits)/sum(gets)*100,2) GETHITRATIO, round(sum(pinhits)/sum(pins)*100,2) PINHITRATIO from v$librarycache";
//	public static final String LIBRARY_CACHE_PIN_HIT_RATIO_STATEMENT = "select round(sum(pinhits)/sum(pins)*100,2) from v$librarycache";
	public static final String SHARED_POOL_FREE_MEMORY_STATEMENT = "select round((sum(decode(name,'free memory',bytes,0))/sum(bytes))*100,2) from v$sgastat";
	public static final String SHARED_POOL_RELOADS_STATEMENT = "select round(sum(reloads)/sum(pins)*100,2) from v$librarycache where namespace in ('SQL AREA','TABLE/PROCEDURE','BODY','TRIGGER')";
	public static final String WAIT_LATCH_GETS_STATEMENT = "select round(((sum(gets) - sum(misses)) / sum(gets))*100,2)	from v$latch";
	public static final String IMMEDIATE_LATCH_GETS_STATEMENT = "select round(((sum(immediate_gets) - sum(immediate_misses)) / sum(immediate_gets))*100,2) from v$latch";
	public static final String REDO_SPACE_WAIT_RATIO_STATEMENT = "select round((req.value/wrt.value)*100,2) from v$sysstat req, v$sysstat wrt where req.name= 'redo log space requests' and wrt.name= 'redo writes'";
	public static final String REDO_ALLOCATION_LATCH_STATEMENT = "select round(greatest("
														+ "(sum(decode(ln.name,'redo allocation',misses,0))"
														+ "/greatest(sum(decode(ln.name,'redo allocation',gets,0)),1)),"
														+ "(sum(decode(ln.name,'redo allocation',immediate_misses,0))"
														+ "/greatest(sum(decode(ln.name,'redo allocation',immediate_gets,0))"
														+ "+sum(decode(ln.name,'redo allocation',immediate_misses,0)),1))"
														+ ")*100,2) "
														+ "from v$latch l,v$latchname ln "
														+ "where l.latch#=ln.latch#";
	public static final String REDO_COPY_LATCHES_STATEMENT = "select round(greatest("
														+ "(sum(decode(ln.name,'redo copy',misses,0))"
														+ "/greatest(sum(decode(ln.name,'redo copy',gets,0)),1)),"
														+ "(sum(decode(ln.name,'redo copy',immediate_misses,0))"
														+ "/greatest(sum(decode(ln.name,'redo copy',immediate_gets,0))"
														+ "+sum(decode(ln.name,'redo copy',immediate_misses,0)),1)) )*100,2) "
														+ "from v$latch l,v$latchname ln "
														+ "where l.latch#=ln.latch#";
	public static final String RECURSIVE_CALLS_RATIO_STATEMENT = "select round((rcv.value/(rcv.value+usr.value))*100,2) from v$sysstat rcv, v$sysstat usr where rcv.name='recursive calls' and usr.name='user calls'";
	public static final String SHORT_TABLE_SCANS_RATIO_STATEMENT = "select round((shrt.value/(shrt.value+lng.value))*100,2) from v$sysstat shrt, v$sysstat lng where shrt.name='table scans (short tables)' and lng.name='table scans (long tables)'";
	public static final String ROLLBACK_SEGMENT_CONTENTION_STATEMENT = "select round(sum(waits)/sum(gets)*100,2) from v$rollstat";
	public static final String CHAINED_FETCH_RATIO_STATEMENT = "select round((cont.value/(scn.value+rid.value))*100,2) from v$sysstat cont, v$sysstat scn, v$sysstat rid 	where cont.name= 'table fetch continued row' and scn.name= 'table scan rows gotten' and rid.name= 'table fetch by rowid'";
	public static final String FREE_LIST_CONTENTION_STATEMENT = "select round((sum(decode(w.class,'free list',count,0))/(sum(decode(name,'db block gets',value,0)) + sum(decode(name,'consistent gets',value,0))))*100,2) from v$waitstat w, v$sysstat";
	public static final String CPU_PARSE_OVERHEAD_STATEMENT = "select round((prs.value/(prs.value+exe.value))*100,2) from v$sysstat prs, v$sysstat exe where prs.name like 'parse count (hard)' and exe.name= 'execute count'";

	public static final String SESSION_STATEMENT = "select SESSIONS_MAX, SESSIONS_CURRENT, SESSIONS_HIGHWATER, USERS_MAX from v$license";
	public static final String INSERT_TOP_SQL = "INSERT INTO TOP_SQL (TIMESLICE, SQL_ID, SRC_DB_NAME_ID, EXECUTIONS, ELAPSED_TIME, CPU_TIME, DISK_READS, DIRECT_WRITES, BUFFER_GETS, ROWS_PROCESSED, PARSE_CALLS, FIRST_LOAD_TIME, LAST_LOAD_TIME, CHILD_NUMBER) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	public static final String INSERT_TOP_SQL_FULLTEXT_ORACLE = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, SYSDATE, SYSDATE)";
	public static final String INSERT_TOP_SQL_FULLTEXT_SQL_SERVER = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, GETDATE(), GETDATE())";
	public static final String INSERT_TOP_SQL_FULLTEXT_POSTGRESQL = "INSERT INTO TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID, SQL_FULLTEXT, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	public static final String INSERT_TOP_SQL_EXPLAIN_PLAN = "INSERT INTO TOP_SQL_EXPLAIN_PLAN (SQL_ID, TIMESLICE, CHILD_NUMBER, SRC_DB_NAME_ID, EXPLAIN_PLAN_ID) VALUES (?, ?, ?, ?, ?)";
	public static final String INSERT_SRC_DB_NAME_ORACLE = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, SYSDATE, SYSDATE)";
	public static final String INSERT_SRC_DB_NAME_SQL_SERVER = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, GETDATE(), GETDATE())";
	public static final String INSERT_SRC_DB_NAME_SQL_SERVER_2008 = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, GETDATE(), GETDATE())";
	public static final String INSERT_SRC_DB_NAME_POSTGRESQL = "INSERT INTO T_SRC_DB_NAME (SRC_DB_NAME_ID, SRC_DB_NAME, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	public static final String INSERT_T_EXPLAIN_PLAN_ORACLE = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, SYSDATE, SYSDATE)";
	public static final String INSERT_T_EXPLAIN_PLAN_SQL_SERVER = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
	public static final String INSERT_T_EXPLAIN_PLAN_SQL_SERVER_2008 = "INSERT INTO T_EXPLAIN_PLAN (PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, GETDATE(), GETDATE())";
	public static final String INSERT_T_EXPLAIN_PLAN_POSTGRESQL = "INSERT INTO T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID, PLAN_HASH_VALUE, SQL_ID, SRC_DB_NAME_ID, CHILD_NUMBER, SQL_EXPLAIN_PLAN, CREATE_DATE, ACCESS_DATE) VALUES (?, ?, ?, ?, ?, ?, LOCALTIMESTAMP, LOCALTIMESTAMP)";
	public static final String INSERT_LOCKS = "INSERT INTO T_LOCKS (TIMESLICE, SRC_DB_NAME_ID, SID_SERIAL, ORA_USER, OS_USER_NAME, OWNER, OBJECT_NAME, OBJECT_TYPE, LOCK_MODE, STATUS, LAST_DDL) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	public static final String INSERT_TABLESPACES = "INSERT INTO T_TABLESPACES (TIMESLICE, SRC_DB_NAME_ID, NAME, TOTAL, USED, FREE, PERCENT_USED, PERCENT_FREE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	public static final String INSERT_TOP_WAIT_EVENTS = "INSERT INTO T_TOP_WAIT_EVENTS (TIMESLICE, SRC_DB_NAME_ID, EVENT, TOTAL_WAITS, TOTAL_TIMEOUTS, TIME_WAITED, AVERAGE_WAIT) VALUES (?, ?, ?, ?, ?, ?, ?)";
	public static final String CHECK_SID = "select sql_id from top_sql_fulltext where sql_id = ? and src_db_name_id = ?";
//	public static final String EXISTS_EXPLAIN_PLAN = "select explain_plan_id from t_explain_plan where dbms_lob.compare(sql_explain_plan, to_clob(?)) = 0";
	public static final String GET_NEXT_EXPLAIN_PLAN_ID_ORACLE = "select explain_plan_seq.nextval from dual";
	public static final String GET_NEXT_EXPLAIN_PLAN_ID_SQL_SERVER = "select (next value for dbo.explain_plan_seq)";
	public static final String GET_NEXT_EXPLAIN_PLAN_ID_SQL_POSTGRESQL = "select nextval('explain_plan_seq')"; 
	public static final String GET_NEXT_SRC_DB_NAME_ID_ORACLE = "select src_db_name_seq.nextval from dual";
	public static final String GET_NEXT_SRC_DB_NAME_ID_SQL_SERVER = "select (next value for dbo.src_db_name_seq)";
	public static final String GET_NEXT_SRC_DB_NAME_ID_POSTGRESQL = "select nextval('src_db_name_seq')"; 
	public static final String SELECT_T_EXPLAIN_PLAN_SQL_SERVER_2008 = "SELECT EXPLAIN_PLAN_ID, PLAN_HASH_VALUE from dbo.T_EXPLAIN_PLAN WHERE SQL_ID = ? AND CHILD_NUMBER = ? AND SRC_DB_NAME_ID = ?";
	public static final String SELECT_SRC_DB_NAME_SQL_SERVER_2008 = "SELECT SRC_DB_NAME_ID FROM T_SRC_DB_NAME WHERE SRC_DB_NAME = ?";
	public static final String BUILD_EXPLAIN_PLAN_1 = "EXPLAIN PLAN SET STATEMENT_ID = '";
	public static final String BUILD_EXPLAIN_PLAN_2 = "' FOR ";
	public static final String BUILD_SELECT_EXPLAIN_PLAN_1 = "SELECT PLAN_TABLE_OUTPUT FROM TABLE (DBMS_XPLAN.DISPLAY('PLAN_TABLE', '";
	public static final String BUILD_SELECT_EXPLAIN_PLAN_2 =  "', 'TYPICAL'))";
	public static final String BUILD_DELETE_EXPLAIN_PLAN = "delete from plan_table where statement_id = ?";
	public static final String GET_EXPLAIN_PLAN_VSQL_PLAN = "SELECT * FROM table(DBMS_XPLAN.DISPLAY_CURSOR(?,?))";
	public static final String LINE_SEPARATOR = "\n";
	public static final String PLAN_HASH_VALUE_PATTERN = "Plan hash value:";
	public static final String CHECK_EXPLAIN_PLAN_ID_PLAN_HASH_VALUE = "select explain_plan_id from T_EXPLAIN_PLAN where plan_hash_value = ? and src_db_name_id = ?";
	public static final String CHECK_EXPLAIN_PLAN_ID_SID_CHILD_NUMBER = "select EXPLAIN_PLAN_ID from T_EXPLAIN_PLAN where sql_id = ? and child_number = ? and src_db_name_id = ?";
	public static final String UPDATE_EXPLAIN_PLAN_ACCESS_DATE = "update T_EXPLAIN_PLAN set access_date = sysdate where explain_plan_id = ? ";
	public static final String UPDATE_SRC_DB_NAME_ACCESS_DATE_ORACLE = "update T_SRC_DB_NAME set access_date = sysdate where SRC_DB_NAME_ID = ? ";
	public static final String UPDATE_SRC_DB_NAME_ACCESS_DATE_SQL_SERVER = "update T_SRC_DB_NAME set access_date = GETDATE() where SRC_DB_NAME_ID = ? ";
	public static final String UPDATE_SRC_DB_NAME_ACCESS_DATE_POSTGRESQL = "update T_SRC_DB_NAME set access_date = LOCALTIMESTAMP where SRC_DB_NAME_ID = ? ";
	public static final String GET_TIMESTAMP_ORACLE = "select sysdate from dual";
	public static final String GET_TIMESTAMP_SQL_SERVER = "select GETDATE()";
	public static final String GET_TIMESTAMP_POSTGRESQL = "select LOCALTIMESTAMP";
	public static final String UPDATE_TOP_SQL_FULLTEXT_ORACLE = "update top_sql_fulltext set access_date = sysdate where sql_id = ?";
	public static final String UPDATE_TOP_SQL_FULLTEXT_SQL_SERVER = "update top_sql_fulltext set access_date = GETDATE() where sql_id = ?";
	public static final String UPDATE_TOP_SQL_FULLTEXT_POSTGRESQL = "update top_sql_fulltext set access_date = LOCALTIMESTAMP where sql_id = ?";

	public static final String GET_SLOW_SQL_STATEMENT = "SELECT * FROM"
	    	+ " (SELECT sql_id, elapsed_time, cpu_time, sql_fulltext, child_number, disk_reads, direct_writes, executions," // ET: changed hash_value to sql_id
	    	+ " first_load_time, last_load_time, parse_calls, buffer_gets, rows_processed FROM v$sql) WHERE ROWNUM <= ?";
	
//	public static final String GET_LOCKS_SQL_STATEMENT = "SELECT l.session_id||','||v.serial# sid_serial,"
//		       + "   l.ORACLE_USERNAME ora_user,"
//		       + "   o.object_name," 
//		       + "   o.object_type," 
//		       + "   DECODE(l.locked_mode,"
//		       + "   0, 'None',"
//		       + "   1, 'Null',"
//		       + "   2, 'Row-S (SS)',"
//		       + "   3, 'Row-X (SX)',"
//		       + "   4, 'Share',"
//		       + "   5, 'S/Row-X (SSX)',"
//		       + "   6, 'Exclusive'," 
//		       + "   TO_CHAR(l.locked_mode)"
//		       + "   ) lock_mode,"
//		       + "   o.status," 
//		       + "   to_char(o.last_ddl_time,'mm.dd.yyyy hh24:mi:ss') last_ddl"
//		       + " FROM dba_objects o, gv$locked_object l, v$session v"
//		       + " WHERE o.object_id = l.object_id"
//		       + " and l.SESSION_ID=v.sid";
//		       + " order by 2,3";
	public static final String GET_LOCKS_SQL_STATEMENT = "SELECT a.session_id sid_serial,"
			+ "   a.oracle_username ora_user,"
			+ "   a.os_user_name,"
			+ "   b.owner,"
			+ "   b.object_name,"
			+ "   b.object_type,"
			+ "   a.locked_mode lock_mode,"
			+ "   b.status,"
			+ "   b.last_ddl_time last_ddl"
			+ " FROM v$locked_object a,"
			+ "  dba_objects b"
			+ " WHERE a.object_id = b.object_id";
	public static final String GET_TABLESPACE_STATS_SQL_STATEMENT = "SELECT"
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
			+ " WHERE t.tablespace=fs.tablespace";
//			+ " ORDER BY t.tablespace";
	public static final String GET_TOP_5_WAIT_EVENTS_SQL_STATEMENT = "select * from ("
			+ " select  event, total_waits, total_timeouts, time_waited, average_wait"
			+ " from v$system_event"
			+ " where event not like 'SQL*Net%'"
			+ " and event not in ('pmon timer','rdbms ipc message','dispatcher timer','smon timer'))"
//			+ " order by time_waited desc)"
			+ " where rownum < 6";
	public static final Map<String, Double> STATUS_MAP = Collections.unmodifiableMap(new HashMap<String, Double>() {
		private static final long serialVersionUID = -4867569384183231001L;
	{
		put("VALID", 1.);
		put("INVALID", 2.);
		put("N/A", 3.);
		}});
	
	// get DB Name SQL
	public static final String SELECT_DB_INSTANCE_NAME = "select sys_context('userenv','instance_name') from dual";
	
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
    public static final String XSL_TOP_WAIT_EVENTS_FILE = "res/OracleTopWaitEventsSS.xsl";
    public static final String JS_SORT_JAR_LOCATION = "res/sorttable.js";
    public static final String JS_SORT_NAME = "sorttable.js";
	// XML tags for the Slow SQL statements	
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
	public static final String OS_USER_NAME_OPEN_TAG = "<OS_USER_NAME>";
	public static final String OS_USER_NAME_CLOSE_TAG = "</OS_USER_NAME>";
	public static final String OWNER_OPEN_TAG = "<OWNER>";
	public static final String OWNER_CLOSE_TAG = "</OWNER>";
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
	
	// XML tags for the TABLESPACES SQL statement
	public static final String TOP_WAIT_EVENTS_OPEN_TAG = "<TOP_WAIT_EVENTS>";
	public static final String TOP_WAIT_EVENTS_CLOSE_TAG = "</TOP_WAIT_EVENTS>";
	public static final String TOP_WAIT_EVENT_OPEN_TAG = "<TOP_WAIT_EVENT>";
	public static final String TOP_WAIT_EVENT_CLOSE_TAG = "</TOP_WAIT_EVENT>";
	public static final String EVENT_OPEN_TAG = "<EVENT>";
	public static final String EVENT_CLOSE_TAG = "</EVENT>";
	public static final String TOTAL_WAITS_OPEN_TAG = "<TOTAL_WAITS>";
	public static final String TOTAL_WAITS_CLOSE_TAG = "</TOTAL_WAITS>";
	public static final String TOTAL_TIMEOUTS_OPEN_TAG = "<TOTAL_TIMEOUTS>";
	public static final String TOTAL_TIMEOUTS_CLOSE_TAG = "</TOTAL_TIMEOUTS>";
	public static final String TIME_WAITED_OPEN_TAG = "<TIME_WAITED>";
	public static final String TIME_WAITED_CLOSE_TAG = "</TIME_WAITED>";
	public static final String AVERAGE_WAIT_OPEN_TAG = "<AVERAGE_WAIT>";
	public static final String AVERAGE_WAIT_CLOSE_TAG = "</AVERAGE_WAIT>";

	public static final String CONFIG_IS_CLEANUP_TASK = "isCleanupTask";
	public static final String CONFIG_IS_ORACLE_NET_CONNECTION_DESCRIPTOR = "isOracleNetConnectionDescriptor";
	public static final String CONFIG_ORACLE_NET_CONNECTION_DESCRIPTOR = "oracleNetConnectionDescriptor";
    public static final String CONFIG_IS_SERVICE_NAME = "isServiceName";
    public static final String CONFIG_SERVICE_NAME = "serviceName";
	public static final String CONFIG_DB_HOST_NAME = "hostName";
    public static final String CONFIG_DB_NAME = "dbName";
    public static final String CONFIG_DB_USERNAME = "dbUsername";
    public static final String CONFIG_DB_PASSWORD = "dbPassword";
    public static final String CONFIG_PORT = "dbPort";
    public static final String CONFIG_IS_ENCRYPTION = "isEncryption";
    public static final String CONFIG_DB_ENCRYPTION = "dbEncryption";
    public static final String CONFIG_DB_ENCRYPTION_TYPES = "dbEncryptionTypes";
    public static final String CONFIG_DB_CHECKSUM = "dbChecksum";
    public static final String CONFIG_DB_CHECKSUM_TYPES = "dbChecksumTypes";
    public static final String CONFIG_IS_ENCRYPTION_HISTORY = "isEncryptionHistory";
    public static final String CONFIG_DB_ENCRYPTION_HISTORY = "dbEncryptionHistory";
    public static final String CONFIG_DB_ENCRYPTION_TYPES_HISTORY = "dbEncryptionTypesHistory";
    public static final String CONFIG_DB_CHECKSUM_HISTORY = "dbChecksumHistory";
    public static final String CONFIG_DB_CHECKSUM_TYPES_HISTORY = "dbChecksumTypesHistory";
    public static final String CONFIG_TOP_SQLS = "topSqls";
    public static final String CONFIG_IS_EXPLAIN_PLAN = "isExplainPlan";
    public static final String CONFIG_IS_DYNAMIC_MEASURES = "isDynamicMeasures";
    public static final String CONFIG_TYPE_OF_SLOWNESS = "typeOfSlowness";
    public static final String CONFIG_HTML_FILE_SQLS = "htmlFileSqls";
    public static final String CONFIG_HTML_FILE_LOCKS = "htmlFileLocks";
    public static final String CONFIG_HTML_FILE_TABLESPACES = "htmlFileTablespaces";
    public static final String CONFIG_HTML_FILE_TOP_WAIT_EVENTS = "htmlFileTopWaitEvents";
    public static final String CONFIG_MG_SUFFIX = "mgSuffix";
    // History tables
    public static final String CONFIG_IS_HISTORY_ON = "isHistoryOn";
    public static final String CONFIG_DB_TYPE_HISTORY = "dbTypeHistory";
    // Oracle tables
    public static final String CONFIG_IS_SERVICE_NAME_HISTORY = "isServiceNameHistory";
    public static final String CONFIG_SERVICE_NAME_HISTORY = "serviceNameHistory";
    public static final String CONFIG_DB_HOST_NAME_HISTORY = "hostNameHistory";
    public static final String CONFIG_DB_NAME_ORACLE_HISTORY = "dbNameOracleHistory";
    // MS SQLServer
    public static final String CONFIG_DB_NAME_SQLSERVER_HISTORY = "dbNameSQLServerHistory";
    // PostgreSql
    public static final String CONFIG_DB_NAME_POSTGRESQL_HISTORY = "dbNamePostgreSQLHistory";
    // variables for all DBs for history tables
    public static final String CONFIG_DB_USERNAME_HISTORY = "dbUsernameHistory";
    public static final String CONFIG_DB_PASSWORD_HISTORY = "dbPasswordHistory";
    public static final String CONFIG_PORT_HISTORY = "dbPortHistory";
    // purge timeframe
    public static final String CONFIG_PURGE_AFTER = "purgeAfter";
    public static final String CONFIG_IS_SQL_SERVER_2008 = "isSqlServer2008";
    
    // db names
    public static final String DB_NAME_ORACLE = "Oracle";
    public static final String DB_NAME_SQLSERVER = "SQLServer";
    public static final String DB_NAME_POSTGRESQL = "PostgreSQL";
    
    public static final String SGA_METRIC_GROUP = "Oracle SGA";
    public static final String SESSION_METRIC_GROUP = "Oracle Sessions";
    public static final String SYSTEM_METRIC_GROUP = "Oracle System";
    public static final String CACHE_HIT_RATIO_METRIC_GROUP = "Oracle Cache Hit Ratio";
    public static final String SQL_METRIC_GROUP = "Oracle SQL";
    public static final String SQL_SPLIT_NAME = "Sql Name";
    public static final String LOCKS_METRIC_GROUP = "Oracle Locks";
    public static final String LOCKS_SPLIT_NAME = "Lock Name";
    public static final String TABLESPACES_METRIC_GROUP = "Oracle Tablespaces";
    public static final String TABLESPACES_SPLIT_NAME = "Tablespace Name";
    public static final String TOP_WAIT_EVENTS_METRIC_GROUP = "Oracle Top 5 Wait Events";
    public static final String TOP_WAIT_EVENTS_SPLIT_NAME = "Top Wait Event Name";
    
    public static final String INSTANCE_UP = "Instance Up";
    public static final String HISTORY_INSTANCE_UP = "History Instance Up";
    public static final String SUCCESS = "Success";
    public static final String FREE_BUFFER_WAIT = "Free Buffer Waits";
    public static final String WRITE_COMPLETE_WAIT = "Write Complete Waits";
    public static final String BUFFER_BUSY_WAIT = "Buffer Busy Waits";
    public static final String DB_BLOCK_CHANGE = "DB Block Changes";
    public static final String DB_BLOCK_GETS = "DB Block Gets";
    public static final String CONSISTENT_GETS = "Consistent Gets";
    public static final String PHYSICAL_READS = "Physical Reads";
    public static final String PHYSICAL_WRITES = "Physical Writes";
    public static final String BUFFER_CACHE_HIT_RATIO = "Buffer Cache Hit Ratio";
    public static final String EXEC_NO_PARSE_RATIO = "Execution Without Parse Ratio";
    public static final String MEMORY_SORT_RATIO = "Memory Sort Ratio";
    public static final String SQL_AREA_GET_RATIO = "SQL Area Get Ratio";
    public static final String SQL_AREA_GET_HIT_RATIO = "SQL Area Get Hit Ratio";
    public static final String SQL_AREA_PIN_HIT_RATIO = "SQL Area Pin Hit Ratio";
    public static final String TABLE_PROCEDURE_GET_HIT_RATIO = "Table/Procedure Get Hit Ratio";
    public static final String TABLE_PROCEDURE_PIN_HIT_RATIO = "Table/Procedure Pin Hit Ratio";
    public static final String BODY_GET_HIT_RATIO = "Body Get Hit Ratio";
    public static final String BODY_PIN_HIT_RATIO = "Body Pin Hit Ratio";
    public static final String TRIGGER_GET_HIT_RATIO = "Trigger Get Hit Ratio";
    public static final String TRIGGER_PIN_HIT_RATIO = "Trigger Pin Hit Ratio";
    public static final String LIBRARY_CACHE_GET_HIT_RATIO = "Library Cache Get Hit Ratio";
    public static final String LIBRARY_CACHE_PIN_HIT_RATIO = "Library Cache Pin Hit Ratio";
    public static final String DICTIONARY_CACHE_HIT_RATIO = "Dictionary Cache Hit Ratio";
    public static final String ORACLE_SHARED_POOL_METRIC_GROUP = "Oracle Shared Pool";
    public static final String SHARED_POOL_FREE_MEMORY = "Shared Pool Free Memory";
    public static final String SHARED_POOL_RELOADS = "Shared Pool Reloads";
    public static final String ORACLE_LATCHES_METRIC_GROUP = "Oracle Latches";
    public static final String WAIT_LATCH_GETS = "Wait Latch Gets";
    public static final String IMMEDIATE_LATCH_GETS = "Immediate Latch Gets";
    public static final String ORACLE_REDO_METRIC_GROUP = "Oracle Redo";
    public static final String REDO_SPACE_WAIT_RATIO = "Redo Space Wait Ratio";
	public static final String REDO_ALLOCATION_LATCH = "Redo Allocation Latch";
	public static final String REDO_COPY_LATCHES = "Redo Copy Latches";
	public static final String ORACLE_MISCELLANEOUS_METRIC_GROUP = "Oracle Miscellaneous";
	public static final String ROLLBACK_SEGMENT_CONTENTION = "Rollback Segment Contention";
	public static final String RECURSIVE_CALLS_RATIO = "Recursive Calls Ratio";
	public static final String SHORT_TABLE_SCANS_RATIO = "Short Table Scans Ratio";
	public static final String CPU_PARSE_OVERHEAD = "CPU Parse Overhead";
	public static final String ORACLE_TABLE_CONTENTION_METRIC_GROUP = "Oracle Table Contention";
	public static final String CHAINED_FETCH_RATIO = "Chained Fetch Ratio";
	public static final String FREE_LIST_CONTENTION = "Free List Contention";

	
	
	public static final String[] ORACLE_SQL_METRICS = {"Executions", "Elapsed Time", "Average Elapsed Time", 
    	"CPU Time", "Average CPU Time", "Disk Reads", "Direct Writes", "Buffer Gets", "Rows Processed", "Parse Calls", 
    	"First Load Time", "Last Load Time", "Child Number"};
    public static final String[] ORACLE_LOCKS_METRICS = {"LockMode", "Status", "LastDdl"};
    public static final String[] ORACLE_TABLESPACES_METRICS = {"Total", "Used", "Free", "Percent Used", "Percent Free"};
    public static final String[] ORACLE_TOP_WAIT_EVENTS_METRICS = {"Total Waits", "Total Timeouts", "Time Waited", "Average Wait"};
    public static final String SQL_AREA = "SQL AREA";
    public static final String TABLE_PROCEDURE = "TABLE/PROCEDURE";
    public static final String BODY = "BODY";
    public static final String TRIGGER = "TRIGGER";
 
    public static final String CONNECTION_TIME = "Connection Time";
    
    public static final String SESSIONS_MAX = "Maximum Concurrent User Sessions";
    public static final String SESSIONS_CURRENT = "Current Concurrent User Sessions";
    public static final String SESSIONS_HIGHWATER = "Highest Concurrent User Sessions";
    public static final String USERS_MAX = "Maximum Named Users";
    public static final String URL_PREFIX_ORACLE = "jdbc:oracle:thin";
    public static final String URL_PREFIX_SQL_SERVER = "jdbc:sqlserver";
    public static final String URL_PREFIX_POSTGRESQL = "jdbc:postgresql";
    public static final String ORACLE_JDBC_DRIVER = "oracle.jdbc.driver.OracleDriver";
    public static final String SQL_SERVER_JDBC_DRIVER = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    public static final String POSTGRESQL_JDBC_DRIVER = "org.postgresql.Driver";
    
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
	public static final String DB_ENCRYPTION = "oracle.net.encryption_client";
	public static final String DB_ENCRYPTION_TYPES = "oracle.net.encryption_types_client";
	public static final String DB_CHECKSUM = "oracle.net.crypto_checksum_client";
	public static final String DB_CHECKSUM_TYPES = "oracle.net.crypto_checksum_types_client";
	
	// SQL statements for a cleanup task
	String[] deleteSqls = {
			"delete from TOP_SQL where timeslice < ?", 
			"delete from TOP_SQL_EXPLAIN_PLAN where timeslice < ?",
			"delete from T_LOCKS where timeslice < ?",
			"delete from T_TABLESPACES where timeslice < ?",
			"delete from T_TOP_WAIT_EVENTS where timeslice < ?",
			"delete from TOP_SQL_FULLTEXT where sql_id not in (select ts.sql_id from TOP_SQL ts, TOP_SQL_FULLTEXT tf where ts.sql_id = tf.sql_id and ts.SRC_DB_NAME_ID = tf.SRC_DB_NAME_ID)",
			"delete from T_SRC_DB_NAME where SRC_DB_NAME_ID not in (select ts.src_db_name_id from TOP_SQL ts, T_SRC_DB_NAME tn where ts.SRC_DB_NAME_ID = tn.SRC_DB_NAME_ID)",
			"delete from TOP_SQL_EXPLAIN_PLAN where sql_id not in (select ts.sql_id from TOP_SQL ts, TOP_SQL_EXPLAIN_PLAN te where ts.sql_id = te.sql_id and ts.SRC_DB_NAME_ID = te.SRC_DB_NAME_ID)",
			"delete from T_EXPLAIN_PLAN  where sql_id not in (select ts.sql_id from TOP_SQL ts, T_EXPLAIN_PLAN  tp where ts.sql_id = tp.sql_id and ts.SRC_DB_NAME_ID = tp.SRC_DB_NAME_ID)"
			};

}
