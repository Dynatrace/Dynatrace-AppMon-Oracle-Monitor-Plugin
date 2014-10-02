<html xmlns="http://www.w3.org/1999/xhtml">
<head>
    <title>Oracle Monitor Plugin</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=EmulateIE8" />
    <meta content="Scroll Wiki Publisher" name="generator"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/liquid.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/blueprint/print.css" media="print"/>
    <link type="text/css" rel="stylesheet" href="css/content-style.css" media="screen, projection, print"/>
    <link type="text/css" rel="stylesheet" href="css/screen.css" media="screen, projection"/>
    <link type="text/css" rel="stylesheet" href="css/print.css" media="print"/>
</head>
<body>
                <h1>Oracle Monitor Plugin</h1>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-Overview"  >
        <h2>Overview</h2>
    <p>
            <img src="images_community/download/attachments/32702472/oraccle_db_dashboard.png" alt="images_community/download/attachments/32702472/oraccle_db_dashboard.png" class="" />
            </p>
    <p>
The Oracle Monitor plugin enables monitoring the values provided in Oracle's v$ tables.The plugin uses JDBC to connect to the Oracle Database and queries the most important metrics from these tables. Having these measures in dynaTrace enables quick correlation of database related performance issues such as high I/O or too many database connections to application transaction performance problems such as long running transactions or slow database queries.    </p>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-PluginDetails"  >
        <h2>Plugin Details</h2>
    <div class="tablewrap">
        <table>
<thead class=" "></thead><tfoot class=" "></tfoot><tbody class=" ">    <tr>
            <td rowspan="1" colspan="1">
        <p>
Plug-In Files    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<strong class=" ">dynaTrace 3.2</strong>:<br/><a href="attachments_32866306_2_com.dynatrace.diagnostics.plugins.OraclePlugin_1.0.5.jar">Oracle Monitor Plugin 1.0.5</a><br/><a href="attachments_32866307_3_Oracle_Database_Dashboard.dashboard.xml">Oracle Monitor Dashboard </a><br/><br/><strong class=" ">dynaTrace 3.5+</strong>:<br/><a href="attachments_50036932_1_com.dynatrace.diagnostics.plugins.OraclePlugin_1.0.8.jar">Oracle Monitor Plugin 1.0.8</a><br/><a href="attachments_42041346_2_Oracle_Database_Dashboard_3.5.dashboard.xml">Oracle Monitor Dashboard</a>    </p>
    <p>
<strong class=" ">dynaTrace 5.x+:</strong><br/><a href="attachments_166232209_1_com.dynatrace.diagnostics.plugins.OraclePlugin_1.0.9.jar">Enhanced Oracle Monitor Plugin 1.0.9<br/></a><a href="attachments_161940480_2_SqlMVC.war">Web UI war</a> file for historical analysis of SQL statements, database locks, explain plans, etc. See section 3 of plugin <a href="attachments_161940484_1_Oracle_Database_Enhanced_Plugin_v._1.0.9.docx">documentation</a> for instructions about deploying web UI application on the application server of choice.<br/><a href="attachments_32866307_3_Oracle_Database_Dashboard.dashboard.xml">Example of Oracle Monitor Dashboard</a><br/><a href="attachments_163546743_2_create_top_sqls_ddl.sql">DDL file with supporting tables for Web UI application</a> for Oracle database<br/><a href="attachments_161939639_1_database.properties">database.properties</a> file<br/><a href="attachments_161940484_1_Oracle_Database_Enhanced_Plugin_v._1.0.9.docx">Documentation</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Author    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
Chuck Miller (chuck.miller@dynatrace.com) &amp; Joe Hoffman (joe.hoffman@dynatrace.com)<br/>Thanks to Yakov Sobolev at JPMorgan Chase for  helping add Service Name support to this plugin<br/>Eugene Turetsky (<a href="mailto:eugene.turetsky@compuware.com">eugene.turetsky@compuware.com</a>) Enhanced Oracle Monitor Plugin v. 1.0.9.    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
dynaTrace Versions    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
3.x, 4.x, 5.x    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
License    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="attachments_5275722_2_dynaTraceBSD.txt">dynaTrace BSD</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Support    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
<a href="https://community/display/DL/Support+Levels">Not Supported</a>    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Known Problems    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
    </p>
            </td>
        </tr>
    <tr>
            <td rowspan="1" colspan="1">
        <p>
Release History    </p>
            </td>
                <td rowspan="1" colspan="1">
        <p>
2010-06-30 1.0.5 Initial Release<br/>2010-11-30 1.0.7 Updated for 3.5, handle reconnect after DB restart, improved logging<br/>2011-06-06 1.0.8 Updated to handle the Oracle Thin Driver syntax for clustered databases which are using a Service name<br/>2014-03-28 1.0.9 Added support for historical analysis of top N slow SQL statements, historical analysis of database locks, historical analysis of explain plans etc. (see <a href="attachments_161940484_1_Oracle_Database_Enhanced_Plugin_v._1.0.9.docx">documentation</a> for details)    </p>
            </td>
        </tr>
</tbody>        </table>
            </div>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-ProvidedMeasures"  >
        <h2>Provided Measures</h2>
    <p>
The following image shows the metrics that the monitor provides:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/oracle_monitor_metrics.png" alt="images_community/download/attachments/32702472/oracle_monitor_metrics.png" class="confluence-embedded-image" />
            </p>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-NewCapabilitiesoftheEnhancedOracleMonitorPlugin"  >
        <h2>New Capabilities of the Enhanced Oracle Monitor Plugin</h2>
    <p>
Enhanced Oracle Monitor Plugin adds to the existing Oracle Monitor Plugin ability to gather statistics on the SQL level. It provides customers with the following statistics for every SQL statement:    </p>
<ul class=" "><li class=" ">    <p>
SQL SID    </p>
</li><li class=" ">    <p>
SQL Full Text    </p>
</li><li class=" ">    <p>
Child Number    </p>
</li><li class=" ">    <p>
Number of Executions    </p>
</li><li class=" ">    <p>
Elapsed Time    </p>
</li><li class=" ">    <p>
Average Elapsed Time    </p>
</li><li class=" ">    <p>
CPU Time    </p>
</li><li class=" ">    <p>
Average CPU Time    </p>
</li><li class=" ">    <p>
Disk Reads    </p>
</li><li class=" ">    <p>
Direct Writes    </p>
</li><li class=" ">    <p>
Buffer Gets    </p>
</li><li class=" ">    <p>
Rows Processed    </p>
</li><li class=" ">    <p>
Parse Calls    </p>
</li><li class=" ">    <p>
First Load Time    </p>
</li><li class=" ">    <p>
Last Load Time    </p>
</li></ul>    <p>
For in-depth SQL analysis there is SQL explain plan which was captured at the time when this SQL statement was executed.    </p>
    <p>
Besides detailed SQL level statistics there are stats about database locks, tablespaces (coming) etc. which give user additional information about state of the database. For database locks the following information is captured:    </p>
<ul class=" "><li class=" ">    <p>
Concatenation of the session_id from the gv$locked_object view and serial# from the v$session view    </p>
</li><li class=" ">    <p>
Oracle User    </p>
</li><li class=" ">    <p>
Object Name    </p>
</li><li class=" ">    <p>
Object Type    </p>
</li><li class=" ">    <p>
Lock Mode    </p>
</li><li class=" ">    <p>
Status    </p>
</li><li class=" ">    <p>
Last DDL Time    </p>
</li></ul>    <p>
Plugin keeps information in the performance warehouse (or in any external relational database) and hence allows going back in history to compare performance of the SQL in question over time. The Web UI piece of the plugin handles getting historical data and allows performing analysis of the slow SQL statements, explain plans, locks, etc. historically.    </p>
    <p>
Following screenshot contains top N SQL statements Dashlet:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/slow_top_N_SQL_dashlet.png" alt="images_community/download/attachments/32702472/slow_top_N_SQL_dashlet.png" class="" />
        </p>
    <p>
Following screenshot contains Database Locks Dashlet:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/Database_Locks_dashlet.png" alt="images_community/download/attachments/32702472/Database_Locks_dashlet.png" class="" />
        </p>
    <p>
Following screenshot contains Analysis Dashlet for top N SQL statements:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/analysis_of_top_N_SQL_dashlet.png" alt="images_community/download/attachments/32702472/analysis_of_top_N_SQL_dashlet.png" class="" />
        </p>
    <p>
Following screenshot contains Explain Plans Dashlet taken at the time of SQL statement execution:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/analysis_of_Explain_Plans_Dashlet.png" alt="images_community/download/attachments/32702472/analysis_of_Explain_Plans_Dashlet.png" class="" />
        </p>
    <p>
Following screenshot contains Analysis of Database Locks Dashlet:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/analysis_of_Database_Locks_dashlet.png" alt="images_community/download/attachments/32702472/analysis_of_Database_Locks_dashlet.png" class="" />
        </p>
    <p>
Following screenshot shows list of dynamic measures which are gathered by the plugin for every top N SQL statement:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/dynamic_measures_captured_by_the_plugin_for_every_top_N_SQL_statement.png" alt="images_community/download/attachments/32702472/dynamic_measures_captured_by_the_plugin_for_every_top_N_SQL_statement.png" class="" />
        </p>
    <p>
Following screenshot shows list of dynamic measures which are gathered by the plugin for every database lock:    </p>
    <p>
            <img src="images_community/download/attachments/32702472/dynamic_measures_captured_by_the_plugin_for_every_database_lock.png" alt="images_community/download/attachments/32702472/dynamic_measures_captured_by_the_plugin_for_every_database_lock.png" class="" />
    <br/>Please see <a href="attachments_161940484_1_Oracle_Database_Enhanced_Plugin_v._1.0.9.docx">documentation</a> of the Enhanced Oracle Monitor Plugin for more details.    </p>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-DeployingEnhancedOracleMonitorPluginandwebUIapplication"  >
        <h2>Deploying Enhanced Oracle Monitor Plugin and web UI application</h2>
    <p>
Besides regular steps which are described for the previous versions of the Oracle Monitor Plugin in the &quot;Configuration Oracle Monitor&quot; section below, there are following deployment steps which need to be done before Enhanced Oracle Monitor Plugin and web UI application can be used:    </p>
<ol class=" "><li class=" ">    <p>
Create supporting tables for the historical analysis:    </p>
<ol class=" "><li class=" ">    <p>
Changing owner's placeholder name in the <a href="attachments_163546743_2_create_top_sqls_ddl.sql">create_top_sqls_ddl.sql</a> script from &quot;c##dt55&quot; to a real name which will be used by the web UI application.    </p>
</li><li class=" ">    <p>
Execute the <a href="attachments_163546743_2_create_top_sqls_ddl.sql">create_top_sqls_ddl.sql</a> script using Oracle sqlplus utility or SQL Developer.    </p>
</li><li class=" ">    <p>
Note that the <a href="attachments_163546743_2_create_top_sqls_ddl.sql">create_top_sqls_ddl.sql</a> script is valid for Oracle database only. We will be posting shortly scripts for creation of the supporting tables for other databases like MS SQL, DB2, PostgreSQL, etc.    </p>
</li></ol></li><li class=" ">    <p>
Change values of the ${db.url}, ${db.user}, and ${db.password} variables in the <a href="attachments_161939641_1_database.properties_file.png">database.properties</a> file to the appropriate values. See database properties file below:    </p>
            <img src="images_community/download/attachments/32702472/database.properties_file.png" alt="images_community/download/attachments/32702472/database.properties_file.png" class="confluence-embedded-image" />
        </li><li class=" ">    <p>
Set environmental variable &ldquo;ext.prop.dir&rdquo; to a directory where the database.properties file is located. For example, execute command<br/>&ldquo;set ext.prop.dir=C:\Users\dmaext0\&rdquo;<br/>on Windows OS to set directory value to C:\Users\dmaext0\.<br/><i class=" ">Note: </i>    </p>
<ol class=" "><li class=" ">    <p>
Do not forget to add &ldquo;\&rdquo; (backward slash) at the end of the directory name. On Unix systems use &ldquo;/&rdquo;.    </p>
</li><li class=" ">    <p>
Make sure that user which owns application server process (e.g. Tomcat, Jetty, JBoss, etc.) where web UI application will be executed has read access to the database.properties file.    </p>
</li></ol></li><li class=" ">    <p>
Use standard deployment procedure to deploy web UI war file on the application server of choice:    </p>
</li></ol><ol class=" "><li class=" ">    <p>
For Tomcat 7.0 the standard deployment procedure is described <a href="https://tomcat.apache.org/tomcat-7.0-doc/appdev/deployment.html">here</a>. One of the options to deploy war file (probably the easiest one) is to follow up steps from the following extract from the above article:<br/><i class=" ">Copy the web application archive file into directory </i><tt class=" ">$CATALINA_BASE/webapps/</tt>. When Tomcat is started, it will automatically expand the web application archive file into its unpacked form, and execute the application that way. This approach would typically be used to install an additional application, provided by a third party vendor or by your internal development staff, into an existing Tomcat installation. <strong class=" ">NOTE</strong> - If you use this approach, and wish to update your application later, you must both replace the web application archive file <strong class=" ">AND</strong> delete the expanded directory that Tomcat created, and then restart Tomcat, in order to reflect your changes.    </p>
</li></ol>    <p>
See section 3 of the plugin <a href="attachments_161940484_1_Oracle_Database_Enhanced_Plugin_v._1.0.9.docx">documentation</a> for more details about web UI war deployment process.    </p>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-AccessprivilegestotheOraclesystemtablesandviewsfortheEnhancedOracleMonitorPlugin"  >
        <h2>Access privileges to the Oracle system tables and views for the Enhanced Oracle Monitor Plugin</h2>
    <p>
Oracle user of the monitored database needs to have SELECT privileges for the following objects:    </p>
<ol class=" "><li class=" ">    <p>
v$buffer_pool_statistics    </p>
</li><li class=" ">    <p>
v$sysstat    </p>
</li><li class=" ">    <p>
v$librarycache    </p>
</li><li class=" ">    <p>
v$license    </p>
</li><li class=" ">    <p>
dba_objects    </p>
</li><li class=" ">    <p>
gv$locked_object    </p>
</li><li class=" ">    <p>
v$session    </p>
</li><li class=" ">    <p>
v$sql    </p>
</li></ol>    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-ConfigurationOracleMonitor"  >
        <h2>Configuration Oracle Monitor</h2>
    <p>
The monitor requires the following configuration settings:    </p>
<ul class=" "><li class=" ">    <p>
hostName: Host name of the Oracle Database Instance    </p>
</li><li class=" ">    <p>
dbName: Database Instance Name (SID) or Service name    </p>
</li><li class=" ">    <p>
dbUsername: Username that is used to access the database. User needs to have query rights to v$ tables    </p>
</li><li class=" ">    <p>
dbPassword: Password that is used to access the database    </p>
</li><li class=" ">    <p>
dbPort: Oracle Database Port for JDBC Connections (default: 1521)    </p>
</li></ul>    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-Installation"  >
        <h2>Installation</h2>
    <p>
Import the Plugin into the dynaTrace Server via the dynaTrace Server Settings menu -&gt; Plugins -&gt; Install Plugin. For details how to do this please refer to the <a href="https://community.dynatrace.com/community/display/DOCDT32/Manage+and+Develop+Plugins#ManageandDevelopPlugins-ManageandDevelopPlugins">dynaTrace documentation</a>.    </p>
    <p>
To use the provided dashboard please leave the default name of the Monitor as &quot;RepositoryDB&quot;, then open the Dashboard and set the Data Source accordingly.    </p>
    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-AccessRequirements"  >
        <h2>Access Requirements</h2>
    <p>
This plugin accesses the following tables requiring only Connect and Resource roles.    </p>
<ul class=" "><li class=" ">    <p>
v$buffer_pool_statistics    </p>
</li><li class=" ">    <p>
v$sysstat    </p>
</li><li class=" ">    <p>
v$librarycache    </p>
</li><li class=" ">    <p>
v$license    </p>
</li></ul>    </div>
    <div class="section-2"  id="32702472_OracleMonitorPlugin-UsageNotes"  >
        <h2>Usage Notes</h2>
    <p>
As of v1.0.8 the Thin Driver syntax is now supported which provides support for clustered databases which use a service name. The DBName (SID) syntax is also still supported.    </p>
    </div>
            </div>
        </div>
        <div class="footer">
        </div>
    </div>
</body>
</html>
