/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.mysql;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Types;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

import io.debezium.relational.Column;
import io.debezium.relational.Table;
import io.debezium.relational.TableId;
import io.debezium.relational.Tables;
import io.debezium.relational.ddl.DdlParserListener.Event;
import io.debezium.relational.ddl.SimpleDdlParserListener;
import io.debezium.util.IoUtil;
import io.debezium.util.Strings;
import io.debezium.util.Testing;

public class MySqlDdlParserTest {

    private MySqlDdlParser parser;
    private Tables tables;
    private SimpleDdlParserListener listener;

    @Before
    public void beforeEach() {
        parser = new MySqlDdlParser();
        tables = new Tables();
        listener = new SimpleDdlParserListener();
        parser.addListener(listener);
    }

    @Test
    public void shouldParseMultipleStatements() {
        String ddl = "CREATE TABLE foo ( " + System.lineSeparator()
                + " c1 INTEGER NOT NULL, " + System.lineSeparator()
                + " c2 VARCHAR(22) " + System.lineSeparator()
                + "); " + System.lineSeparator()
                + "-- This is a comment" + System.lineSeparator()
                + "DROP TABLE foo;" + System.lineSeparator();
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(0); // table created and dropped
        listener.assertNext().createTableNamed("foo").ddlStartsWith("CREATE TABLE foo (");
        listener.assertNext().dropTableNamed("foo").ddlMatches("DROP TABLE foo");
    }

    @Test
    public void shouldParseAlterStatementsAfterCreate() {
        String ddl1 = "CREATE TABLE foo ( c1 INTEGER NOT NULL, c2 VARCHAR(22) );" + System.lineSeparator();
        String ddl2 = "ALTER TABLE foo ADD COLUMN c bigint;" + System.lineSeparator();
        parser.parse(ddl1, tables);
        parser.parse(ddl2, tables);
        listener.assertNext().createTableNamed("foo").ddlStartsWith("CREATE TABLE foo (");
        listener.assertNext().alterTableNamed("foo").ddlStartsWith("ALTER TABLE foo ADD COLUMN c");
    }

    @Test
    public void shouldParseAlterStatementsWithoutCreate() {
        String ddl = "ALTER TABLE foo ADD COLUMN c bigint;" + System.lineSeparator();
        parser.parse(ddl, tables);
        listener.assertNext().alterTableNamed("foo").ddlStartsWith("ALTER TABLE foo ADD COLUMN c");
    }

    @Test
    public void shouldParseCreateTableStatementWithSingleGeneratedAndPrimaryKeyColumn() {
        String ddl = "CREATE TABLE foo ( " + System.lineSeparator()
                + " c1 INTEGER NOT NULL AUTO_INCREMENT, " + System.lineSeparator()
                + " c2 VARCHAR(22) " + System.lineSeparator()
                + "); " + System.lineSeparator();
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table foo = tables.forTable(new TableId(null, null, "foo"));
        assertThat(foo).isNotNull();
        assertThat(foo.columnNames()).containsExactly("c1", "c2");
        assertThat(foo.primaryKeyColumnNames()).isEmpty();
        assertColumn(foo, "c1", "INTEGER", Types.INTEGER, -1, -1, false, true, true);
        assertColumn(foo, "c2", "VARCHAR", Types.VARCHAR, 22, -1, true, false, false);
    }

    @Test
    public void shouldParseCreateTableStatementWithSingleGeneratedColumnAsPrimaryKey() {
        String ddl = "CREATE TABLE my.foo ( " + System.lineSeparator()
                + " c1 INTEGER NOT NULL AUTO_INCREMENT, " + System.lineSeparator()
                + " c2 VARCHAR(22), " + System.lineSeparator()
                + " PRIMARY KEY (c1)" + System.lineSeparator()
                + "); " + System.lineSeparator();
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table foo = tables.forTable(new TableId("my", null, "foo"));
        assertThat(foo).isNotNull();
        assertThat(foo.columnNames()).containsExactly("c1", "c2");
        assertThat(foo.primaryKeyColumnNames()).containsExactly("c1");
        assertColumn(foo, "c1", "INTEGER", Types.INTEGER, -1, -1, false, true, true);
        assertColumn(foo, "c2", "VARCHAR", Types.VARCHAR, 22, -1, true, false, false);

        parser.parse("DROP TABLE my.foo", tables);
        assertThat(tables.size()).isEqualTo(0);
    }

    @Test
    public void shouldParseCreateTableStatementWithMultipleColumnsForPrimaryKey() {
        String ddl = "CREATE TABLE shop ("
                + " id BIGINT(20) NOT NULL AUTO_INCREMENT,"
                + " version BIGINT(20) NOT NULL,"
                + " name VARCHAR(255) NOT NULL,"
                + " owner VARCHAR(255) NOT NULL,"
                + " phone_number VARCHAR(255) NOT NULL,"
                + " primary key (id, name)"
                + " );";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table foo = tables.forTable(new TableId(null, null, "shop"));
        assertThat(foo).isNotNull();
        assertThat(foo.columnNames()).containsExactly("id", "version", "name", "owner", "phone_number");
        assertThat(foo.primaryKeyColumnNames()).containsExactly("id", "name");
        assertColumn(foo, "id", "BIGINT", Types.BIGINT, 20, -1, false, true, true);
        assertColumn(foo, "version", "BIGINT", Types.BIGINT, 20, -1, false, false, false);
        assertColumn(foo, "name", "VARCHAR", Types.VARCHAR, 255, -1, false, false, false);
        assertColumn(foo, "owner", "VARCHAR", Types.VARCHAR, 255, -1, false, false, false);
        assertColumn(foo, "phone_number", "VARCHAR", Types.VARCHAR, 255, -1, false, false, false);

        parser.parse("DROP TABLE shop", tables);
        assertThat(tables.size()).isEqualTo(0);
    }

    @Test
    public void shouldParseCreateUserTable() {
        String ddl = "CREATE TABLE IF NOT EXISTS user (   Host char(60) binary DEFAULT '' NOT NULL, User char(32) binary DEFAULT '' NOT NULL, Select_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Insert_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Update_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Delete_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Drop_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Reload_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Shutdown_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Process_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, File_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Grant_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, References_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Index_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Alter_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Show_db_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Super_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_tmp_table_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Lock_tables_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Execute_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Repl_slave_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Repl_client_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_view_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Show_view_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_routine_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Alter_routine_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_user_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Event_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Trigger_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, Create_tablespace_priv enum('N','Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, ssl_type enum('','ANY','X509', 'SPECIFIED') COLLATE utf8_general_ci DEFAULT '' NOT NULL, ssl_cipher BLOB NOT NULL, x509_issuer BLOB NOT NULL, x509_subject BLOB NOT NULL, max_questions int(11) unsigned DEFAULT 0  NOT NULL, max_updates int(11) unsigned DEFAULT 0  NOT NULL, max_connections int(11) unsigned DEFAULT 0  NOT NULL, max_user_connections int(11) unsigned DEFAULT 0  NOT NULL, plugin char(64) DEFAULT 'mysql_native_password' NOT NULL, authentication_string TEXT, password_expired ENUM('N', 'Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, password_last_changed timestamp NULL DEFAULT NULL, password_lifetime smallint unsigned NULL DEFAULT NULL, account_locked ENUM('N', 'Y') COLLATE utf8_general_ci DEFAULT 'N' NOT NULL, PRIMARY KEY Host (Host,User) ) engine=MyISAM CHARACTER SET utf8 COLLATE utf8_bin comment='Users and global privileges';";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table foo = tables.forTable(new TableId(null, null, "user"));
        assertThat(foo).isNotNull();
        assertThat(foo.columnNames()).contains("Host", "User", "Select_priv");
        assertColumn(foo, "Host", "CHAR BINARY", Types.BLOB, 60, -1, false, false, false);

        parser.parse("DROP TABLE user", tables);
        assertThat(tables.size()).isEqualTo(0);
    }

    @Test
    public void shouldParseCreateTableStatementWithSignedTypes() {
        String ddl = "CREATE TABLE foo ( " + System.lineSeparator()
                + " c1 BIGINT SIGNED NOT NULL, " + System.lineSeparator()
                + " c2 INT UNSIGNED NOT NULL " + System.lineSeparator()
                + "); " + System.lineSeparator();
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table foo = tables.forTable(new TableId(null, null, "foo"));
        assertThat(foo).isNotNull();
        assertThat(foo.columnNames()).containsExactly("c1", "c2");
        assertThat(foo.primaryKeyColumnNames()).isEmpty();
        assertColumn(foo, "c1", "BIGINT SIGNED", Types.BIGINT, -1, -1, false, false, false);
        assertColumn(foo, "c2", "INT UNSIGNED", Types.INTEGER, -1, -1, false, false, false);
    }

    @Test
    public void shouldParseCreateTableStatementWithCharacterSetForTable() {
        String ddl = "CREATE TABLE t ( col1 VARCHAR(25) ) DEFAULT CHARACTER SET utf8 DEFAULT COLLATE utf8_general_ci; ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId(null, null, "t"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("col1");
        assertThat(t.primaryKeyColumnNames()).isEmpty();
        assertColumn(t, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);

        ddl = "CREATE TABLE t2 ( col1 VARCHAR(25) ) DEFAULT CHARSET utf8 DEFAULT COLLATE utf8_general_ci; ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(2);
        Table t2 = tables.forTable(new TableId(null, null, "t2"));
        assertThat(t2).isNotNull();
        assertThat(t2.columnNames()).containsExactly("col1");
        assertThat(t2.primaryKeyColumnNames()).isEmpty();
        assertColumn(t2, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);

        ddl = "CREATE TABLE t3 ( col1 VARCHAR(25) ) CHARACTER SET utf8 COLLATE utf8_general_ci; ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(3);
        Table t3 = tables.forTable(new TableId(null, null, "t3"));
        assertThat(t3).isNotNull();
        assertThat(t3.columnNames()).containsExactly("col1");
        assertThat(t3.primaryKeyColumnNames()).isEmpty();
        assertColumn(t3, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);

        ddl = "CREATE TABLE t4 ( col1 VARCHAR(25) ) CHARSET utf8 COLLATE utf8_general_ci; ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(4);
        Table t4 = tables.forTable(new TableId(null, null, "t4"));
        assertThat(t4).isNotNull();
        assertThat(t4.columnNames()).containsExactly("col1");
        assertThat(t4.primaryKeyColumnNames()).isEmpty();
        assertColumn(t4, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);
    }

    @Test
    public void shouldParseCreateTableStatementWithCharacterSetForColumns() {
        String ddl = "CREATE TABLE t ( col1 VARCHAR(25) CHARACTER SET greek ); ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId(null, null, "t"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("col1");
        assertThat(t.primaryKeyColumnNames()).isEmpty();
        assertColumn(t, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);
    }

    @Test
    public void shouldParseAlterTableStatementThatAddsCharacterSetForColumns() {
        String ddl = "CREATE TABLE t ( col1 VARCHAR(25) ); ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId(null, null, "t"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("col1");
        assertThat(t.primaryKeyColumnNames()).isEmpty();
        assertColumn(t, "col1", "VARCHAR", Types.VARCHAR, 25, null, true);

        ddl = "ALTER TABLE t MODIFY col1 VARCHAR(50) CHARACTER SET greek;";
        parser.parse(ddl, tables);
        Table t2 = tables.forTable(new TableId(null, null, "t"));
        assertThat(t2).isNotNull();
        assertThat(t2.columnNames()).containsExactly("col1");
        assertThat(t2.primaryKeyColumnNames()).isEmpty();
        assertColumn(t2, "col1", "VARCHAR", Types.VARCHAR, 50, "greek", true);

        ddl = "ALTER TABLE t MODIFY col1 VARCHAR(75) CHARSET utf8;";
        parser.parse(ddl, tables);
        Table t3 = tables.forTable(new TableId(null, null, "t"));
        assertThat(t3).isNotNull();
        assertThat(t3.columnNames()).containsExactly("col1");
        assertThat(t3.primaryKeyColumnNames()).isEmpty();
        assertColumn(t3, "col1", "VARCHAR", Types.VARCHAR, 75, "utf8", true);
    }

    @Test
    public void shouldParseCreateDatabaseAndTableThatUsesDefaultCharacterSets() {
        String ddl = "SET character_set_server=utf8;" + System.lineSeparator()
                + "CREATE DATABASE db1 CHARACTER SET utf8mb4;" + System.lineSeparator()
                + "USE db1;" + System.lineSeparator()
                + "CREATE TABLE t1 (" + System.lineSeparator()
                + " id int(11) not null auto_increment," + System.lineSeparator()
                + " c1 varchar(255) default null," + System.lineSeparator()
                + " c2 varchar(255) charset default not null," + System.lineSeparator()
                + " c3 varchar(255) charset latin2 not null," + System.lineSeparator()
                + " primary key ('id')" + System.lineSeparator()
                + ") engine=InnoDB auto_increment=1006 default charset=latin1;" + System.lineSeparator();
        parser.parse(ddl, tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "utf8mb4"); // changes when we use a different database
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId("db1", null, "t1"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("id", "c1", "c2", "c3");
        assertThat(t.primaryKeyColumnNames()).containsExactly("id");
        assertColumn(t, "id", "INT", Types.INTEGER, 11, -1, false, true, true);
        assertColumn(t, "c1", "VARCHAR", Types.VARCHAR, 255, "latin1", true);
        assertColumn(t, "c2", "VARCHAR", Types.VARCHAR, 255, "latin1", false);
        assertColumn(t, "c3", "VARCHAR", Types.VARCHAR, 255, "latin2", false);

        // Create a similar table but without a default charset for the table ...
        ddl = "CREATE TABLE t2 (" + System.lineSeparator()
                + " id int(11) not null auto_increment," + System.lineSeparator()
                + " c1 varchar(255) default null," + System.lineSeparator()
                + " c2 varchar(255) charset default not null," + System.lineSeparator()
                + " c3 varchar(255) charset latin2 not null," + System.lineSeparator()
                + " primary key ('id')" + System.lineSeparator()
                + ") engine=InnoDB auto_increment=1006;" + System.lineSeparator();
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(2);
        Table t2 = tables.forTable(new TableId("db1", null, "t2"));
        assertThat(t2).isNotNull();
        assertThat(t2.columnNames()).containsExactly("id", "c1", "c2", "c3");
        assertThat(t2.primaryKeyColumnNames()).containsExactly("id");
        assertColumn(t2, "id", "INT", Types.INTEGER, 11, -1, false, true, true);
        assertColumn(t2, "c1", "VARCHAR", Types.VARCHAR, 255, "utf8mb4", true);
        assertColumn(t2, "c2", "VARCHAR", Types.VARCHAR, 255, "utf8mb4", false);
        assertColumn(t2, "c3", "VARCHAR", Types.VARCHAR, 255, "latin2", false);
    }

    @Test
    public void shouldParseCreateDatabaseAndUseDatabaseStatementsAndHaveCharacterEncodingVariablesUpdated() {
        parser.parse("SET character_set_server=utf8;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", null);

        parser.parse("CREATE DATABASE db1 CHARACTER SET utf8mb4;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", null); // changes when we USE a different database

        parser.parse("USE db1;", tables);// changes the "character_set_database" system variable ...
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "utf8mb4");

        parser.parse("CREATE DATABASE db2 CHARACTER SET latin1;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "utf8mb4");

        parser.parse("USE db2;", tables);// changes the "character_set_database" system variable ...
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "latin1");

        parser.parse("USE db1;", tables);// changes the "character_set_database" system variable ...
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "utf8mb4");
    }

    @Test
    public void shouldParseSetCharacterSetStatement() {
        parser.parse("SET character_set_server=utf8;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        parser.parse("SET CHARACTER SET utf8mb4;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf8mb4");
        assertVariable("character_set_results", "utf8mb4");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        // Set the character set to the default for the current database, or since there is none then that of the server ...
        parser.parse("SET CHARACTER SET default;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf8");
        assertVariable("character_set_results", "utf8");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        parser.parse("SET CHARSET utf16;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf16");
        assertVariable("character_set_results", "utf16");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        parser.parse("SET CHARSET default;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf8");
        assertVariable("character_set_results", "utf8");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        parser.parse("CREATE DATABASE db1 CHARACTER SET cs1;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", null); // changes when we USE a different database

        parser.parse("USE db1;", tables);// changes the "character_set_database" system variable ...
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "cs1");

        parser.parse("SET CHARSET default;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "cs1");
        assertVariable("character_set_results", "cs1");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", "cs1");
    }

    @Test
    public void shouldParseSetNamesStatement() {
        parser.parse("SET character_set_server=utf8;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_connection", null);
        assertVariable("character_set_database", null);

        parser.parse("SET NAMES utf8mb4 COLLATE junk;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf8mb4");
        assertVariable("character_set_results", "utf8mb4");
        assertVariable("character_set_connection", "utf8mb4");
        assertVariable("character_set_database", null);

        // Set the character set to the default for the current database, or since there is none then that of the server ...
        parser.parse("SET NAMES default;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf8");
        assertVariable("character_set_results", "utf8");
        assertVariable("character_set_connection", "utf8");
        assertVariable("character_set_database", null);

        parser.parse("SET NAMES utf16;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "utf16");
        assertVariable("character_set_results", "utf16");
        assertVariable("character_set_connection", "utf16");
        assertVariable("character_set_database", null);

        parser.parse("CREATE DATABASE db1 CHARACTER SET cs1;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", null); // changes when we USE a different database

        parser.parse("USE db1;", tables);// changes the "character_set_database" system variable ...
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_database", "cs1");

        parser.parse("SET NAMES default;", tables);
        assertVariable("character_set_server", "utf8");
        assertVariable("character_set_client", "cs1");
        assertVariable("character_set_results", "cs1");
        assertVariable("character_set_connection", "cs1");
        assertVariable("character_set_database", "cs1");
    }

    @Test
    public void shouldParseAlterTableStatementAddColumns() {
        String ddl = "CREATE TABLE t ( col1 VARCHAR(25) ); ";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId(null, null, "t"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("col1");
        assertThat(t.primaryKeyColumnNames()).isEmpty();
        assertColumn(t, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);
        assertThat(t.columnWithName("col1").position()).isEqualTo(1);

        ddl = "ALTER TABLE t ADD col2 VARCHAR(50) NOT NULL;";
        parser.parse(ddl, tables);
        Table t2 = tables.forTable(new TableId(null, null, "t"));
        assertThat(t2).isNotNull();
        assertThat(t2.columnNames()).containsExactly("col1", "col2");
        assertThat(t2.primaryKeyColumnNames()).isEmpty();
        assertColumn(t2, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);
        assertColumn(t2, "col2", "VARCHAR", Types.VARCHAR, 50, -1, false, false, false);
        assertThat(t2.columnWithName("col1").position()).isEqualTo(1);
        assertThat(t2.columnWithName("col2").position()).isEqualTo(2);

        ddl = "ALTER TABLE t ADD col3 FLOAT NOT NULL AFTER col1;";
        parser.parse(ddl, tables);
        Table t3 = tables.forTable(new TableId(null, null, "t"));
        assertThat(t3).isNotNull();
        assertThat(t3.columnNames()).containsExactly("col1", "col3", "col2");
        assertThat(t3.primaryKeyColumnNames()).isEmpty();
        assertColumn(t3, "col1", "VARCHAR", Types.VARCHAR, 25, -1, true, false, false);
        assertColumn(t3, "col3", "FLOAT", Types.FLOAT, -1, -1, false, false, false);
        assertColumn(t3, "col2", "VARCHAR", Types.VARCHAR, 50, -1, false, false, false);
        assertThat(t3.columnWithName("col1").position()).isEqualTo(1);
        assertThat(t3.columnWithName("col3").position()).isEqualTo(2);
        assertThat(t3.columnWithName("col2").position()).isEqualTo(3);
    }

    @Test
    public void shouldParseCreateTableWithEnumAndSetColumns() {
        String ddl = "CREATE TABLE t ( c1 ENUM('a','b','c') NOT NULL, c2 SET('a','b','c') NULL);";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(1);
        Table t = tables.forTable(new TableId(null, null, "t"));
        assertThat(t).isNotNull();
        assertThat(t.columnNames()).containsExactly("c1", "c2");
        assertThat(t.primaryKeyColumnNames()).isEmpty();
        assertColumn(t, "c1", "ENUM", Types.CHAR, 1, -1, false, false, false);
        assertColumn(t, "c2", "SET", Types.CHAR, 5, -1, true, false, false);
        assertThat(t.columnWithName("c1").position()).isEqualTo(1);
        assertThat(t.columnWithName("c2").position()).isEqualTo(2);
    }

    @Test
    public void shouldParseGrantStatement() {
        String ddl = "GRANT ALL PRIVILEGES ON `mysql`.* TO 'mysqluser'@'%'";
        parser.parse(ddl, tables);
        assertThat(tables.size()).isEqualTo(0); // no tables
        assertThat(listener.total()).isEqualTo(0);
    }

    @Test
    public void shouldParseSetOfOneVariableStatementWithoutTerminator() {
        String ddl = "set character_set_client=utf8";
        parser.parse(ddl, tables);
        assertVariable("character_set_client", "utf8");
    }

    @Test
    public void shouldParseSetOfOneVariableStatementWithTerminator() {
        String ddl = "set character_set_client = utf8;";
        parser.parse(ddl, tables);
        assertVariable("character_set_client", "utf8");
    }

    @Test
    public void shouldParseSetOfSameVariableWithDifferentScope() {
        String ddl = "SET GLOBAL sort_buffer_size=1000000, SESSION sort_buffer_size=1000000";
        parser.parse(ddl, tables);
        assertGlobalVariable("sort_buffer_size", "1000000");
        assertSessionVariable("sort_buffer_size", "1000000");
    }

    @Test
    public void shouldParseSetOfMultipleVariablesWithInferredScope() {
        String ddl = "SET GLOBAL v1=1, v2=2";
        parser.parse(ddl, tables);
        assertGlobalVariable("v1", "1");
        assertGlobalVariable("v2", "2");
        assertSessionVariable("v2", null);
    }

    @Test
    public void shouldParseSetOfGlobalVariable() {
        String ddl = "SET GLOBAL v1=1; SET @@global.v2=2";
        parser.parse(ddl, tables);
        assertGlobalVariable("v1", "1");
        assertGlobalVariable("v2", "2");
        assertSessionVariable("v1", null);
        assertSessionVariable("v2", null);
    }

    @Test
    public void shouldParseSetOfLocalVariable() {
        String ddl = "SET LOCAL v1=1; SET @@local.v2=2";
        parser.parse(ddl, tables);
        assertLocalVariable("v1", "1");
        assertLocalVariable("v2", "2");
        assertSessionVariable("v1", "1");
        assertSessionVariable("v2", "2");
        assertGlobalVariable("v1", null);
        assertGlobalVariable("v2", null);
    }

    @Test
    public void shouldParseSetOfSessionVariable() {
        String ddl = "SET SESSION v1=1; SET @@session.v2=2";
        parser.parse(ddl, tables);
        assertLocalVariable("v1", "1");
        assertLocalVariable("v2", "2");
        assertSessionVariable("v1", "1");
        assertSessionVariable("v2", "2");
        assertGlobalVariable("v1", null);
        assertGlobalVariable("v2", null);
    }

    @Test
    public void shouldParseButNotSetUserVariableWithHyphenDelimiter() {
        String ddl = "SET @a-b-c-d:=1";
        parser.parse(ddl, tables);
        assertLocalVariable("a-b-c-d", null);
        assertSessionVariable("a-b-c-d", null);
        assertGlobalVariable("a-b-c-d", null);
    }

    @Test
    public void shouldParseVariableWithHyphenDelimiter() {
        String ddl = "SET a-b-c-d=1";
        parser.parse(ddl, tables);
        assertSessionVariable("a-b-c-d", "1");
    }

    @Test
    public void shouldParseStatementsWithQuotedIdentifiers() {
        parser.parse(readFile("ddl/mysql-quoted.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(4);
        assertThat(listener.total()).isEqualTo(10);
        assertThat(tables.forTable("connector_test_ro", null, "products")).isNotNull();
        assertThat(tables.forTable("connector_test_ro", null, "products_on_hand")).isNotNull();
        assertThat(tables.forTable("connector_test_ro", null, "customers")).isNotNull();
        assertThat(tables.forTable("connector_test_ro", null, "orders")).isNotNull();
    }

    @Test
    public void shouldParseIntegrationTestSchema() {
        parser.parse(readFile("ddl/mysql-integration.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(10);
        assertThat(listener.total()).isEqualTo(17);
    }

    @Test
    public void shouldParseCreateStatements() {
        parser.parse(readFile("ddl/mysql-test-create.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(57);
        assertThat(listener.total()).isEqualTo(144);
    }

    @Test
    public void shouldParseStatementForDbz106() {
        parser.parse(readFile("ddl/mysql-dbz-106.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(1);
        assertThat(listener.total()).isEqualTo(1);
    }

    @Test
    public void shouldParseStatementForDbz123() {
        parser.parse(readFile("ddl/mysql-dbz-123.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(1);
        assertThat(listener.total()).isEqualTo(1);
    }

    @Test
    public void shouldParseTestStatements() {
        parser.parse(readFile("ddl/mysql-test-statements.ddl"), tables);
        Testing.print(tables);
        assertThat(tables.size()).isEqualTo(6);
        assertThat(listener.total()).isEqualTo(62);
        listener.forEach(this::printEvent);
    }

    @Test
    public void shouldParseSomeLinesFromCreateStatements() {
        parser.parse(readLines(189, "ddl/mysql-test-create.ddl"), tables);
        assertThat(tables.size()).isEqualTo(39);
        assertThat(listener.total()).isEqualTo(120);
    }

    @Test
    public void shouldParseMySql56InitializationStatements() {
        parser.parse(readLines(1, "ddl/mysql-test-init-5.6.ddl"), tables);
        assertThat(tables.size()).isEqualTo(85); // 1 table
        assertThat(listener.total()).isEqualTo(118);
        listener.forEach(this::printEvent);
    }

    @Test
    public void shouldParseMySql57InitializationStatements() {
        parser.parse(readLines(1, "ddl/mysql-test-init-5.7.ddl"), tables);
        assertThat(tables.size()).isEqualTo(123);
        assertThat(listener.total()).isEqualTo(132);
        listener.forEach(this::printEvent);
    }

    @Test
    public void shouldParseTicketMonsterLiquibaseStatements() {
        parser.parse(readLines(1, "ddl/mysql-ticketmonster-liquibase.ddl"), tables);
        assertThat(tables.size()).isEqualTo(7);
        assertThat(listener.total()).isEqualTo(16);
        listener.forEach(this::printEvent);
    }

    @Test
    public void shouldParseEnumOptions() {
        assertParseEnumAndSetOptions("ENUM('a','b','c')", "a,b,c");
        assertParseEnumAndSetOptions("ENUM('a')", "a");
        assertParseEnumAndSetOptions("ENUM()", "");
        assertParseEnumAndSetOptions("ENUM ('a','b','c') CHARACTER SET", "a,b,c");
        assertParseEnumAndSetOptions("ENUM ('a') CHARACTER SET", "a");
        assertParseEnumAndSetOptions("ENUM () CHARACTER SET", "");
    }

    @Test
    public void shouldParseSetOptions() {
        assertParseEnumAndSetOptions("SET('a','b','c')", "a,b,c");
        assertParseEnumAndSetOptions("SET('a')", "a");
        assertParseEnumAndSetOptions("SET()", "");
        assertParseEnumAndSetOptions("SET ('a','b','c') CHARACTER SET", "a,b,c");
        assertParseEnumAndSetOptions("SET ('a') CHARACTER SET", "a");
        assertParseEnumAndSetOptions("SET () CHARACTER SET", "");
    }

    protected void assertParseEnumAndSetOptions(String typeExpression, String optionString) {
        List<String> options = MySqlDdlParser.parseSetAndEnumOptions(typeExpression);
        String commaSeperatedOptions = Strings.join(",", options);
        assertThat(optionString).isEqualTo(commaSeperatedOptions);
    }

    protected void assertVariable(String name, String expectedValue) {
        String actualValue = parser.systemVariables().getVariable(name);
        if (expectedValue == null) {
            assertThat(actualValue).isNull();
        } else {
            assertThat(actualValue).isEqualToIgnoringCase(expectedValue);
        }
    }

    protected void assertVariable(MySqlSystemVariables.Scope scope, String name, String expectedValue) {
        String actualValue = parser.systemVariables().getVariable(name, scope);
        if (expectedValue == null) {
            assertThat(actualValue).isNull();
        } else {
            assertThat(actualValue).isEqualToIgnoringCase(expectedValue);
        }
    }

    protected void assertGlobalVariable(String name, String expectedValue) {
        assertVariable(MySqlSystemVariables.Scope.GLOBAL, name, expectedValue);
    }

    protected void assertSessionVariable(String name, String expectedValue) {
        assertVariable(MySqlSystemVariables.Scope.SESSION, name, expectedValue);
    }

    protected void assertLocalVariable(String name, String expectedValue) {
        assertVariable(MySqlSystemVariables.Scope.LOCAL, name, expectedValue);
    }

    protected void printEvent(Event event) {
        Testing.print(event);
    }

    protected String readFile(String classpathResource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(classpathResource);) {
            assertThat(stream).isNotNull();
            return IoUtil.read(stream);
        } catch (IOException e) {
            fail("Unable to read '" + classpathResource + "'");
        }
        assert false : "should never get here";
        return null;
    }

    /**
     * Reads the lines starting with a given line number from the specified file on the classpath. Any lines preceding the
     * given line number will be included as empty lines, meaning the line numbers will match the input file.
     * 
     * @param startingLineNumber the 1-based number designating the first line to be included
     * @param classpathResource the path to the file on the classpath
     * @return the string containing the subset of the file contents; never null but possibly empty
     */
    protected String readLines(int startingLineNumber, String classpathResource) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(classpathResource);) {
            assertThat(stream).isNotNull();
            StringBuilder sb = new StringBuilder();
            AtomicInteger counter = new AtomicInteger();
            IoUtil.readLines(stream, line -> {
                if (counter.incrementAndGet() >= startingLineNumber) sb.append(line);
                sb.append(System.lineSeparator());
            });
            return sb.toString();
        } catch (IOException e) {
            fail("Unable to read '" + classpathResource + "'");
        }
        assert false : "should never get here";
        return null;
    }

    protected void assertColumn(Table table, String name, String typeName, int jdbcType, int length,
                                String charsetName, boolean optional) {
        Column column = table.columnWithName(name);
        assertThat(column.name()).isEqualTo(name);
        assertThat(column.typeName()).isEqualTo(typeName);
        assertThat(column.jdbcType()).isEqualTo(jdbcType);
        assertThat(column.length()).isEqualTo(length);
        assertThat(column.charsetName()).isEqualTo(charsetName);
        assertThat(column.scale()).isEqualTo(-1);
        assertThat(column.isOptional()).isEqualTo(optional);
        assertThat(column.isGenerated()).isFalse();
        assertThat(column.isAutoIncremented()).isFalse();
    }

    protected void assertColumn(Table table, String name, String typeName, int jdbcType, int length, int scale,
                                boolean optional, boolean generated, boolean autoIncremented) {
        Column column = table.columnWithName(name);
        assertThat(column.name()).isEqualTo(name);
        assertThat(column.typeName()).isEqualTo(typeName);
        assertThat(column.jdbcType()).isEqualTo(jdbcType);
        assertThat(column.length()).isEqualTo(length);
        assertThat(column.scale()).isEqualTo(scale);
        assertThat(column.isOptional()).isEqualTo(optional);
        assertThat(column.isGenerated()).isEqualTo(generated);
        assertThat(column.isAutoIncremented()).isEqualTo(autoIncremented);
    }

}
