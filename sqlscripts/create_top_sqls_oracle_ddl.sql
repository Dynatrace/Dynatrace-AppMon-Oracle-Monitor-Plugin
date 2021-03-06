DROP INDEX C##DT55.PK_TOP_SQL;
DROP INDEX C##DT55.IDX1_TOP_SQL;
DROP INDEX C##DT55.PK_TOP_SQL_FULLTEXT;
DROP TABLE C##DT55.TOP_SQL;
DROP TABLE C##DT55.TOP_SQL_FULLTEXT;
DROP INDEX C##DT55.IDX1_T_LOCKS;
DROP TABLE C##DT55.T_LOCKS;
DROP SEQUENCE explain_plan_seq;
DROP INDEX C##DT55.IDX1_TOP_SQL_EXPLAIN_PLAN;
DROP TABLE C##DT55.TOP_SQL_EXPLAIN_PLAN;
DROP INDEX C##DT55.PK_T_EXPLAIN_PLAN;
DROP INDEX C##DT55.IDX1_T_EXPLAIN_PLAN; 
DROP INDEX C##DT55.IDX2_T_EXPLAIN_PLAN; 
DROP TABLE C##DT55.T_EXPLAIN_PLAN;
DROP SEQUENCE src_db_name_seq;
DROP TABLE C##DT55.T_SRC_DB_NAME;
DROP INDEX C##DT55.PK_T_TABLESPACES;
DROP TABLE C##DT55.T_TABLESPACES;

CREATE SEQUENCE explain_plan_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;

CREATE SEQUENCE src_db_name_seq
 START WITH     1
 INCREMENT BY   1
 NOCACHE
 NOCYCLE;

CREATE TABLE C##DT55.TOP_SQL 
(
  TIMESLICE TIMESTAMP(6) NOT NULL 
, SQL_ID VARCHAR2(20) NOT NULL
, SRC_DB_NAME_ID NUMBER NOT NULL
, EXECUTIONS NUMBER 
, ELAPSED_TIME NUMBER 
, CPU_TIME NUMBER 
, DISK_READS NUMBER 
, DIRECT_WRITES NUMBER 
, BUFFER_GETS NUMBER 
, ROWS_PROCESSED NUMBER 
, PARSE_CALLS NUMBER 
, FIRST_LOAD_TIME VARCHAR2(19) 
, LAST_LOAD_TIME VARCHAR2(19) 
, CHILD_NUMBER NUMBER NOT NULL
);

CREATE TABLE C##DT55.T_SRC_DB_NAME
(
  SRC_DB_NAME_ID NUMBER NOT NULL
, SRC_DB_NAME VARCHAR2(240) NOT NULL
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE C##DT55.TOP_SQL_FULLTEXT 
(
  SQL_ID VARCHAR2(20) NOT NULL 
, SRC_DB_NAME_ID NUMBER NOT NULL
, SQL_FULLTEXT CLOB NOT NULL 
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE C##DT55.TOP_SQL_EXPLAIN_PLAN 
(
  TIMESLICE TIMESTAMP(6) NOT NULL 
, SQL_ID VARCHAR2(20) NOT NULL 
, CHILD_NUMBER NUMBER NOT NULL
, SRC_DB_NAME_ID NUMBER NOT NULL
, EXPLAIN_PLAN_ID NUMBER 	  
);

CREATE TABLE C##DT55.T_EXPLAIN_PLAN 
(
  EXPLAIN_PLAN_ID NUMBER NOT NULL	
, PLAN_HASH_VALUE NUMBER
, SQL_ID VARCHAR2(20) NOT NULL
, SRC_DB_NAME_ID NUMBER NOT NULL
, CHILD_NUMBER NUMBER NOT NULL
, SQL_EXPLAIN_PLAN CLOB 
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE C##DT55.T_LOCKS 
(
  TIMESLICE TIMESTAMP(6) NOT NULL 
, SRC_DB_NAME_ID NUMBER NOT NULL
, SID_SERIAL VARCHAR2(80) NOT NULL 
, ORA_USER VARCHAR2(30)
, OBJECT_NAME VARCHAR2(128)
, OBJECT_TYPE VARCHAR2(23)
, LOCK_MODE VARCHAR2(15)
, STATUS VARCHAR2(7)
, LAST_DDL VARCHAR2(30)
);

CREATE TABLE C##DT55.T_TABLESPACES 
(
  TIMESLICE TIMESTAMP(6) NOT NULL 
, SRC_DB_NAME_ID NUMBER NOT NULL
, NAME VARCHAR2(30) NOT NULL
, TOTAL NUMBER
, USED NUMBER
, FREE NUMBER
, PERCENT_USED NUMBER(5, 2)
, PERCENT_FREE NUMBER(5, 2)
);

CREATE UNIQUE INDEX C##DT55.PK_TOP_SQL ON C##DT55.TOP_SQL (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.IDX1_TOP_SQL ON C##DT55.TOP_SQL (SQL_ID, TIMESLICE, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX C##DT55.IDX2_TOP_SQL ON C##DT55.TOP_SQL (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.PK_TOP_SQL_FULLTEXT ON C##DT55.TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.IDX1_TOP_SQL_EXPLAIN_PLAN ON C##DT55.TOP_SQL_EXPLAIN_PLAN (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.PK_T_EXPLAIN_PLAN ON C##DT55.T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID);
CREATE INDEX C##DT55.IDX1_T_EXPLAIN_PLAN ON C##DT55.T_EXPLAIN_PLAN(SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX C##DT55.IDX2_T_EXPLAIN_PLAN ON C##DT55.T_EXPLAIN_PLAN(PLAN_HASH_VALUE, SRC_DB_NAME_ID);
CREATE INDEX C##DT55.IDX1_T_LOCKS ON C##DT55.T_LOCKS (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.PK_T_SRC_DB_NAME ON C##DT55.T_SRC_DB_NAME (SRC_DB_NAME_ID);
CREATE UNIQUE INDEX C##DT55.IDX1_T_SRC_DB_NAME ON C##DT55.T_SRC_DB_NAME (SRC_DB_NAME);
CREATE UNIQUE INDEX C##DT55.PK_T_TABLESPACES ON C##DT55.T_TABLESPACES (TIMESLICE, SRC_DB_NAME_ID, NAME);