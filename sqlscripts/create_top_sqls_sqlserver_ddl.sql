USE EMAP_HISTORY
GO
IF OBJECT_ID('dbo.TOP_SQL', 'U') IS NOT NULL
	DROP TABLE dbo.TOP_SQL ;
GO
IF OBJECT_ID ('dbo.TOP_SQL_FULLTEXT', 'U') IS NOT NULL
   DROP TABLE TOP_SQL_FULLTEXT;
GO
IF OBJECT_ID ('dbo.T_LOCKS', 'U') IS NOT NULL
   DROP TABLE T_LOCKS;
GO
IF OBJECT_ID ('dbo.TOP_SQL_EXPLAIN_PLAN', 'U') IS NOT NULL
   DROP TABLE TOP_SQL_EXPLAIN_PLAN;
GO
IF OBJECT_ID ('dbo.T_EXPLAIN_PLAN', 'U') IS NOT NULL
   DROP TABLE T_EXPLAIN_PLAN;
GO
IF OBJECT_ID ('dbo.T_SRC_DB_NAME', 'U') IS NOT NULL
   DROP TABLE T_SRC_DB_NAME;
GO
IF OBJECT_ID ('dbo.T_TABLESPACES', 'U') IS NOT NULL
   DROP TABLE T_TABLESPACES;
GO
IF OBJECT_ID ('dbo.explain_plan_seq', 'SO') IS NOT NULL
   DROP SEQUENCE explain_plan_seq;
GO
IF OBJECT_ID ('dbo.src_db_name_seq', 'SO') IS NOT NULL
   DROP SEQUENCE src_db_name_seq;
GO

CREATE TABLE dbo.TOP_SQL 
(
  TIMESLICE DATETIME NOT NULL 
, SQL_ID VARCHAR(20) NOT NULL
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, EXECUTIONS NUMERIC(38,9) 
, ELAPSED_TIME NUMERIC(38,9) 
, CPU_TIME NUMERIC(38,9) 
, DISK_READS NUMERIC(38,9) 
, DIRECT_WRITES NUMERIC(38,9) 
, BUFFER_GETS NUMERIC(38,9) 
, ROWS_PROCESSED NUMERIC(38,9) 
, PARSE_CALLS NUMERIC(38,9) 
, FIRST_LOAD_TIME VARCHAR(19) 
, LAST_LOAD_TIME VARCHAR(19) 
, CHILD_NUMBER NUMERIC(28) NOT NULL
);

CREATE SEQUENCE explain_plan_seq
 START WITH     1
 INCREMENT BY   1
 NO CACHE
 NO CYCLE;

CREATE SEQUENCE src_db_name_seq
 START WITH     1
 INCREMENT BY   1
 NO CACHE
 NO CYCLE;

CREATE TABLE dbo.T_SRC_DB_NAME
(
  SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, SRC_DB_NAME VARCHAR(256) NOT NULL
, CREATE_DATE DATETIME NOT NULL
, ACCESS_DATE DATETIME NOT NULL
);

CREATE TABLE dbo.TOP_SQL_FULLTEXT 
(
  SQL_ID VARCHAR(20) NOT NULL 
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, SQL_FULLTEXT VARCHAR(max) NOT NULL 
, CREATE_DATE DATETIME NOT NULL
, ACCESS_DATE DATETIME NOT NULL
);

CREATE TABLE dbo.TOP_SQL_EXPLAIN_PLAN 
(
  TIMESLICE DATETIME NOT NULL 
, SQL_ID VARCHAR(20) NOT NULL 
, CHILD_NUMBER	NUMERIC(28) NOT NULL
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, EXPLAIN_PLAN_ID NUMERIC(28) 	  
);

CREATE TABLE dbo.T_EXPLAIN_PLAN 
(
  EXPLAIN_PLAN_ID NUMERIC(28) NOT NULL	
, PLAN_HASH_VALUE NUMERIC(28)
, SQL_ID VARCHAR(20) NOT NULL
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, CHILD_NUMBER NUMERIC(28) NOT NULL
, SQL_EXPLAIN_PLAN VARCHAR(MAX) 
, CREATE_DATE DATETIME NOT NULL
, ACCESS_DATE DATETIME NOT NULL
);

CREATE TABLE dbo.T_LOCKS 
(
  TIMESLICE DATETIME NOT NULL 
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, SID_SERIAL VARCHAR(80) NOT NULL 
, ORA_USER VARCHAR(30)
, OBJECT_NAME VARCHAR(128)
, OBJECT_TYPE VARCHAR(23)
, LOCK_MODE VARCHAR(15)
, STATUS VARCHAR(7)
, LAST_DDL VARCHAR(30)
);

CREATE TABLE dbo.T_TABLESPACES 
(
  TIMESLICE DATETIME NOT NULL 
, SRC_DB_NAME_ID NUMERIC(28) NOT NULL
, NAME VARCHAR(30) NOT NULL
, TOTAL NUMERIC(38,9)
, USED NUMERIC(38,9)
, FREE NUMERIC(38,9)
, PERCENT_USED NUMERIC(5,2)
, PERCENT_FREE NUMERIC(5,2)
);

CREATE UNIQUE INDEX PK_TOP_SQL ON dbo.TOP_SQL (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_TOP_SQL ON dbo.TOP_SQL (SQL_ID, TIMESLICE, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX IDX2_TOP_SQL ON dbo.TOP_SQL (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_TOP_SQL_FULLTEXT ON dbo.TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_TOP_SQL_EXPLAIN_PLAN ON dbo.TOP_SQL_EXPLAIN_PLAN (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_T_EXPLAIN_PLAN ON dbo.T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID);
CREATE INDEX IDX1_T_EXPLAIN_PLAN ON dbo.T_EXPLAIN_PLAN(SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX IDX2_T_EXPLAIN_PLAN ON dbo.T_EXPLAIN_PLAN(PLAN_HASH_VALUE, SRC_DB_NAME_ID);
CREATE INDEX IDX1_T_LOCKS ON dbo.T_LOCKS (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_T_SRC_DB_NAME ON dbo.T_SRC_DB_NAME (SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_T_SRC_DB_NAME ON dbo.T_SRC_DB_NAME (SRC_DB_NAME);
CREATE UNIQUE INDEX PK_T_TABLESPACES ON dbo.T_TABLESPACES (TIMESLICE, SRC_DB_NAME_ID, NAME);
GO