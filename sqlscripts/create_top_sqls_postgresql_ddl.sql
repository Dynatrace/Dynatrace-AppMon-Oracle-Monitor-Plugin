DROP INDEX PK_TOP_SQL;
DROP INDEX IDX1_TOP_SQL;
DROP INDEX PK_TOP_SQL_FULLTEXT;
DROP TABLE TOP_SQL;
DROP TABLE TOP_SQL_FULLTEXT;
DROP INDEX IDX1_T_LOCKS;
DROP TABLE T_LOCKS;
DROP SEQUENCE explain_plan_seq;
DROP INDEX IDX1_TOP_SQL_EXPLAIN_PLAN;
DROP TABLE TOP_SQL_EXPLAIN_PLAN;
DROP INDEX PK_T_EXPLAIN_PLAN;
DROP INDEX IDX1_T_EXPLAIN_PLAN; 
DROP INDEX IDX2_T_EXPLAIN_PLAN; 
DROP TABLE T_EXPLAIN_PLAN;
DROP SEQUENCE src_db_name_seq;
DROP TABLE T_SRC_DB_NAME;
DROP INDEX PK_T_TABLESPACES;
DROP TABLE T_TABLESPACES;

CREATE SEQUENCE explain_plan_seq
 START WITH     1
 INCREMENT BY   1
 NO CYCLE;

CREATE SEQUENCE src_db_name_seq
 START WITH     1
 INCREMENT BY   1
 NO CYCLE;

CREATE TABLE TOP_SQL 
(
  TIMESLICE TIMESTAMP NOT NULL 
, SQL_ID VARCHAR(20) NOT NULL
, SRC_DB_NAME_ID NUMERIC NOT NULL
, EXECUTIONS NUMERIC 
, ELAPSED_TIME NUMERIC 
, CPU_TIME NUMERIC 
, DISK_READS NUMERIC 
, DIRECT_WRITES NUMERIC 
, BUFFER_GETS NUMERIC 
, ROWS_PROCESSED NUMERIC 
, PARSE_CALLS NUMERIC 
, FIRST_LOAD_TIME VARCHAR(19) 
, LAST_LOAD_TIME VARCHAR(19) 
, CHILD_NUMBER NUMERIC NOT NULL
);

CREATE TABLE T_SRC_DB_NAME
(
  SRC_DB_NAME_ID NUMERIC NOT NULL
, SRC_DB_NAME VARCHAR(256) NOT NULL
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE TOP_SQL_FULLTEXT 
(
  SQL_ID VARCHAR(20) NOT NULL 
, SRC_DB_NAME_ID NUMERIC NOT NULL
, SQL_FULLTEXT TEXT NOT NULL 
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE TOP_SQL_EXPLAIN_PLAN 
(
  TIMESLICE TIMESTAMP NOT NULL 
, SQL_ID VARCHAR(20) NOT NULL 
, CHILD_NUMBER NUMERIC NOT NULL
, SRC_DB_NAME_ID NUMERIC NOT NULL
, EXPLAIN_PLAN_ID NUMERIC 	  
);

CREATE TABLE T_EXPLAIN_PLAN 
(
  EXPLAIN_PLAN_ID NUMERIC NOT NULL	
, PLAN_HASH_VALUE NUMERIC
, SQL_ID VARCHAR(20) NOT NULL
, SRC_DB_NAME_ID NUMERIC NOT NULL
, CHILD_NUMBER NUMERIC NOT NULL
, SQL_EXPLAIN_PLAN TEXT 
, CREATE_DATE TIMESTAMP NOT NULL
, ACCESS_DATE TIMESTAMP NOT NULL
);

CREATE TABLE T_LOCKS 
(
  TIMESLICE TIMESTAMP NOT NULL 
, SRC_DB_NAME_ID NUMERIC NOT NULL
, SID_SERIAL VARCHAR(80) NOT NULL 
, ORA_USER VARCHAR(30)
, OBJECT_NAME VARCHAR(128)
, OBJECT_TYPE VARCHAR(23)
, LOCK_MODE VARCHAR(15)
, STATUS VARCHAR(7)
, LAST_DDL VARCHAR(30)
);

CREATE TABLE T_TABLESPACES 
(
  TIMESLICE TIMESTAMP NOT NULL 
, SRC_DB_NAME_ID NUMERIC NOT NULL
, NAME VARCHAR(30) NOT NULL
, TOTAL NUMERIC
, USED NUMERIC
, FREE NUMERIC
, PERCENT_USED NUMERIC(5, 2)
, PERCENT_FREE NUMERIC(5, 2)
);

CREATE UNIQUE INDEX PK_TOP_SQL ON TOP_SQL (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_TOP_SQL ON TOP_SQL (SQL_ID, TIMESLICE, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX IDX2_TOP_SQL ON TOP_SQL (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_TOP_SQL_FULLTEXT ON TOP_SQL_FULLTEXT (SQL_ID, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_TOP_SQL_EXPLAIN_PLAN ON TOP_SQL_EXPLAIN_PLAN (TIMESLICE, SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_T_EXPLAIN_PLAN ON T_EXPLAIN_PLAN (EXPLAIN_PLAN_ID);
CREATE INDEX IDX1_T_EXPLAIN_PLAN ON T_EXPLAIN_PLAN(SQL_ID, CHILD_NUMBER, SRC_DB_NAME_ID);
CREATE INDEX IDX2_T_EXPLAIN_PLAN ON T_EXPLAIN_PLAN(PLAN_HASH_VALUE, SRC_DB_NAME_ID);
CREATE INDEX IDX1_T_LOCKS ON T_LOCKS (TIMESLICE, SRC_DB_NAME_ID);
CREATE UNIQUE INDEX PK_T_SRC_DB_NAME ON T_SRC_DB_NAME (SRC_DB_NAME_ID);
CREATE UNIQUE INDEX IDX1_T_SRC_DB_NAME ON T_SRC_DB_NAME (SRC_DB_NAME);
CREATE UNIQUE INDEX PK_T_TABLESPACES ON T_TABLESPACES (TIMESLICE, SRC_DB_NAME_ID, NAME);