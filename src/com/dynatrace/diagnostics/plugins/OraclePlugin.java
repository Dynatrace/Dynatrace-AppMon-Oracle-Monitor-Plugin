package com.dynatrace.diagnostics.plugins;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import oracle.jdbc.pool.OracleDataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.dynatrace.diagnostics.pdk.MonitorEnvironment;
import com.dynatrace.diagnostics.pdk.MonitorMeasure;
import com.dynatrace.diagnostics.pdk.PluginEnvironment;
import com.dynatrace.diagnostics.pdk.Status;
import com.dynatrace.diagnostics.pdk.Status.StatusCode;
import com.dynatrace.diagnostics.pdk.TaskEnvironment;
import com.dynatrace.diagnostics.plugins.domain.CacheHitRatioStats;
import com.dynatrace.diagnostics.plugins.domain.LOCK_MODE;
import com.dynatrace.diagnostics.plugins.domain.LocksMetrics;
import com.dynatrace.diagnostics.plugins.domain.OBJECT_STATUS;
import com.dynatrace.diagnostics.plugins.domain.SqlMetrics;
import com.dynatrace.diagnostics.plugins.domain.TablespacesMetrics;
import com.dynatrace.diagnostics.plugins.domain.TopWaitEventsMetrics;
import com.dynatrace.diagnostics.plugins.utils.HelperUtils;
import com.dynatrace.diagnostics.plugins.utils.OraclePluginConstants;
import com.dynatrace.diagnostics.plugins.utils.PURGE_AFTER;

public class OraclePlugin implements OraclePluginConstants {
	
    private String xslSqls = EMPTY_STRING;
    private String xslLocks = EMPTY_STRING;
    private String xslTablespaces = EMPTY_STRING;
    private String xslTopWaitEvents = EMPTY_STRING;
    
    private java.sql.Connection con = null;
    private java.sql.Connection conHistory = null;
    
    private  String host = EMPTY_STRING;
    private  String dbName = EMPTY_STRING;
    private  String userName = EMPTY_STRING;
    private  String password = EMPTY_STRING;
    private  boolean isEncryption;
    private  String dbEncryption = EMPTY_STRING;
    private  String dbEncryptionTypes = EMPTY_STRING;
    private  String dbChecksum = EMPTY_STRING;
    private  String dbChecksumTypes = EMPTY_STRING;
    private Properties connectionProperties;
    private  boolean isEncryptionHistory;
    private  String dbEncryptionHistory = EMPTY_STRING;
    private  String dbEncryptionTypesHistory = EMPTY_STRING;
    private  String dbChecksumHistory = EMPTY_STRING;
    private  String dbChecksumTypesHistory = EMPTY_STRING;
    private Properties connectionPropertiesHistory;
    private  String port = EMPTY_STRING;
    private String dbTypeHistory = EMPTY_STRING;
    private  String hostHistory = EMPTY_STRING;
    private  String dbNameHistory = EMPTY_STRING;
    private  String userNameHistory = EMPTY_STRING;
    private  String passwordHistory = EMPTY_STRING;
    private  String portHistory = EMPTY_STRING;
    private long topSqls;
    private boolean isExplainPlan;
    private boolean isDynamicMeasures;
    private String typeOfSlowness = EMPTY_STRING;
    private String htmlFileSqls = EMPTY_STRING;
    private String htmlFileLocks = EMPTY_STRING;
    private String htmlFileTablespaces = EMPTY_STRING;
    private String htmlFileTopWaitEvents = EMPTY_STRING;
    private long srcDbNameId = -1;
    private boolean isServiceName;
//    private String serviceName;
    private boolean isServiceNameHistory;
    private String serviceNameHistory = EMPTY_STRING;
    private String historyUrl = EMPTY_STRING;
    private String oracleUrl = EMPTY_STRING;
    private String historyJdbcDriverClass = EMPTY_STRING;
    private boolean isHistoryOn;
    private boolean isCleanupTask;
    private boolean isOracleNetConnectionDescriptor;
    private String oracleNetConnectionDescriptor = EMPTY_STRING;
    private String purgeAfter = EMPTY_STRING;
    private PURGE_AFTER purge;
    private boolean isSqlServer2008;
    private double instanceUp = 0;
    private String mgSuffix = EMPTY_STRING;
    private static final Logger log = Logger.getLogger(OraclePlugin.class.getName());
   
    public static String getConnectionUrl(String dbType, boolean isNetDescriptor, String netDescriptor, String host, String port, String dbName, boolean isService) {
    	log.finer("Inside getConnectionUrl method ...");
    	String url = null;
    	if (dbType.equals(DB_NAME_ORACLE)) {
    		// Oracle DBMS
    		if (isNetDescriptor) {
    			url = URL_PREFIX_ORACLE + ":@" + netDescriptor;
    		} else {
	    		if (isService) {
	        		url = URL_PREFIX_ORACLE + ":@" + host + ":" + port + "/" + dbName;
	        	} else {
	        		url = URL_PREFIX_ORACLE + ":@" + host + ":" + port + ":" + dbName;
	        	}
    		}
    	} else if (dbType.equals(DB_NAME_SQLSERVER)) {
    		// MS SqlServer
    		url = URL_PREFIX_SQL_SERVER + "://" + host + ":" + port + ";DatabaseName=" + dbName + ";";
    	} else if (dbType.equals(DB_NAME_POSTGRESQL)) {
    		// postgreSql
    		url = URL_PREFIX_POSTGRESQL + "://" + host + ":" + port + "/" + dbName;
    	}
    	
    	log.finer("getConnectionUrl method: connection string is " + url);
    	return url;
    }
    
    boolean isHistoryOn() {
    	return isHistoryOn;
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
        log.finer("Inside of setup method ...");
        String value;
         
        isCleanupTask = env.getConfigBoolean(CONFIG_IS_CLEANUP_TASK);
        if (log.isLoggable(Level.FINER)) {
        	log.finer("setup method: " + CONFIG_IS_CLEANUP_TASK + " parameter set to " + isCleanupTask);
        }
		if (isCleanupTask) {
			if (env instanceof TaskEnvironment) {
				try {
					getHistoryTablesParms(env);
					return new Status(StatusCode.Success);
				} catch (Exception e) {
					return new Status(Status.StatusCode.ErrorInfrastructure, e.getMessage(), e.getMessage(), e);
				}
			} else {
				String msg = "setup method: Plugin is configured as a monitor but the isCleanupTask parameter is 'true'. Plugin cannot run cleanup job.";
				log.severe(msg);
				throw new RuntimeException(msg);
			}
		}
		mgSuffix = env.getConfigString(CONFIG_MG_SUFFIX);
        //get configuration for monitor plugin
		isOracleNetConnectionDescriptor = env.getConfigBoolean(CONFIG_IS_ORACLE_NET_CONNECTION_DESCRIPTOR);
		if (isOracleNetConnectionDescriptor) {
			oracleNetConnectionDescriptor = ((oracleNetConnectionDescriptor = env.getConfigString(CONFIG_ORACLE_NET_CONNECTION_DESCRIPTOR)) == null ? null : oracleNetConnectionDescriptor.trim());
			if (oracleNetConnectionDescriptor == null || oracleNetConnectionDescriptor.isEmpty()) {
				return new Status(Status.StatusCode.ErrorInfrastructure, "setup method: oracleNetConnectionDescriptor is null or empty", "setup method: oracleNetConnectionDescriptor is null or empty");
			}
		} else {
	        host = ((value = env.getConfigString(CONFIG_DB_HOST_NAME)) == null ? EMPTY_STRING : value.trim());
	        port = (env.getConfigString(CONFIG_PORT) == null) ? "1521" : env.getConfigString(CONFIG_PORT);
	        isServiceName = env.getConfigBoolean(CONFIG_IS_SERVICE_NAME);
	        if (!isServiceName) {
	        	dbName = ((value = env.getConfigString(CONFIG_DB_NAME)) == null ? EMPTY_STRING : value.trim());
	        } else {
	        	dbName = ((value = env.getConfigString(CONFIG_SERVICE_NAME)) == null ? EMPTY_STRING : value.trim());
	        }
		}
        userName = ((value = env.getConfigString(CONFIG_DB_USERNAME)) == null ? EMPTY_STRING : value.trim());
        password = ((value = env.getConfigPassword(CONFIG_DB_PASSWORD)) == null ? EMPTY_STRING : value.trim());
        
	    connectionProperties = new Properties();
	    connectionProperties.put("user", userName);
	    connectionProperties.put("password", password);
        // get encryption and integrity parameters
        isEncryption = env.getConfigBoolean(CONFIG_IS_ENCRYPTION);
        if (isEncryption) {
	        dbEncryption = ((value = env.getConfigString(CONFIG_DB_ENCRYPTION)) == null ? EMPTY_STRING : value.trim());
	        dbEncryptionTypes = ((value = env.getConfigString(CONFIG_DB_ENCRYPTION_TYPES)) == null ? EMPTY_STRING : value.trim());
	        dbChecksum = ((value = env.getConfigString(CONFIG_DB_CHECKSUM)) == null ? EMPTY_STRING : value.trim());
	        dbChecksumTypes = ((value = env.getConfigString(CONFIG_DB_CHECKSUM_TYPES)) == null ? EMPTY_STRING : value.trim());
	        connectionProperties.put(DB_ENCRYPTION, dbEncryption);
	        connectionProperties.put(DB_ENCRYPTION_TYPES, dbEncryptionTypes);
	        connectionProperties.put(DB_CHECKSUM, dbChecksum);
	        connectionProperties.put(DB_CHECKSUM_TYPES, dbChecksumTypes);
        }
        // get configuration for the history database
        try {
        	getHistoryTablesParms(env);
        } catch (Exception e) {
			return new Status(Status.StatusCode.ErrorInfrastructure, e.getMessage(), e.getMessage(), e);
		}
 	    topSqls = (env.getConfigLong(CONFIG_TOP_SQLS) == null) ? 10 : env.getConfigLong(CONFIG_TOP_SQLS);
	    isExplainPlan = (env.getConfigBoolean(CONFIG_IS_EXPLAIN_PLAN) == null) ? false : env.getConfigBoolean(CONFIG_IS_EXPLAIN_PLAN);
	    isDynamicMeasures = (env.getConfigBoolean(CONFIG_IS_DYNAMIC_MEASURES) == null) ? false : env.getConfigBoolean(CONFIG_IS_DYNAMIC_MEASURES);
	    typeOfSlowness = ((value = env.getConfigString(CONFIG_TYPE_OF_SLOWNESS)) == null ? EMPTY_STRING : value.trim());
	    String s;
	    htmlFileSqls =  (s = env.getConfigString(CONFIG_HTML_FILE_SQLS)) != null ? s.trim() : EMPTY_STRING;
	    String sortTable = null;
		try {
			if (htmlFileSqls != null && !htmlFileSqls.isEmpty()) {
				xslSqls = getFile(XSL_SQL_FILE);
				String jsFileName = new StringBuilder(
						FilenameUtils.getFullPath(htmlFileSqls)).append(
						JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName),
						sortTable, DEFAULT_ENCODING);
				sortTable = getFile(JS_SORT_JAR_LOCATION);
			} 
			htmlFileLocks = (s = env.getConfigString(CONFIG_HTML_FILE_LOCKS)) != null ? s.trim() : EMPTY_STRING;
			if (htmlFileLocks != null && !htmlFileLocks.isEmpty()) {
			    xslLocks = getFile(XSL_LOCKS_FILE);
				String jsFileName = new StringBuilder(FilenameUtils.getFullPath(htmlFileLocks)).append(JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName), sortTable, DEFAULT_ENCODING);
				if (sortTable == null) {
					sortTable = getFile(JS_SORT_JAR_LOCATION);
				}
			} 
			htmlFileTablespaces = (s = env.getConfigString(CONFIG_HTML_FILE_TABLESPACES)) != null ? s.trim() : EMPTY_STRING;
			if (htmlFileTablespaces != null && !htmlFileTablespaces.isEmpty()) {
			    xslTablespaces = getFile(XSL_TABLESPACES_FILE);
				String jsFileName = new StringBuilder(FilenameUtils.getFullPath(htmlFileTablespaces)).append(JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName), sortTable, DEFAULT_ENCODING);
				if (sortTable == null) {
					sortTable = getFile(JS_SORT_JAR_LOCATION);
				}
			}
			htmlFileTopWaitEvents = (s = env.getConfigString(CONFIG_HTML_FILE_TOP_WAIT_EVENTS)) != null ? s.trim() : EMPTY_STRING;
			if (htmlFileTopWaitEvents != null && !htmlFileTopWaitEvents.isEmpty()) {
			    xslTopWaitEvents = getFile(XSL_TOP_WAIT_EVENTS_FILE);
				String jsFileName = new StringBuilder(FilenameUtils.getFullPath(htmlFileTopWaitEvents)).append(JS_SORT_NAME).toString();
				FileUtils.writeStringToFile(new File(jsFileName), sortTable, DEFAULT_ENCODING);
				if (sortTable == null) {
					sortTable = getFile(JS_SORT_JAR_LOCATION);
				}
			}
		} catch (Exception e) {
			String msg = "setup method: exception occurred '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		
		if (log.isLoggable(Level.FINER)) {
        	log.finer("setup method: isServiceName is '" + isServiceName + "'");
     		log.finer("setup method: " + (isServiceName ? "serviceName is '" : "dbName is '") + dbName + "'");
        	log.finer("setup method: host is '" + host + "'");
           	log.finer("setup method: port is '" + port + "'");
        	log.finer("setup method: userName is '" + userName + "'");
         	log.finer("setup method: password is '" + password + "'");
         	if (isHistoryOn) {
	         	log.finer("setup method: dbTypeHistory is '" + dbTypeHistory + "'");
	          	if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
	         		log.finer("setup method: isServiceNameHistory is '" + isServiceNameHistory + "'");
	         		log.finer("setup method: " + (isServiceNameHistory ? "serviceNameHistory is '" : "dbNameHistory is '") + dbNameHistory + "'");
	         	} else {
	         		log.finer("setup method: dbNameHistory is '" + dbNameHistory + "'");
	         	}
	         	log.finer("setup method: hostHistory is '" + hostHistory + "'");
	           	log.finer("setup method: portHistory is '" + portHistory + "'");
	        	log.finer("setup method: userNameHistory is '" + userNameHistory + "'");
	         	log.finer("setup method: passwordHistory is '" + passwordHistory + "'");
         	}
			log.finer("setup method: xslSqls is '" + xslSqls + "'");
			log.finer("setup method: xslLocks is '" + xslLocks + "'");
			log.finer("setup method: xslLocks is '" + xslTablespaces + "'");
			log.finer("setup method: xslLocks is '" + xslTopWaitEvents + "'");
			log.finer("setup method: topSqls is '" + topSqls + "'");
			log.finer("setup method: typeOfSlowness is '" + typeOfSlowness + "'");
			log.finer("setup method: htmlFileSqls is '" + htmlFileSqls + "'");
			log.finer("setup method: htmlFileLocks is '" + htmlFileLocks + "'");
			log.finer("setup method: htmlFileTablespaces is '" + htmlFileTablespaces + "'");
			log.finer("setup method: htmlFileTopWaitEvents is '" + htmlFileTopWaitEvents + "'");
        }
		
        Status stat;
	    // get connection to the monitored database
	    try {
	        log.finer("setup method: Connecting to Oracle ...");
	       	oracleUrl = getConnectionUrl(DB_NAME_ORACLE, isOracleNetConnectionDescriptor, oracleNetConnectionDescriptor, host, port, dbName, isServiceName);
	        log.finer("setup method: Connection string is ... " + oracleUrl);
	        log.finer("setup method: Opening database connection ...");
	        con = getOracleConnection(oracleUrl, connectionProperties);
	        if (isOracleNetConnectionDescriptor) {
	        	dbName = getDbInstanceName(con);
	        }
	        if (dbName == null || dbName.isEmpty()) {
	        	String msg = "setup method: returned dbName from the getDbInstanceName method is null or empty string";
	        	log.log(Level.SEVERE, msg);
	        	return new Status(Status.StatusCode.ErrorTargetService, msg, msg);
	        }
	        stat = new Status();
	    } catch (SQLException e) {
	       	log.log(Level.SEVERE, e.getMessage(), e);
	        return getErrorStatus(e);
	    } finally {
	      	// do nothing here
	    }
	        
        return stat;
    }
    
    private String getDbInstanceName(Connection con) throws SQLException {
		String instance = null;
    	PreparedStatement st = null;
    	ResultSet rs = null;
    	try {
    		st = con.prepareStatement(SELECT_DB_INSTANCE_NAME);
    		rs = st.executeQuery();
			while (rs.next()) {
				instance = rs.getString(1).toUpperCase();
			}
    	} catch(SQLException e) {
    		String msg = "getDbInstanceName method: SQLException is '" + HelperUtils.getExceptionAsString(e) + "'";
    		log.severe(msg);
    		throw e;
    	} finally {
    		try { 
    			if (rs != null) rs.close(); 
    		} catch (SQLException e) {}
    		
    		try {
    			if (st != null) st.close();
    		}  catch (SQLException e) {}
    	}
    	
    	log.finer("getDbInstanceName method: instance is '" + (instance == null ? "null" : instance) + "'");
    	return instance;
    }
    
	private void getHistoryTablesParms(PluginEnvironment env) {
		String value;
		isHistoryOn = env.getConfigBoolean(CONFIG_IS_HISTORY_ON);
		if (isCleanupTask) {
			if (!isHistoryOn) {
				String msg = "setup method: 'isHistoryOn must be 'true' for the cleanup job.";
				log.severe(msg);
				throw new RuntimeException(msg);
			}
			// get purgeAfter parameter
			purgeAfter = ((value = env.getConfigString(CONFIG_PURGE_AFTER)) == null ? EMPTY_STRING : value.trim());
			try {
				purge = PURGE_AFTER.valueOfIgnoreCase(purgeAfter);
			} catch (IllegalArgumentException e) {
				String msg = "getHistoryTablesParms method: Incorrect value of the purgeAfter parameter '" + purgeAfter + "'";
				log.severe(msg);
				throw new RuntimeException(msg);
			}
		}
			
		if (!isHistoryOn) {
			return;
		}
		
		isSqlServer2008 = env.getConfigBoolean(CONFIG_IS_SQL_SERVER_2008);
		
		dbTypeHistory = ((value = env.getConfigString(CONFIG_DB_TYPE_HISTORY)) == null ? EMPTY_STRING : value.trim());
		if (dbTypeHistory != null
				&& !(dbTypeHistory = dbTypeHistory.trim()).isEmpty()) {
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				// Oracle
				isServiceNameHistory = env.getConfigBoolean(CONFIG_IS_SERVICE_NAME_HISTORY);
				if (!isServiceNameHistory) {
					dbNameHistory = ((value = env.getConfigString(CONFIG_DB_NAME_ORACLE_HISTORY)) == null ? EMPTY_STRING : value.trim());
				} else {
					serviceNameHistory = ((value = env.getConfigString(CONFIG_SERVICE_NAME_HISTORY)) == null ? EMPTY_STRING : value.trim());
					dbNameHistory = serviceNameHistory;
				}
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				// SqlServer
				dbNameHistory = ((value = env.getConfigString(CONFIG_DB_NAME_SQLSERVER_HISTORY)) == null ? EMPTY_STRING : value.trim());
				log.finer("setup method: dbNameHistory is '" + dbNameHistory
						+ "'");
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				// PostgreSql
				dbNameHistory = ((value = env.getConfigString(CONFIG_DB_NAME_POSTGRESQL_HISTORY)) == null ? EMPTY_STRING : value.trim());
			} else {
				String msg = "setup method: dbTypeHistory does not contain correct name of the DBMS. dbTypeHistory is '"
						+ dbTypeHistory + "'";
				log.severe(msg);
				throw new RuntimeException(msg);
			}
		} else {
			String msg = "setup method: dbTypeHistory variable is null or empty";
			log.severe(msg);
			throw new RuntimeException(msg);
		}

		hostHistory = ((value = env.getConfigString(CONFIG_DB_HOST_NAME_HISTORY)) == null ? EMPTY_STRING : value.trim());
		userNameHistory = ((value = env.getConfigString(CONFIG_DB_USERNAME_HISTORY)) == null ? EMPTY_STRING : value.trim());
		portHistory = (env.getConfigString(CONFIG_PORT_HISTORY) == null) ? "1521" : env.getConfigString(CONFIG_PORT_HISTORY).trim();
		passwordHistory = ((value = env.getConfigPassword(CONFIG_DB_PASSWORD_HISTORY)) == null ? EMPTY_STRING : value.trim());
		connectionPropertiesHistory = new Properties();
		connectionPropertiesHistory.put("user", userNameHistory);
		connectionPropertiesHistory.put("password", passwordHistory);
		isEncryptionHistory = env.getConfigBoolean(CONFIG_IS_ENCRYPTION_HISTORY);
		if (isEncryptionHistory) {
			dbEncryptionHistory = ((value = env.getConfigString(CONFIG_DB_ENCRYPTION_HISTORY)) == null ? EMPTY_STRING : value.trim());
			dbEncryptionTypesHistory = ((value = env.getConfigString(CONFIG_DB_ENCRYPTION_TYPES_HISTORY)) == null ? EMPTY_STRING : value.trim());
			dbChecksumHistory = ((value = env.getConfigString(CONFIG_DB_CHECKSUM_HISTORY)) == null ? EMPTY_STRING : value.trim());
			dbChecksumTypesHistory = ((value = env.getConfigString(CONFIG_DB_CHECKSUM_TYPES_HISTORY)) == null ? EMPTY_STRING : value.trim());
			connectionPropertiesHistory.put(DB_ENCRYPTION, dbEncryptionHistory);
			connectionPropertiesHistory.put(DB_ENCRYPTION_TYPES, dbEncryptionTypesHistory);
			connectionPropertiesHistory.put(DB_CHECKSUM, dbChecksumHistory);
			connectionPropertiesHistory.put(DB_CHECKSUM_TYPES, dbChecksumTypesHistory);
		}
		// set connection to history tables
		try {
			log.finer("setup method: Connecting to the History Database...");
			historyUrl = getConnectionUrl(dbTypeHistory, false, null, hostHistory,
					portHistory, dbNameHistory, isServiceNameHistory);
			log.finer("setup method: Connection string for History Database is ... "
					+ historyUrl);
			log.finer("setup method: Opening database connection for History Database...");
			historyJdbcDriverClass = getJdbcDriverClass(dbTypeHistory);
			Class.forName(historyJdbcDriverClass);
			conHistory = java.sql.DriverManager.getConnection(historyUrl,
					connectionPropertiesHistory);
		} catch (ClassNotFoundException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new RuntimeException(e);
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw new RuntimeException(e);
		} finally {
			// do nothing here
		}
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
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '"	+ dbTypeHistory + "'");
				throw new RuntimeException(msg);
			}

	    	try {
	    		if (!dbTypeHistory.equals(DB_NAME_SQLSERVER) || !isSqlServer2008) {
					id = getNextSeqId(con, nextQuery);
				}
			   	con.setAutoCommit(true);
				String insertQuery = null;
				if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
					insertQuery = INSERT_SRC_DB_NAME_ORACLE;
				} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
					if (isSqlServer2008) {
						insertQuery = INSERT_SRC_DB_NAME_SQL_SERVER_2008;
					} else {
						insertQuery = INSERT_SRC_DB_NAME_SQL_SERVER;
					}
				} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
					insertQuery = INSERT_SRC_DB_NAME_POSTGRESQL;
				} else {
					String msg;
					log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory + "'");
					throw new RuntimeException(msg);
				}
				ps = con.prepareStatement(insertQuery);
				if (!dbTypeHistory.equals(DB_NAME_SQLSERVER) || !isSqlServer2008) {
					ps.setLong(1, id);
					ps.setString(2,  dbName);
				} else {
					ps.setString(1,  dbName);
				}
				ps.executeUpdate();
				ps.clearParameters();
				if (dbTypeHistory.equals(DB_NAME_SQLSERVER) && isSqlServer2008) {
					id = getSrcDbNameIdByDbName(con, dbName);
				}
	    	} catch (Exception e) {
	    		log.severe("getSrcDbNameId method: '" + HelperUtils.getExceptionAsString(e) + "'");
	    		throw e;
	    	} finally {
	    		try {
	    			if (ps != null)
	    				ps.close();
	    		} catch (SQLException e) {
	    			// do nothing
	    		}
	    	}
    	}
    	
    	return id;
    	 
    }
    
    private long getSrcDbNameIdByDbName(Connection con, String dbName) throws SQLException {
    	long id = -1;
    	PreparedStatement ps = null;
    	ResultSet rs = null;
    	try {
    		ps = con.prepareStatement(SELECT_SRC_DB_NAME_SQL_SERVER_2008);
    		ps.setString(1,  dbName);
    		rs = ps.executeQuery();
    		while (rs.next()) {
    			id = rs.getLong(1);
    		}
    	} catch (SQLException e) {
    		String msg = "getSrcDbNameIdByDbName method: SQLException was thrown. Stacktrace is " + HelperUtils.getExceptionAsString(e);
    		log.severe(msg);
    		throw e;
    	} finally {
    		try {
    			if (rs != null) rs.close();
    		} catch (SQLException e) {}
    		try {
    			if (ps != null) ps.close();
    		} catch (SQLException e) {}
    	}
    	
    	return id;    	
    }
    
    private long checkSrcDbNameId(Connection con, String dbName) throws SQLException {
    	log.finer("Entering checkSrcDbNameId method...");
		long id = Long.MIN_VALUE;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// check if dbName is in the T_SRC_DB_NAME table already
			ps = con.prepareStatement("select src_db_name_id from t_src_db_name where upper(src_db_name) = ?");
			String s = dbName;
			ps.setString(1, s.toUpperCase());
			rs = ps.executeQuery();
			while (rs.next()) {
				id = rs.getLong(1);
			}
			if (id > 0) {
				log.finer("checkSrcDbNameId method: dbName '" + dbName + "' is found. srcDbNameId value is " + id);
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
					log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory + "'");
					throw new RuntimeException(msg);
				}	
				ps = con.prepareStatement(updateQuery);
				ps.setLong(1, id);
				int i = ps.executeUpdate();
				if (i != 1) {
					log.finer("checkExplainPlanId method: number of updated records should be 1, but " + i + " records were returned");
				}
			} else {
				log.finer("checkSrcDbNameId method: dbName '" + dbName + "' is not found.");
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
		PreparedStatement st = null;
		ResultSet rs = null;
		try {
			st = con.prepareStatement(sql);
			rs = st.executeQuery();

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
		log.finer("getNextSeqId method: returning id value " + id);
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
    
	public Status execute(PluginEnvironment env) throws Exception {
		log.finer("Inside of execute method ...");
		Status stat = new Status();

		if (env instanceof TaskEnvironment) {
			if (isCleanupTask && isHistoryOn) {
				// execute cleanup task
				// get purge after timestamp
				try {
					Date purgeDate = getPurgeAfterDate(purge);
					if (purgeDate != null) {
						runCleanupTask(purgeDate);
						return new Status(StatusCode.Success);
					} else {
						return new Status(StatusCode.PartialSuccess, "execute method: the cleanup task is setup with purgeAfter parameter set to '" + purge.name() + "' which require to skip cleanup.");
					}
				} catch (SQLException e) {
					String msg = "execute method: stack trace is " + HelperUtils.getExceptionAsString(e);
					log.severe(msg);
					// get new history connection
					try {
//						resetHistoryConnection();
						resetConnection(true); // reset connection for history database
					} catch (Exception e1) {
						msg = "execute method: stack trace is " + HelperUtils.getExceptionAsString(e1);
					}
					return new Status(StatusCode.ErrorInfrastructure, msg, msg);
				}
			} else {
				String msg = "execute method: the isCleanupTask and isHistoryOn parameter should be set to 'true' for the Oracle Monitor Plugin Task to perform cleanup.";
				log.warning(msg);
				return new Status(StatusCode.PartialSuccess, msg, msg);
			}
			
		}

		// Oracle RDBMS metrics
		try {
			populateSGAInfo((MonitorEnvironment) env);
			populateSystemInfo((MonitorEnvironment) env);
			populateSessionInfo((MonitorEnvironment) env);
			populateSpecializedHitRatios((MonitorEnvironment) env);
			populateGetPinHitRatios((MonitorEnvironment) env);
			populateDictionaryHitRatios((MonitorEnvironment) env);
			populateSharedPoolRatios((MonitorEnvironment) env);
			populateLatches((MonitorEnvironment) env);
			populateRedo((MonitorEnvironment) env);
			populateMiscellaneousMetrics((MonitorEnvironment) env);
			populateTableContentionMetrics((MonitorEnvironment) env);
			stat = new Status();
		} catch (SQLException e) {
			String msg = "execute method: '"
					+ HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			return new Status(StatusCode.ErrorInfrastructure, msg, msg);
		}
		// check if dbName exists in the history database
		if (isHistoryOn) {
	        srcDbNameId = getSrcDbNameId(conHistory, dbName);
	        if (srcDbNameId <= 0) {
	        	String msg = "execute method: srcDbNameId should be greater than 0 while it is equals " + srcDbNameId;
	        	log.severe(msg);
	        	return new Status(StatusCode.ErrorInfrastructure, msg, msg);
	        }
		}
		// Top SQL metrics
		if (log.isLoggable(Level.FINER)) {
			log.finer("execute method: htmlFileSqls is '" + htmlFileSqls + "' isHistoryOn is '" + isHistoryOn + "'");
		}
		Timestamp timestamp = null;
		if ((htmlFileSqls != null && !htmlFileSqls.isEmpty()) || isHistoryOn || isDynamicMeasures) {
			// generate slow SQL page
			try {
				if (isHistoryOn) {
					if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
						throw new Exception(
								"execute method: current timestamp should not be null");
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateSqlsInfo((MonitorEnvironment) env, con, conHistory,
						timestamp, srcDbNameId, isHistoryOn);
			} catch (Exception e) {
				String msg = "execute method: '"
						+ HelperUtils.getExceptionAsString(e) + "'";
				log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
			}
		}
		// Locks metrics
		if (log.isLoggable(Level.FINER)) {
			log.finer("execute method: htmlFileLocks is '" + htmlFileLocks + "' isHistoryOn is '" + isHistoryOn + "'");
		}
		if ((htmlFileLocks != null && !htmlFileLocks.isEmpty()) || isHistoryOn || isDynamicMeasures) {
			// generate Oracle locks page
			try {
				if (isHistoryOn) {
					if (timestamp == null) {
						if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
							throw new Exception(
									"execute method: current timestamp should not be null");
						}
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateLocksInfo((MonitorEnvironment) env, con, conHistory,
						timestamp, isHistoryOn);
			} catch (Exception e) {
				String msg = "execute method: '"
						+ HelperUtils.getExceptionAsString(e) + "'";
				log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
			}
		}
		// Tablespaces metrics
		if (log.isLoggable(Level.FINER)) {
			log.finer("execute method: htmlFileTablespaces is '" + htmlFileTablespaces + "' isHistoryOn is '" + isHistoryOn + "'");
		}
		if ((htmlFileTablespaces != null && !htmlFileTablespaces.isEmpty()) || isHistoryOn || isDynamicMeasures) {
			// generate Oracle tablespaces page
			try {
				if (isHistoryOn) {
					if (timestamp == null) {
						if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
							throw new Exception(
									"execute method: current timestamp should not be null");
						}
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateTablespacesInfo((MonitorEnvironment) env, con,
						conHistory, timestamp, isHistoryOn);
			} catch (Exception e) {
				String msg = "execute method: '"
						+ HelperUtils.getExceptionAsString(e) + "'";
				log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
			}
		}
		// Top Wait Events metrics
		if (log.isLoggable(Level.FINER)) {
			log.finer("execute method: htmlFileTopWaitEvents is '" + htmlFileTopWaitEvents + "' isHistoryOn is '" + isHistoryOn + "'");
		}
		if ((htmlFileTopWaitEvents != null && !htmlFileTopWaitEvents.isEmpty()) || isHistoryOn || isDynamicMeasures) {
			// generate Oracle Top Wait Events page
			try {
				if (isHistoryOn) {
					if (timestamp == null) {
						if ((timestamp = getCurrentTimestamp(conHistory)) == null) {
							throw new Exception(
									"execute method: current timestamp should not be null");
						}
					}
				} else {
					timestamp = new Timestamp(System.currentTimeMillis());
				}
				populateTopWaitEventsInfo((MonitorEnvironment) env, con,
						conHistory, timestamp, isHistoryOn);
			} catch (Exception e) {
				String msg = "execute method: '"
						+ HelperUtils.getExceptionAsString(e) + "'";
				log.severe(msg);
				return new Status(StatusCode.ErrorInfrastructure, msg, msg);
			}
		}

		return stat;
	}

	double getInstanceUp(MonitorEnvironment env, boolean isHistoryConnection) throws SQLException {
		log.finer("Inside populateInstanceUp method ...");
		
		ResultSet rs = null;
		PreparedStatement st = null;
		instanceUp = 0;
		try {
			if (isHistoryConnection) {
				if (conHistory == null) {
					return 0;
				}
				return conHistory.isValid(5) ? 1 : 0; // timeout is 5 seconds
			} else {
				if (con == null) {
					return 0;
				}
				st = con.prepareStatement("select 1 from dual");
				rs = st.executeQuery();
				
				while (rs.next()) {
					instanceUp = (rs.getInt(1) == 1 ? 1 : 0);
				}
			}
		} catch (SQLException e) {
			log.severe("populateInstanceUp method: " + HelperUtils.getExceptionAsString(e));
			throw e;
		} finally {
			try {
				if (rs != null) rs.close();
			} catch (SQLException e) {}

			try {
				if (st != null) st.close();
			} catch (SQLException e) {}
		}
		
		return instanceUp;
	}
    
	private void runCleanupTask(Date purgeDate) throws SQLException {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering runCleanupTask method: purge date is '" + purgeDate + "'");
		}

		boolean autoCommitOrig = conHistory.getAutoCommit();

		PreparedStatement ps = null;
		try {
			conHistory.setAutoCommit(false);
			for (String sql : deleteSqls) {
				if (log.isLoggable(Level.FINER)) {
					log.finer("runCleanupTask method: delete statement '" + sql + "'");
				}
				ps = conHistory.prepareStatement(sql);
				if (sql.indexOf("?") != -1) {
					ps.setDate(1, new java.sql.Date(purgeDate.getTime()));
				}
				int i = ps.executeUpdate();
				if (log.isLoggable(Level.FINER)) {
					log.finer("runCleanupTask method: delete statement '" + sql + "'purged " + i + " rows");
				}
				ps.clearParameters();
				ps.close();
			}
			conHistory.commit();
		} catch (SQLException e) {
			conHistory.rollback();
			throw e;
		} finally {
			try {
				if (ps != null) ps.close();
			} catch (SQLException e) {
				String msg = "runCleanupTask method: exception when close prepared statement " + HelperUtils.getExceptionAsString(e);
				log.warning(msg);
			}
			try {conHistory.setAutoCommit(autoCommitOrig);} catch (SQLException e) {}
		}
	}
	
	void resetConnection(boolean isHistoryConnection) {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering resetConnection method: isHistoryConnection is '" + isHistoryConnection + "'");
		}
		String dbType;
		boolean isDbConnectionDescriptor;
		String dbConnectorDescriptor;
		String dbHost;
		String dbPort;
		String databaseName;
		boolean isDbServiceName;
		String dbDriver;
		Connection dbCon;
		Properties dbProperties;
		
		if (isHistoryConnection) {
			if (!isHistoryOn) {
				return;
			}
			if (conHistory != null) {
				try {conHistory.close();} catch (SQLException e){}
			}
			conHistory = null;
			
			// getConnectionUrl(dbTypeHistory, false, null, hostHistory, portHistory, dbNameHistory, isServiceNameHistory);
			dbType = dbTypeHistory;
			isDbConnectionDescriptor = false;
			dbConnectorDescriptor = null;
			dbHost = hostHistory;
			dbPort = portHistory;
			databaseName = dbNameHistory;
			isDbServiceName = isServiceNameHistory;
			dbDriver = getJdbcDriverClass(dbTypeHistory);
			dbProperties = connectionPropertiesHistory;
			if (log.isLoggable(Level.FINER)) {
				log.finer("resetConnection method: dbType = '" + dbTypeHistory
						+ "', isDbConnectionDescriptor = '" + isDbConnectionDescriptor
						+ "', dbConnectorDescriptor = 'null"
						+ "', dbHost = '" + dbHost
						+ "', dbPort = '" + dbPort
						+ "', databaseName = '" + databaseName
						+ "', isDbServiceName = '" + isDbServiceName
						+ "', dbDriver = '" + dbDriver
						+ "', dbProperties = '" + dbProperties
						);
			}
		} else {
			// getConnectionUrl(DB_NAME_ORACLE, isOracleNetConnectionDescriptor, oracleNetConnectionDescriptor, host, port, dbName, isServiceName);
			if (con != null) {
				try {con.close();} catch (SQLException e){}
			}
			con = null;
			dbType = DB_NAME_ORACLE;
			isDbConnectionDescriptor = isOracleNetConnectionDescriptor;
			dbConnectorDescriptor = oracleNetConnectionDescriptor;
			dbHost = host;
			dbPort = port;
			databaseName = dbName;
			isDbServiceName = isServiceName;
			dbDriver = ORACLE_JDBC_DRIVER;
			dbProperties = connectionProperties;
			if (log.isLoggable(Level.FINER)) {
				log.finer("resetConnection method: dbType = '" + dbTypeHistory
						+ "', isDbConnectionDescriptor = '" + isDbConnectionDescriptor
						+ "', dbConnectorDescriptor = '" + dbConnectorDescriptor
						+ "', dbHost = '" + dbHost
						+ "', dbPort = '" + dbPort
						+ "', databaseName = '" + databaseName
						+ "', isDbServiceName = '" + isDbServiceName
						+ "', dbDriver = '" + dbDriver
						+ "', dbProperties = '" + dbProperties
						);
			}
		}
		try {
			String url = getConnectionUrl(dbType, isDbConnectionDescriptor, dbConnectorDescriptor, dbHost, dbPort, databaseName, isDbServiceName);
	        log.finer("resetConnection method: Connection string is ... " + url);
	        log.finer("resetConnection method: Opening database connection ...");
	        if (!dbType.equals(DB_NAME_ORACLE)) {
	        	Class.forName(dbDriver);
	        	dbCon = java.sql.DriverManager.getConnection(url, dbProperties);
	        } else {
	 	        dbCon = getOracleConnection(url, connectionProperties);
	        }
	        	
	        if (!isHistoryConnection) {
		        if (isOracleNetConnectionDescriptor) {
		        	dbName = getDbInstanceName(dbCon);
		        }
		        if (dbName == null || dbName.isEmpty()) {
		        	String msg = "resetConnection method: returned dbName from the getDbInstanceName method is null or empty string";
		        	log.log(Level.SEVERE, msg);
		        	throw new RuntimeException(msg);
		        }
	        }
		} catch (Exception e) {
	    	String msg = HelperUtils.getExceptionAsString(e);
	    	log.log(Level.SEVERE, HelperUtils.getExceptionAsString(e), e);
	    	throw new RuntimeException(msg);
	    } finally {
	    	// do nothing here
	    }
		
		if (isHistoryConnection) {
			conHistory = dbCon;
			if (log.isLoggable(Level.FINER)) {
				log.finer("resetConnection method: conHistory is class " + conHistory.getClass().getName());
			}
		} else {
			con = dbCon;
			if (log.isLoggable(Level.FINER)) {
				log.finer("resetConnection method: con is class " + con.getClass().getName());
			}
		}
	}
	
	private Connection getOracleConnection(String url, Properties conProperties) throws SQLException {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getOracleConnection method: url is '" + url + "', conProperties is '" + Arrays.toString(conProperties.entrySet().toArray()) + "'");
		}
		Connection dbCon = null;
		try {
			OracleDataSource ods =  new OracleDataSource();
	        ods.setConnectionProperties(conProperties);
	        ods.setURL(url);
	        Properties cacheProps = new Properties();
	        cacheProps.put( "InitialLimit", "2" );
	        cacheProps.put( "MinLimit", "2" );
	        cacheProps.put( "MaxLimit", "2" );
	        cacheProps.put( "MaxStatementsLimit", "50" );
	        ods.setConnectionCacheProperties(cacheProps);
	        dbCon = ods.getConnection();
	        if (log.isLoggable(Level.FINER)) {
				log.finer("getOracleConnection method: dbCon variable is class " + dbCon.getClass().getName());
			}
	        DatabaseMetaData md;
	        if (dbCon != null && (md = dbCon.getMetaData()) != null) {
				log.finer(new StringBuilder("getOracleConnection method: Database product name is '")
						.append(md.getDatabaseProductName())
						.append("', database product version is '")
						.append(md.getDatabaseProductVersion())
						.append("', major version number is '")
						.append(md.getDatabaseMajorVersion())
						.append("', minor version number is '")
						.append(md.getDatabaseMinorVersion())
						.append("'").toString());
	        }
		} catch (SQLException e) {
			String msg = "getOracleConnection method: " + HelperUtils.getExceptionAsString(e);
			log.severe(msg);
			throw e;
		}
	    
		return dbCon;
	}
	
    private Date getPurgeAfterDate(PURGE_AFTER purge) {
    	// TODO add timezone here
    	Calendar calendar = new GregorianCalendar();
		Date date = new Date();
		calendar.setTime(date);
		
    	switch(purge) {
    	case NEVER:
    		return null;
    	case ONE_WEEK:
    		calendar.add(Calendar.WEEK_OF_YEAR, -1);
    		break;
    	case TWO_WEEKS:
    		calendar.add(Calendar.WEEK_OF_YEAR, -2);
    		break;
    	case THREE_WEEKS:
    		calendar.add(Calendar.WEEK_OF_YEAR, -3);
    		break;
    	case FOUR_WEEKS:
    		calendar.add(Calendar.WEEK_OF_YEAR, -4);
    		break;
    	case ONE_MONTH:
    		calendar.add(Calendar.MONTH, -1);
    		break;
    	case TWO_MONTHS:
    		calendar.add(Calendar.MONTH, -2);
    		break;
    	case THREE_MONTHS:
    		calendar.add(Calendar.MONTH, -3);
    		break;
    	case SIX_MONTHS:
    		calendar.add(Calendar.MONTH, -6);
    		break;
    	case ONE_YEAR:
    		calendar.add(Calendar.YEAR, -1);
    		break;
    	case TWO_YEARS:
    		calendar.add(Calendar.YEAR, -2);
    		break;
    	case THREE_YEARS:
    		calendar.add(Calendar.YEAR, -3);
    		break;
    		default:
    			String msg = "getPurgeAfterDate method: purge enum '" + purge.name() + " is unknown";
    			log.severe(msg);
    			throw new RuntimeException(msg);
    	}
    	
    	return calendar.getTime();
    }
    
	private Timestamp getCurrentTimestamp(Connection con) throws Exception {
		Timestamp timestamp = null;
		ResultSet rs = null;
		PreparedStatement st = null;
		try {
			String sql = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				sql = GET_TIMESTAMP_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				sql = GET_TIMESTAMP_SQL_SERVER;
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				sql = GET_TIMESTAMP_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory + "'");
				throw new RuntimeException(msg);
			}	
			st = con.prepareStatement(sql);
			rs = st.executeQuery();
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
        PreparedStatement st = null;

        log.finer("Inside populateSGAInfo method ...");

        try {
        	st = con.prepareStatement(SGA_STATEMENT);
            sgaResult = st.executeQuery();
            while (sgaResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, FREE_BUFFER_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating FREE_BUFFER_WAIT ... ");
                        measure.setValue(sgaResult.getDouble("FREE_BUFFER_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, WRITE_COMPLETE_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating WRITE_COMPLETE_WAIT ...");
                        measure.setValue(sgaResult.getDouble("WRITE_COMPLETE_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_BUSY_WAIT)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating BUFFER_BUSY_WAIT ...");
                        measure.setValue(sgaResult.getDouble("BUFFER_BUSY_WAIT"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_CHANGE)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating DB_BLOCK_CHANGE ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_CHANGE"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, DB_BLOCK_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating DB_BLOCK_GETS ...");
                        measure.setValue(sgaResult.getDouble("DB_BLOCK_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, CONSISTENT_GETS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating CONSISTENT_GETS ...");
                        measure.setValue(sgaResult.getDouble("CONSISTENT_GETS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_READS)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating PHYSICAL_READS ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_READS"));
                    }
                }
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, PHYSICAL_WRITES)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating PHYSICAL_WRITES ...");
                        measure.setValue(sgaResult.getDouble("PHYSICAL_WRITES"));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
 				throw e;
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
        	st = con.prepareStatement(BUFFER_RATIO_BUFFER_CACHE_STATEMENT);
            ratioResult = st.executeQuery();
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, BUFFER_CACHE_HIT_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating BUFFER_CACHE_HIT_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        	throw e;
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
        	st = con.prepareStatement(BUFFER_RATIO_EXEC_NOPARSE_STATEMENT);
            ratioResult = st.executeQuery();
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, EXEC_NO_PARSE_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating EXEC_NO_PARSE_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        	throw e;
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
        	st = con.prepareStatement(BUFFER_RATIO_MEMORY_SORT_STATEMENT);
            ratioResult = st.executeQuery();
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, MEMORY_SORT_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating MEMORY_SORT_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        	throw e;
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
        	st = con.prepareStatement(BUFFER_RATIO_SQLAREA_STATEMENT);
            ratioResult = st.executeQuery();
            while (ratioResult.next()) {
                if ((measures = env.getMonitorMeasures(SGA_METRIC_GROUP, SQL_AREA_GET_RATIO)) != null) {
                    for (MonitorMeasure measure : measures) {
                        log.finer("populateSGAInfo method: Populating SQL_AREA_GET_RATIO ...");
                        measure.setValue(ratioResult.getDouble(1));
                    }
                }
            }
        } catch (SQLException e) {
        	log.log(Level.SEVERE, e.getMessage(), e);
        	throw e;
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

  public void populateSystemInfo(MonitorEnvironment env) throws SQLException {

        Collection<MonitorMeasure> measures = null;
        double timeBefore = 0;
        double timeAfter = 0;
        double totalConnectionTime = 0;
        Connection timerCon = null;

        log.finer("Inside populateSystemInfo method ...");
        
        try {
            log.finer("populateSystemInfo method: Connecting to Oracle ...");
            log.finer("populateSystemInfo method: Connection string is ... " + oracleUrl);
            log.finer("populateSystemInfo method: Opening database connection ...");
            try {
	            timeBefore = System.currentTimeMillis();
	            timerCon = java.sql.DriverManager.getConnection(oracleUrl, connectionProperties);
	            timeAfter = System.currentTimeMillis();
	            totalConnectionTime = timeAfter - timeBefore;
            } catch (Exception e) {
            	log.severe("populateSystemInfo method: " + HelperUtils.getExceptionAsString(e));
            	resetConnection(false); // reset connection for a monitored database
            	throw e;
            }
            if ((measures = env.getMonitorMeasures(SYSTEM_METRIC_GROUP, CONNECTION_TIME)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSystemInfo method: Populating CONNECTION_TIME ... ");
                    measure.setValue(totalConnectionTime);
                }
            }
        } catch (SQLException e) {
        	log.severe("populateSystemInfo method: " + HelperUtils.getExceptionAsString(e));
        	throw e;
        } finally {
        	log.finer("populateSystemInfo method: Closing database connection ...");
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
        PreparedStatement st = null;
        log.finer("Inside populateSessionInfo method ...");

        try {
        	st = con.prepareStatement(SESSION_STATEMENT);
            sessionResult = st.executeQuery();
            sessionResult.next();
            //while (sessionResult.next()) {
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_MAX ... ");
                    measure.setValue(sessionResult.getDouble("SESSIONS_MAX"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_CURRENT)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_CURRENT ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_CURRENT"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, SESSIONS_HIGHWATER)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating SESSIONS_HIGHWATER ...");
                    measure.setValue(sessionResult.getDouble("SESSIONS_HIGHWATER"));
                }
            }
            if ((measures = env.getMonitorMeasures(SESSION_METRIC_GROUP, USERS_MAX)) != null) {
                for (MonitorMeasure measure : measures) {
                    log.finer("populateSessionInfo method: Populating USERS_MAX ...");
                    measure.setValue(sessionResult.getDouble("USERS_MAX"));
                }
            }
        } catch (SQLException e) {
        	log.severe("populateSessionInfo method: " + HelperUtils.getExceptionAsString(e));
            throw e;
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

	public void populateSpecializedHitRatios(MonitorEnvironment env) throws SQLException {
		ResultSet rs = null;
		Collection<MonitorMeasure> measures;
		PreparedStatement st = null;
		log.finer("Inside populateSpecializedHitRatios method ...");

		try {
			st = con.prepareStatement(LIBRARY_CACHE_HIT_RATIOS_STATEMENT);
			rs = st.executeQuery();
			List<CacheHitRatioStats> cacheHitRatios = getCacheHitRatios(rs);
			rs.close();
			st.close();
			String metricNameGet;
			String metricNamePin;
			for (CacheHitRatioStats cacheHitRatio : cacheHitRatios) {
				if (cacheHitRatio.getNamespace().equalsIgnoreCase(SQL_AREA)) {
					metricNameGet = SQL_AREA_GET_HIT_RATIO;
					metricNamePin = SQL_AREA_PIN_HIT_RATIO;
				} else if (cacheHitRatio.getNamespace().equalsIgnoreCase(TABLE_PROCEDURE)) {
					metricNameGet = TABLE_PROCEDURE_GET_HIT_RATIO;
					metricNamePin = TABLE_PROCEDURE_PIN_HIT_RATIO;
				} else if (cacheHitRatio.getNamespace().equalsIgnoreCase(BODY)) {
					metricNameGet = BODY_GET_HIT_RATIO;
					metricNamePin = BODY_PIN_HIT_RATIO;
				} else if (cacheHitRatio.getNamespace().equalsIgnoreCase(TRIGGER)) {
					metricNameGet = TRIGGER_GET_HIT_RATIO;
					metricNamePin = TRIGGER_PIN_HIT_RATIO;
				} else {
					String msg = "populateSpecializedHitRatios method: namespace '" + cacheHitRatio.getNamespace() + "' is not recognized. Allowed namespaces are: 'SQL AREA', 'TABLE/PROCEDURE', 'BODY', and 'TRIGGER'";
					log.severe(msg);
					throw new RuntimeException(msg);
				}
				if ((measures = env.getMonitorMeasures(CACHE_HIT_RATIO_METRIC_GROUP, metricNameGet)) != null) {
					for (MonitorMeasure measure : measures) {
						log.finer("populateSpecializedHitRatios method: Populating " + metricNameGet + " ...");
						measure.setValue(cacheHitRatio.getGetHitRatio());
					}
				}
				if ((measures = env.getMonitorMeasures(CACHE_HIT_RATIO_METRIC_GROUP, metricNamePin)) != null) {
					for (MonitorMeasure measure : measures) {
						log.finer("populateSpecializedHitRatios method: Populating " + metricNamePin + " ...");
						measure.setValue(cacheHitRatio.getPinHitRatio());
					}
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
		}

	}
	
	public void populateGetPinHitRatios(MonitorEnvironment env) throws SQLException {
		ResultSet rs = null;
		Collection<MonitorMeasure> measures;
		PreparedStatement st = null;
		log.finer("Inside populateGetPinHitRatios method ...");

		try {
			st = con.prepareStatement(LIBRARY_CACHE_GET_PIN_HIT_RATIO_STATEMENT);
			rs = st.executeQuery();
			double[] array = getArrayRatios(rs);
			rs.close();
			st.close();
			if ((measures = env.getMonitorMeasures(CACHE_HIT_RATIO_METRIC_GROUP, LIBRARY_CACHE_GET_HIT_RATIO)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("populateGetPinHitRatios method: Populating " + LIBRARY_CACHE_GET_HIT_RATIO + " ...");
					measure.setValue(array[0]);
				}
			}
			if ((measures = env.getMonitorMeasures(CACHE_HIT_RATIO_METRIC_GROUP, LIBRARY_CACHE_PIN_HIT_RATIO)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("populateGetPinHitRatios method: Populating " + LIBRARY_CACHE_PIN_HIT_RATIO + " ...");
					measure.setValue(array[1]);
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
		}

	}
	
	public void populateDictionaryHitRatios(MonitorEnvironment env) throws SQLException {
		ResultSet rs = null;
		Collection<MonitorMeasure> measures;
		PreparedStatement st = null;
		log.finer("Inside populateDictionaryHitRatios method ...");

		try {
			st = con.prepareStatement(DICTIONARY_CACHE_HIT_RATIO_STATEMENT);
			rs = st.executeQuery();
			double d = getResultSetDoubleValue(rs);
			rs.close();
			st.close();
			if ((measures = env.getMonitorMeasures(CACHE_HIT_RATIO_METRIC_GROUP, DICTIONARY_CACHE_HIT_RATIO)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("populateDictionaryHitRatios method: Populating " + DICTIONARY_CACHE_HIT_RATIO + " ...");
					measure.setValue(d);
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
		}

	}
	
	public void populateSharedPoolRatios(MonitorEnvironment env) throws SQLException {
		populateMetric(env, SHARED_POOL_FREE_MEMORY_STATEMENT, ORACLE_SHARED_POOL_METRIC_GROUP, SHARED_POOL_FREE_MEMORY);
		populateMetric(env, SHARED_POOL_RELOADS_STATEMENT, ORACLE_SHARED_POOL_METRIC_GROUP, SHARED_POOL_RELOADS);
	}
	
	public void populateLatches(MonitorEnvironment env) throws SQLException {
		populateMetric(env, WAIT_LATCH_GETS_STATEMENT, ORACLE_LATCHES_METRIC_GROUP, WAIT_LATCH_GETS);
		populateMetric(env, IMMEDIATE_LATCH_GETS_STATEMENT, ORACLE_LATCHES_METRIC_GROUP, IMMEDIATE_LATCH_GETS);
	}
	
	public void populateRedo(MonitorEnvironment env) throws SQLException {
		populateMetric(env, REDO_SPACE_WAIT_RATIO_STATEMENT, ORACLE_REDO_METRIC_GROUP, REDO_SPACE_WAIT_RATIO);
		populateMetric(env, REDO_ALLOCATION_LATCH_STATEMENT, ORACLE_REDO_METRIC_GROUP, REDO_ALLOCATION_LATCH);
		populateMetric(env, REDO_COPY_LATCHES_STATEMENT, ORACLE_REDO_METRIC_GROUP, REDO_COPY_LATCHES);
	}
	
	public void populateMiscellaneousMetrics(MonitorEnvironment env) throws SQLException {
		populateMetric(env, RECURSIVE_CALLS_RATIO_STATEMENT, ORACLE_MISCELLANEOUS_METRIC_GROUP, RECURSIVE_CALLS_RATIO);
		populateMetric(env, SHORT_TABLE_SCANS_RATIO_STATEMENT, ORACLE_MISCELLANEOUS_METRIC_GROUP, SHORT_TABLE_SCANS_RATIO);
		populateMetric(env, ROLLBACK_SEGMENT_CONTENTION_STATEMENT, ORACLE_MISCELLANEOUS_METRIC_GROUP, ROLLBACK_SEGMENT_CONTENTION);
		populateMetric(env, CPU_PARSE_OVERHEAD_STATEMENT, ORACLE_MISCELLANEOUS_METRIC_GROUP, CPU_PARSE_OVERHEAD);
	}
	
	public void populateTableContentionMetrics(MonitorEnvironment env) throws SQLException {
		populateMetric(env, CHAINED_FETCH_RATIO_STATEMENT, ORACLE_TABLE_CONTENTION_METRIC_GROUP, CHAINED_FETCH_RATIO);
		populateMetric(env, FREE_LIST_CONTENTION_STATEMENT, ORACLE_TABLE_CONTENTION_METRIC_GROUP, FREE_LIST_CONTENTION);
	}
	
	public void populateMetric(MonitorEnvironment env, String sql, String group, String metric) throws SQLException {
		ResultSet rs = null;
		Collection<MonitorMeasure> measures;
		PreparedStatement st = null;
		log.finer("Inside populateMetric method ...");

		try {
			st = con.prepareStatement(sql);
			rs = st.executeQuery();
			double d = getResultSetDoubleValue(rs);
			rs.close();
			st.close();
			if ((measures = env.getMonitorMeasures(group, metric)) != null) {
				for (MonitorMeasure measure : measures) {
					log.finer("populateMetric method: Populating " + metric+ " ...");
					measure.setValue(d);
				}
			}
		} catch (SQLException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
			throw e;
		} finally {
			try {
				if (rs != null) {
					rs.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
			try {
				if (st != null) {
					st.close();
				}
			} catch (SQLException e) {
				// ignore exception
			}
		}

	}
  
  public void teardown(PluginEnvironment env) throws Exception {
        log.finer("teardown method: Exiting Oracle Monitor Plugin ... ");
        if (con != null) con.close();
        if (conHistory != null) conHistory.close();
  }
  
  private List<CacheHitRatioStats> getCacheHitRatios(ResultSet rs) throws SQLException {
	  if (rs == null) {
		  return null;
	  }
	  List<CacheHitRatioStats> list = new ArrayList<CacheHitRatioStats>();
	  while(rs.next()) {
		  CacheHitRatioStats hr = new CacheHitRatioStats();
		  hr.setNamespace(rs.getString("NAMESPACE"));
		  hr.setPinHitRatio(rs.getDouble("PINHITRATIO"));
		  hr.setGetHitRatio(rs.getDouble("GETHITRATIO"));
		  list.add(hr);
	  }
	  
	  return list;
  }
  
  private double[] getArrayRatios(ResultSet rs) throws SQLException {
	  if (rs == null) {
		  return null;
	  }
	  double[] array = new double[2];
	  while(rs.next()) {
		  array[0] = rs.getDouble("GETHITRATIO");
		  array[1] = rs.getDouble("PINHITRATIO");
	  }
	  
	  return array;
  }
  
  private double getResultSetDoubleValue(ResultSet rs) throws SQLException {
	  double d = Double.NaN;
	  if (rs == null) {
		  return d;
	  }
	  while(rs.next()) {
		  d = rs.getDouble(1);
	  }
	  
	  return d;
  }
    
  private void populateSqlsInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, long dbNameId, boolean isHistoryOn) throws Exception {

        ResultSet rs = null;
        PreparedStatement st = null;
        FileWriterWithEncoding fw = null;
//        List<SqlMetrics> sqls = new ArrayList<SqlMetrics>();
        if (log.isLoggable(Level.FINER)) {
        	log.finer("Inside of populateSqlsInfo method:  timestamp is '" + timestamp + "', dbNameId is " + dbNameId + ", isHistoryOn is '" + isHistoryOn + "'");
        }

        try {
        	//build SQL
        	log.finer("populateSqlsInfo method: sql is '" + GET_SLOW_SQL_STATEMENT + "'");
        	
        	st = con.prepareStatement(GET_SLOW_SQL_STATEMENT);
        	st.setLong(1, topSqls);
            rs = st.executeQuery();
            
            List<SqlMetrics> sqls = getTopSqls(dbNameId, rs);
            Collections.sort(sqls, new Comparator<SqlMetrics>() {
                public int compare(SqlMetrics o1, SqlMetrics o2) {
                    //Sorts by typeOfSlowness 
                	return compareSqlMetrics(o1, o2, typeOfSlowness);
                }
            });
            setSqlKey(sqls);
            if (log.isLoggable(Level.FINER)) {
            	log.finer("populateSqlsInfo method: sqls are '" + Arrays.toString(sqls.toArray()) + "'");
            }
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
            if (htmlFileSqls != null && !htmlFileSqls.isEmpty()) {
            	String sqlsHTML = buildSqlsXML(sqls);
				log.finer(sqlsHTML);
				
				fw = new FileWriterWithEncoding(htmlFileSqls, DEFAULT_ENCODING);
				
				fw.write(sqlsHTML, 0, sqlsHTML.length());
				fw.flush();
				fw.close();
            }
			
			// create dynamic measures for a list of sql statements' measures
			if (isDynamicMeasures) {
				log.finer("populateSqlsInfo method: sqls list is '" + Arrays.toString(sqls.toArray()));
				populateSqlMeasures(env, sqls);
			}
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
  	
  	private int compareSqlMetrics(SqlMetrics o1, SqlMetrics o2, String typeOfSlowness) {
  		if (typeOfSlowness.equals("elapsed_time")) {
  			return o1.getElapsedTime() < o2.getElapsedTime() ? -1 : o1.getElapsedTime() > o2.getElapsedTime() ? 1 : 0;
  		} else if (typeOfSlowness.equals("buffer_gets")) {
  			return o1.getBufferGets() < o2.getBufferGets() ? -1 : o1.getBufferGets() > o2.getBufferGets() ? 1 : 0;
  		} else if (typeOfSlowness.equals("cpu_time")) {
  			return o1.getCpuTime() < o2.getCpuTime() ? -1 : o1.getCpuTime() > o2.getCpuTime() ? 1 : 0;
		} else if (typeOfSlowness.equals("executions")) {
			return o1.getExecutions() < o2.getExecutions() ? -1 : o1.getExecutions() > o2.getExecutions() ? 1 : 0;		
		} else if (typeOfSlowness.equals("parse_calls")) {
			return o1.getParseCalls() < o2.getParseCalls() ? -1 : o1.getParseCalls() > o2.getParseCalls() ? 1 : 0;	
		} else if (typeOfSlowness.equals("disk_reads")) {
			return o1.getDiskReads() < o2.getDiskReads() ? -1 : o1.getDiskReads() > o2.getDiskReads() ? 1 : 0;	
		} else if (typeOfSlowness.equals("direct_writes")) {
			return o1.getDirectWrites() < o2.getDirectWrites() ? -1 : o1.getDirectWrites() > o2.getDirectWrites() ? 1 : 0;
		} else if (typeOfSlowness.equals("rows_processed")) {
			return o1.getRowsProcessed() < o2.getRowsProcessed() ? -1 : o1.getRowsProcessed() > o2.getRowsProcessed() ? 1 : 0;
		} else {
			String msg = "";
			log.severe(msg);
			throw new RuntimeException(msg);
		}
  	}

	private void setSqlKey(List<SqlMetrics> sqls) {
		for (SqlMetrics sql : sqls) {
			// setup key
			String sid = sql.getSid();
			String sqlFullText = sql.getSqlFulltext();
			if (sid != null && !sid.isEmpty() && sqlFullText != null
					&& !sqlFullText.isEmpty()) {
				int len;
				if (sqlFullText.length() > MAX_LENGTH_SQL_TEXT) {
					len = MAX_LENGTH_SQL_TEXT;
				} else {
					len = sqlFullText.length();
				}
				sql.setKey(new StringBuilder(sid).append("_")
						.append(sqlFullText.substring(0, len)).toString());
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
				  log.finer("getExplainPlansVSqlPlan method: exception is '" + HelperUtils.getExceptionAsString(e) + "'");
			  }
			  
			  if (sb == null) {
				  sb = new StringBuilder(NA).append(LINE_SEPARATOR); 
			  } else if (sb.toString().isEmpty()) {
				  sb.append(NA).append(LINE_SEPARATOR);
			  }
			  
			  sql.setExplainPlan(sb.toString());
		  }
	  } catch (Exception e) {
		  log.finer("getExplainPlansVSqlPlan method: exception is '" + HelperUtils.getExceptionAsString(e) + "'");
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
		log.finer("Entering checkExplainPlanId method...");
		long explainPlanId = Long.MIN_VALUE;
		PreparedStatement ps = null;
		ResultSet rs = null;
		Long planHashValue;
		try {
			if ((planHashValue = sql.getPlanHashValue()) != Long.MIN_VALUE) {
				// plan_hash_value is extracted
				ps = con.prepareStatement(CHECK_EXPLAIN_PLAN_ID_PLAN_HASH_VALUE);
				ps.setLong(1,  planHashValue);
				ps.setLong(2,  sql.getSrcDbNameId());
				rs = ps.executeQuery();
				while (rs.next()) {
					explainPlanId = rs.getLong(1);
				}
				log.finer("checkExplainPlanId method: planHashValue is '" + sql.getPlanHashValue() + ", explainPlanId is " + explainPlanId);
			} else {
				ps = con.prepareStatement(CHECK_EXPLAIN_PLAN_ID_SID_CHILD_NUMBER);
				ps.setString(1, sql.getSid());
				ps.setLong(2, sql.getChildNumber().longValue());
				ps.setLong(3, sql.getSrcDbNameId());
				rs = ps.executeQuery();
	
				while (rs.next()) {
					explainPlanId = rs.getLong(1);
				}
				
				log.finer("checkExplainPlanId method: sid is '" + sql.getSid() + "', childNumber is " + sql.getChildNumber() + ", explainPlanId is " + explainPlanId);
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
					log.finer("checkExplainPlanId method: number of updated records should be 1, but " + i + " records were returned");
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

	private void insertExplainPlan(Connection con, List<SqlMetrics> sqls,
			Timestamp timestamp) throws Exception {
		if (log.isLoggable(Level.FINER)) {
	      	log.finer("Inside of insertExplainPlan method: sqls list is '" + Arrays.toString(sqls.toArray()) + "', timestamp is " + timestamp);
	    }
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_TOP_SQL_EXPLAIN_PLAN);
			long id;
			String msg = null;
			for (SqlMetrics sql : sqls) {
				if (sql.getExplainPlan() == null
						|| sql.getExplainPlan().isEmpty()) {
					sql.setExplainPlan(NA);
				}
				// get PLAN_HASH_VALUE
				sql.setPlanHashValue(extractPlanHashValue(sql.getExplainPlan()));
				log.finer("insertExplainPlan method: planHashValue is "
						+ sql.getPlanHashValue());

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
						log.severe(msg = "insertExplainPlan method: incorrect dbTypeHistory '"
								+ dbTypeHistory + "'");
						try {
							// excessive try/catch block due to bug in Eclipse 4.4.x see https://bugs.eclipse.org/bugs/show_bug.cgi?id=405569 
							throw new RuntimeException(msg);
						} catch (Exception e) {
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
					if (!dbTypeHistory.equals(DB_NAME_SQLSERVER) || !isSqlServer2008) {
						id = getNextSeqId(con, nextQuery);
						sql.setExplainPlanId(id);
					}
					insertExplainPlanTable(con, sql);
					
				} else {
					sql.setExplainPlanId(id);
				}
				// insert into top_sql_explain_plan table reference on the
				// existing explain plan id
				ps.setString(1, sql.getSid());
				ps.setTimestamp(2, timestamp);
				ps.setLong(3, sql.getChildNumber().longValue());
				ps.setLong(4, sql.getSrcDbNameId());
				ps.setLong(5, sql.getExplainPlanId());
				ps.executeUpdate();
				ps.clearParameters();
			}
			
		} catch (SQLException e) {
			log.severe("insertExplainPlan method: "
					+ HelperUtils.getExceptionAsString(e));
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

	private Long extractPlanHashValue(String explainPlan) {
		String planHashValueString;
		long returnValue;
		int i;
		if ((i = explainPlan.indexOf(PLAN_HASH_VALUE_PATTERN)) >= 0) { 
			int j;
			if ((j = explainPlan.indexOf("\n", i + PLAN_HASH_VALUE_PATTERN.length() + 1)) > 0) {
				planHashValueString = explainPlan.substring(i + PLAN_HASH_VALUE_PATTERN.length() + 1, j - 1).trim();
				log.finer("extractPlanHashValue method: planHashValueString is '" + planHashValueString + "'");
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
		
		log.finer("extractPlanHashValue method: returning returnValue '" + returnValue + "'");
		
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
//		log.finer("getExplainPlanId method: returning explainPlanId value " + explainPlanId);
//		return explainPlanId;
//	}
  
	private void insertExplainPlanTable(Connection con, SqlMetrics sql) throws Exception {
		if (log.isLoggable(Level.FINER)) {
	      	log.finer("Inside of insertExplainPlanTable method: sql is '" + sql + "'");
	    }
	  
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			String insertQuery = null;
			if (dbTypeHistory.equals(DB_NAME_ORACLE)) {
				insertQuery = INSERT_T_EXPLAIN_PLAN_ORACLE;
			} else if (dbTypeHistory.equals(DB_NAME_SQLSERVER)) {
				if (isSqlServer2008) {
					insertQuery = INSERT_T_EXPLAIN_PLAN_SQL_SERVER_2008;
				} else {
					insertQuery = INSERT_T_EXPLAIN_PLAN_SQL_SERVER;
				}
			} else if (dbTypeHistory.equals(DB_NAME_POSTGRESQL)) {
				insertQuery = INSERT_T_EXPLAIN_PLAN_POSTGRESQL;
			} else {
				String msg;
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory + "'");
				throw new RuntimeException(msg);
			}
			ps = con.prepareStatement(insertQuery);
			if (!dbTypeHistory.equals(DB_NAME_SQLSERVER) || !isSqlServer2008) {
				ps.setLong(1, sql.getExplainPlanId());
				ps.setLong(2, sql.getPlanHashValue());
				ps.setString(3,  sql.getSid());
				ps.setLong(4,  sql.getSrcDbNameId());
				ps.setLong(5, sql.getChildNumber().longValue());
				ps.setString(6, sql.getExplainPlan());
			} else {
				ps.setLong(1, sql.getPlanHashValue());
				ps.setString(2,  sql.getSid());
				ps.setLong(3,  sql.getSrcDbNameId());
				ps.setLong(4, sql.getChildNumber().longValue());
				ps.setString(5, sql.getExplainPlan());
			}
			ps.executeUpdate();
			ps.clearParameters();
			if (ps != null) ps.close();
			if (isSqlServer2008) {
				//TODO get and set ExplainPlanId
				long id = getExplainPlanIdSqlServer2008(con, sql);
				sql.setExplainPlanId(id);
			}
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
	
	private long getExplainPlanIdSqlServer2008(Connection con, SqlMetrics sql) throws SQLException {
		if (log.isLoggable(Level.FINER)) {
	      	log.finer("Inside of getExplainPlanIdSqlServer2008 method: sql is '" + sql + "'");
	    }
		PreparedStatement ps = null;
		ResultSet rs = null;
		SqlMetrics sqlTemp = new SqlMetrics();
		sqlTemp.setExplainPlanId(-1L);
		try {
			ps = con.prepareStatement(SELECT_T_EXPLAIN_PLAN_SQL_SERVER_2008);
			ps.setString(1, sql.getSid());
			ps.setDouble(2, sql.getChildNumber());
			ps.setLong(3, sql.getSrcDbNameId());
			rs = ps.executeQuery();
			while (rs.next()) {
				sqlTemp.setExplainPlanId(rs.getLong(1));
				sqlTemp.setPlanHashValue(rs.getLong(2));
				sqlTemp.setSid(sql.getSid());
				sqlTemp.setSrcDbNameId(sql.getSrcDbNameId());
				sqlTemp.setChildNumber(sql.getChildNumber());
			}
			log.finer("getExplainPlanIdSqlServer2008 method: selected SqlMetrics object is " 
					+ " ExplainPlanId = '" + sqlTemp.getExplainPlanId() 
					+ "', PlanHashValue = '" + sqlTemp.getPlanHashValue()
					+ "', SID = '" + sqlTemp.getSid()
					+ "', SrcDbNameId = '" + sqlTemp.getSrcDbNameId()
					+ "', ChildNumber = '" + sqlTemp.getChildNumber()
					+ "', \r\n original SqlMetrics object is " 
					+ " ExplainPlanId = '" + sql.getExplainPlanId() 
					+ "', PlanHashValue = '" + sql.getPlanHashValue()
					+ "', SID = '" + sql.getSid()
					+ "', SrcDbNameId = '" + sql.getSrcDbNameId()
					+ "', ChildNumber = '" + sql.getChildNumber()
					+ ",");
			if (sqlTemp.getSid() == null || !sql.getSid().equals(sqlTemp.getSid())) {
				String msg = sqlTemp.getSid() == null || sqlTemp.getSid().trim().isEmpty() ? 
						"getExplainPlanIdSqlServer2008 method: ExplainPlanId is not found for the original Oracle SID '" + sql.getSid() + "'"
						: "getExplainPlanIdSqlServer2008 method: the original Oracle SID '" + sql.getSid() + "' is different from the found one '" + sqlTemp.getSid() + "'";
				log.severe(msg);
				throw new RuntimeException(msg);							
			}
		} catch (SQLException e) {
			String msg = "getExplainPlanIdSqlServer2008 method: exception was thrown. Stacktrace is '" + HelperUtils.getExceptionAsString(e) + "'";
			log.severe(msg);
			throw e;
		} finally {
			try {
				if (rs != null) rs.close();
			} catch(SQLException e) {}
			try {
				if (ps != null) ps.close();
			} catch(SQLException e) {}
		}
		
		return sqlTemp.getExplainPlanId();
		
	}
  
  private void insertTopSqlData(Connection con, List<SqlMetrics> sqls, Timestamp timestamp) throws Exception {
	  if (log.isLoggable(Level.FINER)) {
      	log.finer("Inside of insertTopSqlData method:  sqls list is '" + Arrays.toString(sqls.toArray()) + "', timestamp is " + timestamp);
      }
	  
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
		if (log.isLoggable(Level.FINER)) {
	      	log.finer("Inside of insertTopSqlFulltext method:  sqls list is '" + Arrays.toString(sqls.toArray()) + "'");
	    }

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
				log.severe(msg = "getSrcDbNameId method: incorrect dbTypeHistory '" + dbTypeHistory + "'");
				throw new RuntimeException(msg);
			}
			ps = con.prepareStatement(insertSql);
			for (SqlMetrics sql : sqls) {
				if (isSqlSidInserted(con, sql.getSid(), sql.getSrcDbNameId())) {
					PreparedStatement ps1 = null;
					try {
						ps1 = con.prepareStatement(updateSql);
						ps1.setString(1, sql.getSid());
						int i = ps1.executeUpdate();
						if (i != 1) {
							log.finer("insertTopSqlFulltext method: update statement should return 1 record but it returned " + i);
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
  
	private boolean isSqlSidInserted(Connection con, String sid, long srcDbNameId) {
		boolean result = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = con.prepareStatement(CHECK_SID);
			ps.setString(1, sid);
			ps.setLong(2, srcDbNameId);
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
	  	log.finer("Inside buildSqlsXML method ...");
	  	
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
			xmlSb.append(SQL_FULLTEXT_OPEN_TAG).append(StringEscapeUtils.escapeHtml4(sqlFullText))
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
    	}
    	
    	// add sqls tag
    	xmlSb.append(SQLS_CLOSE_TAG);
    	
    	log.finer("buildSqlsXml method: xml sqls file  is '" + xmlSb.toString());
    	
    	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslSqls.getBytes(DEFAULT_ENCODING));
  }

  private String convertXMLToHTML(byte[] xml, byte[] xsl) throws Exception {
	  	log.finer("Inside of convertXMLToHTML method...");
	  	
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
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getFile method: fileName is '" + fileName + "'");
		}
		URL url = this.getClass().getClassLoader().getResource(fileName);
		return IOUtils.toString(url, DEFAULT_ENCODING); // was System.getProperty("file.encoding")
  }
	
  private void populateLocksInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, boolean isHistoryOn) throws Exception {

        ResultSet rs = null;
        PreparedStatement st = null;
        FileWriterWithEncoding fw = null;
//        List<LocksMetrics> locks = new ArrayList<LocksMetrics>();
        if (log.isLoggable(Level.FINER)) {
        	log.finer("Inside of populateLocksInfo method:  timestamp is '" + timestamp + "', isHistoryOn is '" + isHistoryOn + "'");
        }

        try {
        	st = con.prepareStatement(GET_LOCKS_SQL_STATEMENT); 	
            rs = st.executeQuery();
            List<LocksMetrics> locksData = getLocks(rs);
            rs.close();
            st.close();
            Collections.sort(locksData, new Comparator<LocksMetrics>() {
                public int compare(LocksMetrics o1, LocksMetrics o2) {
                	int i;
                	if ((i = o1.getOraUser().compareTo(o2.getOraUser())) != 0) {
                		return i;
                	} else {
                		return o1.getObjectName().compareTo(o2.getObjectName());
                	}
                }
            });
            if (isHistoryOn) {
	            // insert locks data into T_LOCKS table
	            insertLocksData(conHistory, locksData, timestamp, srcDbNameId);
            }

			// build html page and write it someplace on the server
            if (htmlFileLocks != null && !htmlFileLocks.isEmpty()) {
            	String locksHTML = buildLocksXML(locksData);
            	log.finer("populateLocksInfo method: locksHTML is '" + locksHTML + "'");
            	fw = new FileWriterWithEncoding(htmlFileLocks, DEFAULT_ENCODING);
    			fw.write(locksHTML, 0, locksHTML.length());
    			fw.flush();
    			fw.close();
            }
            
			// create dynamic measures for a list of lock's measures
			if (isDynamicMeasures) {
				log.finer("populateLocksInfo method: locks list is '" + Arrays.toString(locksData.toArray()));
				populateLocksMeasures(env, locksData);
			}
			
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
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		List<LocksMetrics> list = new ArrayList<LocksMetrics>();
		Map<String, String> keys = new HashMap<String, String>();
		String s;
		while (rs.next()) {
			LocksMetrics lm = new LocksMetrics();
			lm.setSidSerial(rs.getString("SID_SERIAL"));
			lm.setOraUser(rs.getString("ORA_USER"));
			lm.setOsUserName(rs.getString("OS_USER_NAME"));
			lm.setOwner(rs.getString("OWNER"));
			lm.setObjectName(rs.getString("OBJECT_NAME"));
			lm.setObjectType(rs.getString("OBJECT_TYPE"));
			int i = rs.getInt("LOCK_MODE");
			lm.setLockMode((i < LOCK_MODE.values().length) ? LOCK_MODE.values()[i].lockMode() : "n/a");
			lm.setLockModeMeasure((i < LOCK_MODE.values().length) ? Double.valueOf(i) : Double.NaN);
			lm.setStatus(s = rs.getString("STATUS"));
			lm.setStatusMeasure(Double.valueOf(OBJECT_STATUS.valueOf(s.toUpperCase()).ordinal()));
			Timestamp t = rs.getTimestamp("LAST_DDL");
			lm.setLastDdl((t != null) ? sdf.format(new Date(t.getTime())) : "n/a");
			lm.setLastDdlMeasure(Double.valueOf(t.getTime()));
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
		if (log.isLoggable(Level.FINER)) {
	        log.finer("Inside of insertLocksData method:  locks is '" + Arrays.toString(locks.toArray()) + "', timestamp is '" + timestamp + "', dbNameId is " + dbNameId);
	    }
		  
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_LOCKS);
			  for (LocksMetrics lock : locks) {
				  ps.setTimestamp(1, timestamp);
				  ps.setLong(2, dbNameId);
				  ps.setString(3, lock.getSidSerial());
				  ps.setString(4, lock.getOraUser());
				  ps.setString(5, lock.getOsUserName());
				  ps.setString(6, lock.getOwner());
				  ps.setString(7, lock.getObjectName());
				  ps.setString(8,  lock.getObjectType());
				  ps.setString(9, lock.getLockMode());
				  ps.setString(10, lock.getStatus());
				  ps.setString(11, lock.getLastDdl());
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
    	
	  	log.finer("Inside buildLocksXML method...");
	  	
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
	     		
	     	// os user name
	     		xmlSb.append(OS_USER_NAME_OPEN_TAG).append(lock.getOsUserName())
	     			.append(OS_USER_NAME_CLOSE_TAG);
	     		
	     	// owner
	     		xmlSb.append(OWNER_OPEN_TAG).append(lock.getOwner())
	     			.append(OWNER_CLOSE_TAG);
	     		
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
    	
    	log.finer("xml locks file  is '" + xmlSb.toString());
    	
    	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslLocks.getBytes(DEFAULT_ENCODING));
    	
    	
  }
  
  private void populateTablespacesInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, boolean isHistoryOn) throws Exception {

      ResultSet rs = null;
      PreparedStatement st = null;
      FileWriterWithEncoding fw = null;
      if (log.isLoggable(Level.FINER)) {
      	log.finer("Inside of populateTablespacesInfo method:  timestamp is '" + timestamp + "', isHistoryOn is '" + isHistoryOn + "'");
      }

      try {
      	st = con.prepareStatement(GET_TABLESPACE_STATS_SQL_STATEMENT);
        rs = st.executeQuery();
        List<TablespacesMetrics> tablespacesData = getTablespaces(rs);
        rs.close();
        st.close();
        Collections.sort(tablespacesData, new Comparator<TablespacesMetrics>() {
            public int compare(TablespacesMetrics o1, TablespacesMetrics o2) {
            	return o1.getName().compareTo(o2.getName());
            }
        });
        if (isHistoryOn) {
	        // insert tablespace data into T_TABLESPACES table
	        insertTablespacesData(conHistory, tablespacesData, timestamp, srcDbNameId);
        }
        if (htmlFileTablespaces != null && !htmlFileTablespaces.isEmpty()) {
	        // build html page and write it someplace on the web server
	        String tablespacesHTML = buildTablespacesXML(tablespacesData);
				
			log.finer("populateTablespacesInfo method: tablespacesHTML is '" + tablespacesHTML + "'");
			fw = new FileWriterWithEncoding(htmlFileTablespaces, DEFAULT_ENCODING);
			fw.write(tablespacesHTML, 0, tablespacesHTML.length());
			fw.flush();
			fw.close();
        }
			
		// create dynamic measures for a list of lock's measures
        if (isDynamicMeasures) {
        	log.finer("populateTablespacesInfo method: tablespaces list is '" + Arrays.toString(tablespacesData.toArray()));
        	populateTablespacesMeasures(env, tablespacesData); 
        }
			
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

  private void populateTopWaitEventsInfo(MonitorEnvironment env, Connection con, Connection conHistory, Timestamp timestamp, boolean isHistoryOn) throws Exception {

      ResultSet rs = null;
      PreparedStatement st = null;
      FileWriterWithEncoding fw = null;
      if (log.isLoggable(Level.FINER)) {
      	log.finer("Inside of populateTopWaitEventsInfo method:  timestamp is '" + timestamp + "', isHistoryOn is '" + isHistoryOn + "'");
      }

      try {
      	st = con.prepareStatement(GET_TOP_5_WAIT_EVENTS_SQL_STATEMENT);
        rs = st.executeQuery();
        List<TopWaitEventsMetrics> topWaitEventsData = getTopWaitEvents(rs);
        rs.close();
        st.close();
        Collections.sort(topWaitEventsData, new Comparator<TopWaitEventsMetrics>() {
            public int compare(TopWaitEventsMetrics o1, TopWaitEventsMetrics o2) {
            	return o1.getEvent().compareTo(o2.getEvent());
            }
        });
        if (isHistoryOn) {
	        // insert top wait events data into T_TOP_WAIT_EVENTS table
        	insertTopWaitEventsData(conHistory, topWaitEventsData, timestamp, srcDbNameId);
        }
        if (htmlFileTopWaitEvents != null && !htmlFileTopWaitEvents.isEmpty()) {
	        // build html page and write it someplace on the web server
	        String topWaitEventsHTML = buildTopWaitEventsXML(topWaitEventsData);
				
			log.finer("populateTopWaitEventsInfo method: topWaitEventsHTML is '" + topWaitEventsHTML + "'");
			fw = new FileWriterWithEncoding(htmlFileTopWaitEvents, DEFAULT_ENCODING);
			fw.write(topWaitEventsHTML, 0, topWaitEventsHTML.length());
			fw.flush();
			fw.close();
        }
			
		// create dynamic measures for a list of lock's measures
        if (isDynamicMeasures) {
        	log.finer("populateTopWaitEventsInfo method: top wait events list is '" + Arrays.toString(topWaitEventsData.toArray()));
        	populateTopWaitEventsMeasures(env, topWaitEventsData); 
        }
			
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
  
  private List<TopWaitEventsMetrics> getTopWaitEvents(ResultSet rs) throws SQLException {
		List<TopWaitEventsMetrics> list = new ArrayList<TopWaitEventsMetrics>();
		Map<String, String> keys = new HashMap<String, String>();
		while (rs.next()) {
			TopWaitEventsMetrics tm = new TopWaitEventsMetrics(); //event, total_waits, total_timeouts, time_waited, average_wait
			tm.setEvent(rs.getString("EVENT"));
			tm.setTotalWaits(rs.getDouble("TOTAL_WAITS"));
			tm.setTotalTimeouts(rs.getDouble("TOTAL_TIMEOUTS"));
			tm.setTimeWaited(rs.getDouble("TIME_WAITED") * 100.0); // internally Oracle keeps this data in hundredth's of a second
			tm.setAverageWait(rs.getDouble("AVERAGE_WAIT") * 100.0); // internally Oracle keeps this data in hundredth's of a second
			// calculate key
			String key = tm.getEvent();
			// check if key is unique
			key = HelperUtils.getUniqueKey(keys, key);
			keys.put(key, "");
			tm.setKey(key);

			list.add(tm);
		}

		return list;
}
  
	private void insertTablespacesData(Connection con, List<TablespacesMetrics> tablespaces, Timestamp timestamp, long dbNameId) throws Exception {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Inside of insertTablespacesData method:  tablespaces is '" + Arrays.toString(tablespaces.toArray()) + "', timestamp is '" + timestamp + "', dbNameId is " + dbNameId);
		}
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
			log.severe("insertTablespacesData method: "
					+ HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertTablespacesData method: "
					+ HelperUtils.getExceptionAsString(e));
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
  
	private void insertTopWaitEventsData(Connection con, List<TopWaitEventsMetrics> topWaitEvents, Timestamp timestamp, long dbNameId) throws Exception {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Inside of insertTopWaitEventsData method:  topWaitEvents is '" + Arrays.toString(topWaitEvents.toArray()) + "', timestamp is '" + timestamp + "', dbNameId is " + dbNameId);
		}
		PreparedStatement ps = null;
		try {
			con.setAutoCommit(true);
			ps = con.prepareStatement(INSERT_TOP_WAIT_EVENTS);
			for (TopWaitEventsMetrics topWaitEvent : topWaitEvents) {
				ps.setTimestamp(1, timestamp);
				ps.setLong(2, dbNameId);
				ps.setString(3, topWaitEvent.getEvent());
				ps.setDouble(4, topWaitEvent.getTotalWaits());
				ps.setDouble(5, topWaitEvent.getTotalTimeouts());
				ps.setDouble(6, topWaitEvent.getTimeWaited());
				ps.setDouble(7, topWaitEvent.getAverageWait());
				ps.executeUpdate();
				ps.clearParameters();
			}
		} catch (SQLException e) {
			log.severe("insertTopWaitEventsData method: "
					+ HelperUtils.getExceptionAsString(e));
			throw e;
		} catch (Exception e) {
			log.severe("insertTopWaitEventsData method: " 
					+ HelperUtils.getExceptionAsString(e));
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
  	
  	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslTablespaces.getBytes(DEFAULT_ENCODING));
  	
  	
  }
  
  private String buildTopWaitEventsXML(List<TopWaitEventsMetrics> topWaitEvents) throws Exception{
	  	
		log.finer("Inside buildTopWaitEventsXML method...");
		  	
	  	SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss");
	  	
	  	StringBuilder xmlSb = new StringBuilder(XML_INITIAL_LENGTH);
	  	xmlSb.append(XML_LINE_1).append(TOP_WAIT_EVENTS_OPEN_TAG);
	  	
	  	// Add date and timestamp
	  	xmlSb.append(HD_OPEN_TAG).append(DATE_OPEN_TAG)
	  		.append(dateFormat.format(new Date())).append(DATE_CLOSE_TAG)
	  		.append(HD_CLOSE_TAG);
	  	
	  	// process rows from the result set
	  	
	  	for (TopWaitEventsMetrics topWaitEvent : topWaitEvents){
	  		xmlSb.append(TOP_WAIT_EVENT_OPEN_TAG);
	  		
	  		// event
	   		xmlSb.append(EVENT_OPEN_TAG).append(topWaitEvent.getEvent())
	   			.append(EVENT_CLOSE_TAG);

		    // total waits
		    xmlSb.append(TOTAL_WAITS_OPEN_TAG).append(FORMATTER_LONG.format(topWaitEvent.getTotalWaits()))
		     	.append(TOTAL_WAITS_CLOSE_TAG);
		     		
		    // total timeouts
		    xmlSb.append(TOTAL_TIMEOUTS_OPEN_TAG).append(FORMATTER_LONG.format(topWaitEvent.getTotalTimeouts())) 
		    	.append(TOTAL_TIMEOUTS_CLOSE_TAG);
		     		
		    // time waited
		    xmlSb.append(TIME_WAITED_OPEN_TAG).append(FORMATTER_SIZE.format(topWaitEvent.getTimeWaited())) 
		    	.append(TIME_WAITED_CLOSE_TAG);
		     	
		    // average wait
		    xmlSb.append(AVERAGE_WAIT_OPEN_TAG).append(FORMATTER_SIZE.format(topWaitEvent.getAverageWait()))
		     	.append(AVERAGE_WAIT_CLOSE_TAG);
		     		
			// add lock close tag
			xmlSb.append(TOP_WAIT_EVENT_CLOSE_TAG);
	  	}
	  	
	  	// add tablespaces tag
	  	xmlSb.append(TOP_WAIT_EVENTS_CLOSE_TAG);
	  	
	  	log.finer("xml topWaitEvents file  is '" + xmlSb.toString());
	  	
	  	return convertXMLToHTML(xmlSb.toString().getBytes(DEFAULT_ENCODING), xslTopWaitEvents.getBytes(DEFAULT_ENCODING));
	  }
  
  private void populateTablespacesMeasures(MonitorEnvironment env, List<TablespacesMetrics> tablespaces) {
	  log.finer("Inside of populateTablespacesMeasures method...");
	  String metricGroupName = dbName + mgSuffix;
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
						// set non-dynamic measure if exists
						setNonDynamicMeasure(env, metricGroupName, tablespace.getName() + "_TOTAL", tablespace.getTotal());
						break;
					case 1:
						// Used
						dynamicMeasure.setValue(tablespace.getUsed());
						// set non-dynamic measure if exists
						setNonDynamicMeasure(env, metricGroupName, tablespace.getName() + "_USED", tablespace.getUsed());
						break;
					case 2:
						// Free
						dynamicMeasure.setValue(tablespace.getFree());
						// set non-dynamic measure if exists
						setNonDynamicMeasure(env, metricGroupName, tablespace.getName() + "_FREE", tablespace.getFree());
						break;
					case 3:
						// Percent Used
						dynamicMeasure.setValue(tablespace.getPercentUsed());
						// set non-dynamic measure if exists
						setNonDynamicMeasure(env, metricGroupName, tablespace.getName() + "_USED_PCT", tablespace.getPercentUsed());
						break;
					case 4:
						// Percent Free
						dynamicMeasure.setValue(tablespace.getPercentFree());
						// set non-dynamic measure if exists
						setNonDynamicMeasure(env, metricGroupName, tablespace.getName() + "_FREE_PCT", tablespace.getPercentFree());
						break;
					default:
						log.severe("populateTablespacesMeasures method: index " + i + " is unknown. Index skipped");
				}
			}
		}
	  }
  }
  
  private void populateTopWaitEventsMeasures(MonitorEnvironment env, List<TopWaitEventsMetrics> topWaitEvents) {
	  log.finer("Inside of populateLocksMeasures method...");
	  
		  for (int i = 0; i < ORACLE_TOP_WAIT_EVENTS_METRICS.length; i++) {
			  log.finer("populateTopWaitEventsMeasures method: metric # " + i + ", metric name is '" + ORACLE_TOP_WAIT_EVENTS_METRICS[i] + "'");
			  for (TopWaitEventsMetrics topWaitEvent : topWaitEvents) {
				  log.finer("populateTopWaitEventsMeasures method: lock's key is '" + topWaitEvent.getKey() + "'");
				  Collection<MonitorMeasure> monitorMeasures = env.getMonitorMeasures(TOP_WAIT_EVENTS_METRIC_GROUP, ORACLE_TOP_WAIT_EVENTS_METRICS[i]);
					for (MonitorMeasure subscribedMonitorMeasure : monitorMeasures) {
						MonitorMeasure dynamicMeasure = env.createDynamicMeasure(subscribedMonitorMeasure, TOP_WAIT_EVENTS_SPLIT_NAME, topWaitEvent.getKey());
						switch (i) {
						case 0:
							// total waits
							dynamicMeasure.setValue(topWaitEvent.getTotalWaits());
							break;
						case 1:
							// total timeouts
							dynamicMeasure.setValue(topWaitEvent.getTotalTimeouts());
							break;
						case 2:
							// time waited
							dynamicMeasure.setValue(topWaitEvent.getTimeWaited());
							break;
						case 3:
							// average wait
							dynamicMeasure.setValue(topWaitEvent.getAverageWait());
							break;
						default:
							log.severe("populateTopWaitEventsMeasures method: index " + i + " is unknown. Index skipped");
						}		
					}
			  }
		  }
  }
  
  private void setNonDynamicMeasure(MonitorEnvironment env, String mg, String measureName, double d) {
	  Collection<MonitorMeasure> measures = env.getMonitorMeasures(mg, measureName);
		for (MonitorMeasure measure : measures) {
			log.finer("setNonDynamicMeasure method: Populating SUCCESS...");
			measure.setValue(d);
		}
  }

  private void populateSqlMeasures(MonitorEnvironment env, List<SqlMetrics> sqls) {
	  log.finer("Inside of populateSqlMeasures method...");
	  
	  for (int i = 0; i < ORACLE_SQL_METRICS.length; i++) {
		  log.finer("populateSqlMeasures method: metric # " + i + ", metric name is '" + ORACLE_SQL_METRICS[i] + "'");
		  for (SqlMetrics sql : sqls) {
			  log.finer("populateSqlMeasures method: sql's sid is '" + sql.getSid() + "', key is '" + sql.getKey() + "'");
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
	  log.finer("Inside of populateLocksMeasures method...");
	  
		  for (int i = 0; i < ORACLE_LOCKS_METRICS.length; i++) {
			  log.finer("populateLocksMeasures method: metric # " + i + ", metric name is '" + ORACLE_LOCKS_METRICS[i] + "'");
			  for (LocksMetrics lock : locks) {
				  log.finer("populateLocksMeasures method: lock's key is '" + lock.getKey() + "'");
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
	if (log.isLoggable(Level.FINER)) {
		log.finer("Entering replaceImagesIntoHtmlPage method");
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
//      if (log.isLoggable(Level.FINER)) {
//           log.finer("replaceImagesIntoHtmlPage method: modified page is '" + doc.html() + "'");
//		}
     
    return doc.html();
  }
  
  public static List<String> getImagesFromExcelReport(Workbook wb, URL footerUrl) throws IOException {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getImagesFromExcelReport method");
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
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getImageAsString method");
		}
		// get images for HTML img tag as data image png file base64 encoded. Examples, "res/header.png", "res/footer.png"
//		return new String(Base64.encodeBase64(IOUtils.toByteArray(this.getClass().getClassLoader().getResource(fileName))));
		return new String(Base64.encodeBase64(IOUtils.toByteArray(url)), Charset.defaultCharset());

  }
  
  public static File getNewHtmlPageFile(String page, String dashboard) throws IOException {
		if (log.isLoggable(Level.FINER)) {
			log.finer("Entering getNewHtmlPageFile method");
		}
		File file = File.createTempFile(new StringBuilder(dashboard).append(HTML_FILE_UPDATED).toString(), HTML_FILE_SUFFIX);
		file.deleteOnExit();
		if (log.isLoggable(Level.FINER)) {
			log.finer("getNewHtmlPageFile method: canonical path to file is '" + file.getCanonicalPath() + "'");
//			log.finer("getNewHtmlPageFile method: page is '" + page + "'");
		}
		FileUtils.writeStringToFile(file, page);
		return file;
  }
	
  
}