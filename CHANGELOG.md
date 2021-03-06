# Change log

All notable changes are documented in this file. Release numbers follow [Semantic Versioning](http://semver.org)

## 0.3.3

October 18, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12331604)

### New features since 0.3.2

None

### Backwards-incompatible changes since 0.3.2

None

### Fixes since 0.3.2

This release includes all of the fixes from the link:release-0-3-2[0.3.2] release, and also includes the following fixes:

* MySQL connector now works with MySQL 5.5. [DBZ-115](https://issues.jboss.org/projects/DBZ/issues/DBZ-115)
* MySQL connector now handles `BIT(n)` column values. [DBZ-123](https://issues.jboss.org/projects/DBZ/issues/DBZ-123)
* MySQL connector supports failing over based on subset of GTIDs. [DBZ-129](https://issues.jboss.org/projects/DBZ/issues/DBZ-129)
* MySQL connector processes GTIDs with line feeds and carriage returns. [DBZ-135](https://issues.jboss.org/projects/DBZ/issues/DBZ-135)
* MySQL connector has improved output of GTIDs and status when reading the binary log. [DBZ-130](https://issues.jboss.org/projects/DBZ/issues/DBZ-130), [DBZ-131](https://issues.jboss.org/projects/DBZ/issues/DBZ-131)
* MySQL connector properly handles multi-character `ENUM` and `SET` values. [DBZ-132](https://issues.jboss.org/projects/DBZ/issues/DBZ-132)


## 0.3.2

September 26, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12331401)

### New features since 0.3.1

None

### Backwards-incompatible changes since 0.3.1

None

### Fixes since 0.3.1

This release includes all of the fixes from the link:release-0-3-1[0.3.1] release, and also includes the following fixes:

* MySQL connector now handles zero-value dates. [DBZ-114](https://issues.jboss.org/projects/DBZ/issues/DBZ-114)
* MySQL connector no longer prints out password-related configuration properties, though [KAFKA-4171](https://issues.apache.org/jira/browse/KAFKA-4171) for a similar issue with Kafka Connect. [DBZ-122](https://issues.jboss.org/projects/DBZ/issues/DBZ-122)
* MySQL connector no longer causes "Error registering AppInfo mbean" warning in Kafka Connect. [DBZ-124](https://issues.jboss.org/projects/DBZ/issues/DBZ-124)
* MySQL connector periodically outputs status when reading binlog. [DBZ-116](https://issues.jboss.org/projects/DBZ/issues/DBZ-116)
* MongoDB connector periodically outputs status when reading binlog. [DBZ-117](https://issues.jboss.org/projects/DBZ/issues/DBZ-117)
* MySQL connector correctly uses long for the `server.id` configuration property. [DBZ-118](https://issues.jboss.org/projects/DBZ/issues/DBZ-118)
* MySQL connector fails or warns when MySQL is not using row-level logging. [DBZ-128](https://issues.jboss.org/projects/DBZ/issues/DBZ-128)


## 0.3.1

August 30, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12331359)

### New features

* Added support for secure (encrypted) connections to MySQL. [DBZ-99](https://issues.jboss.org/projects/DBZ/issues/DBZ-99)

### Backwards-incompatible changes since 0.3.0

None

### Fixes since 0.3.0

This release includes all of the fixes from the link:release-0-2-4[0.2.4] release, and also includes the following fixes:

* MySQL connector now properly decodes string values from the binlog based upon the column's character set encoding as read by the DDL statement. Upon upgrade and restart, the connector will re-read the recorded database history and now associate the columns with their the character sets, and any newly processed events will use properly encoded strings values. As expected, previously generated events are never altered. Force a snapshot to regenerate events for the servers. [DBZ-102](https://issues.jboss.org/projects/DBZ/issues/DBZ-102)
* Corrected how the MySQL connector parses some DDL statements. [DBZ-106](https://issues.jboss.org/projects/DBZ/issues/DBZ-106)
* Corrected the MySQL connector to handle MySQL server GTID sets with newline characters. [DBZ-107](https://issues.jboss.org/projects/DBZ/issues/DBZ-107), [DBZ-111](https://issues.jboss.org/projects/DBZ/issues/DBZ-111)
* Corrected the MySQL connector's startup logic properly compare the MySQL SSL-related system properties to prevent overwriting them. The connector no longer fails when the system properties are the same, which can happen upon restart or starting a second MySQL connector with the same keystore. [DBZ-112](https://issues.jboss.org/projects/DBZ/issues/DBZ-112)
* Removed unused code and test case. [DBZ-108](https://issues.jboss.org/projects/DBZ/issues/DBZ-108)
* Ensure that the MySQL error code and SQLSTATE are included in exceptions reported by the connector. [DBZ-109](https://issues.jboss.org/projects/DBZ/issues/DBZ-109)


## 0.3.0

August 16, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12329661)

### New features

* New MongoDB connector supports capturing changes from a MongoDB replica set or a MongoDB sharded cluster. See the [documentation](http://debezium.io/docs/connectors/mongodb) for details. [DBZ-2](https://issues.jboss.org/projects/DBZ/issues/DBZ-2)

### Backwards-incompatible changes since 0.2.0

* Upgraded to Kafka 0.10.0.1, which means that the Debezium connectors can only be used with Kafka Connect 0.10.0.1. Check Kafka documentation for compatibility with other versions of Kafka brokers. [DBZ-62](https://issues.jboss.org/projects/DBZ/issues/DBZ-62), [DBZ-80](https://issues.jboss.org/projects/DBZ/issues/DBZ-80)
* By default the MySQL connector now represents temporal values with millisecond, microsecond, or nanosecond precision based upon the precision of the source database columns. This changes the schema name of these fields to Debezium-specific constants, and the meaning/interpretation of the literal values now depends on this schema name. To enable previous behavior that always used millisecond precision using only Kafka Connect logical types, set `time.precision.mode` connector property to `connect`. [DBZ-91](https://issues.jboss.org/projects/DBZ/issues/DBZ-91)
* Removed several methods in the `GtidSet` class inside the MySQL connector. The class was introduced in 0.2. This change will only affect applications explicitly using the class (by reusing the MySQL connector JAR), and will not affect how the MySQL connector works. _Changed in 0.2.2._ [DBZ-79](https://issues.jboss.org/projects/DBZ/issues/DBZ-79)
* The `source` field within each MySQL change event now contains the binlog position of that event (rather than the next event). Events persisted by earlier versions of the connector are unaffected. This change _may_ adversely clients that are directly using the position within the `source` field. _Changed in 0.2.2._ [DBZ-76](https://issues.jboss.org/projects/DBZ/issues/DBZ-76)
* Correted the names of the Avro-compliant Kafka Connect schemas generated by the MySQL connector for the `before` and `after` fields in its data change events. Consumers that require knowledge (by name) of the particular schemas used in 0.2 events may have trouble consuming events produced by the 0.2.1 (or later) connector. _Fixed in 0.2.1_. [DBZ-72](https://issues.jboss.org/projects/DBZ/issues/DBZ-72)

### Fixes since 0.2.0

* MySQL snapshots records DDL statements as separate events on the schema change topic. [DBZ-97](https://issues.jboss.org/browse/DBZ-97)
* MySQL connector tolerates binlog filename missing from ROTATE events in certain situations. [DBZ-95](https://issues.jboss.org/browse/DBZ-95)
* Stream result set rows when taking snapshot of MySQL databases to prevent out of memory problems with very large databases. _Fixed in 0.2.4._ [DBZ-94](https://issues.jboss.org/browse/DBZ-94)
* Add more verbose logging statements to the MySQL connector to show progress and activity. _Fixed in 0.2.4._ [DBZ-92](https://issues.jboss.org/browse/DBZ-92)
* Corrected potential error during graceful MySQL connector shutdown. _Fixed in 0.2.4._ [DBZ-103](https://issues.jboss.org/browse/DBZ-103)
* The Kafka Connect schema names used in the MySQL connector's change events are now always Avro-compatible schema names [DBZ-86](https://issues.jboss.org/projects/DBZ/issues/DBZ-86)
* Corrected parsing errors when MySQL DDL statements are generated by Liquibase. _Fixed in 0.2.3._ [DBZ-83](https://issues.jboss.org/browse/DBZ-83)
* Corrected support of MySQL `TINYINT` and `SMALLINT` types. _Fixed in 0.2.3._ [DBZ-84](https://issues.jboss.org/browse/DBZ-84), [DBZ-87](https://issues.jboss.org/browse/DBZ-87)
* Corrected support of MySQL temporal types, including `DATE`, `TIME`, and `TIMESTAMP`. _Fixed in 0.2.3._ [DBZ-85](https://issues.jboss.org/browse/DBZ-85)
* Corrected support of MySQL `ENUM` and `SET` types. [DBZ-100](https://issues.jboss.org/browse/DBZ-100)
* Corrected call to MySQL `SHOW MASTER STATUS` so that it works on pre-5.7 versions of MySQL. _Fixed in 0.2.3._ [DBZ-82](https://issues.jboss.org/browse/DBZ-82)
* Correct how the MySQL connector records offsets with multi-row MySQL events so that, even if the connector experiences a non-graceful shutdown (i.e., crash) after committing the offset of _some_ of the rows from such an event, upon restart the connector will resume with the remaining rows in that multi-row event. Previously, the connector might incorrectly restart at the next event. _Fixed in 0.2.2._ [DBZ-73](https://issues.jboss.org/projects/DBZ/issues/DBZ-73)
* Shutdown of the MySQL connector immediately after a snapshot completes (before another change event is reccorded) will now be properly marked as complete. _Fixed in 0.2.2._ [DBZ-77](https://issues.jboss.org/projects/DBZ/issues/DBZ-77)
* The MySQL connector's plugin archive now contains the MySQL JDBC driver JAR file required by the connector. _Fixed in 0.2.1._ [DBZ-71](https://issues.jboss.org/projects/DBZ/issues/DBZ-71)


## 0.2.4

August 16, 2016 - [Detailed release notes](https://issues.jboss.org/projects/DBZ/versions/12331221)

### Fixes since 0.2.3

* Stream result set rows when taking snapshot of MySQL databases to prevent out of memory problems with very large databases. [DBZ-94](https://issues.jboss.org/browse/DBZ-94)
* Add more verbose logging statements to the MySQL connector to show progress and activity during snapshots. [DBZ-92](https://issues.jboss.org/browse/DBZ-92)
* Corrected potential error during graceful MySQL connector shutdown. [DBZ-103](https://issues.jboss.org/browse/DBZ-103)


## 0.2.3

July 26, 2016 - [Detailed release notes](https://issues.jboss.org/projects/DBZ/versions/12330932)

### Backwards-incompatible changes since 0.2.2

None

### Fixes since 0.2.2

* Corrected parsing errors when MySQL DDL statements are generated by Liquibase. [DBZ-83](https://issues.jboss.org/browse/DBZ-83)
* Corrected support of MySQL `TINYINT` and `SMALLINT` types. [DBZ-84](https://issues.jboss.org/browse/DBZ-84), [DBZ-87](https://issues.jboss.org/browse/DBZ-87)
* Corrected support of MySQL temporal types, including `DATE`, `TIME`, and `TIMESTAMP`. [DBZ-85](https://issues.jboss.org/browse/DBZ-85)
* Corrected call to MySQL `SHOW MASTER STATUS` so that it works on pre-5.7 versions of MySQL. [DBZ-82](https://issues.jboss.org/browse/DBZ-82)


## 0.2.2

June 22, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/versions/12330862)

### Backwards-incompatible changes since 0.2.1

* Removed several methods in the `GtidSet` class inside the MySQL connector. The class was introduced in 0.2. This change will only affect applications explicitly using the class (by reusing the MySQL connector JAR), and will not affect how the MySQL connector works. [DBZ-79](https://issues.jboss.org/projects/DBZ/issues/DBZ-79)
* The `source` field within each MySQL change event now contains the binlog position of that event (rather than the next event). Events persisted by earlier versions of the connector are unaffected. This change _may_ adversely clients that are directly using the position within the `source` field. [DBZ-76](https://issues.jboss.org/projects/DBZ/issues/DBZ-76)

### Fixes since 0.2.1

* Correct how the MySQL connector records offsets with multi-row MySQL events so that, even if the connector experiences a non-graceful shutdown (i.e., crash) after committing the offset of _some_ of the rows from such an event, upon restart the connector will resume with the remaining rows in that multi-row event. Previously, the connector might incorrectly restart at the next event. [DBZ-73](https://issues.jboss.org/projects/DBZ/issues/DBZ-73)
* Shutdown of the MySQL connector immediately after a snapshot completes (before another change event is reccorded) will now be properly marked as complete. [DBZ-77](https://issues.jboss.org/projects/DBZ/issues/DBZ-77)



## 0.2.1

June 10, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12330752)

### Backwards-incompatible changes since 0.2.0

* Correted the names of the Avro-compliant Kafka Connect schemas generated by the MySQL connector for the `before` and `after` fields in its data change events. Consumers that require knowledge (by name) of the particular schemas used in 0.2 events may have trouble consuming events produced by the 0.2.1 (or later) connector. ([DBZ-72](https://issues.jboss.org/projects/DBZ/issues/DBZ-72))

### Fixes since 0.2.0

* The MySQL connector's plugin archive now contains the MySQL JDBC driver JAR file required by the connector.([DBZ-71](https://issues.jboss.org/projects/DBZ/issues/DBZ-71))

## 0.2.0

June 8, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12329465)

### New features

* MySQL connector supports *high availability* MySQL cluster topologies. See the [documentation](http://debezium.io/docs/connectors/mysql) for details. ([DBZ-37](https://issues.jboss.org/projects/DBZ/issues/DBZ-37))
* MySQL connector now by default starts by performing a *consistent snapshot* of the schema and contents of the upstream MySQL databases in its current state. See the [documentation](http://debezium.io/docs/connectors/mysql#snapshots) for details about how this works and how it impacts other database clients. ([DBZ-31](https://issues.jboss.org/projects/DBZ/issues/DBZ-31))
* MySQL connector can be configured to *exclude*, *truncate*, or *mask* specific columns in events. ([DBZ-29](https://issues.jboss.org/projects/DBZ/issues/DBZ-29))
* MySQL connector events can be serialized using the [Confluent Avro converter](http://docs.confluent.io/3.0.0/avro.html) or the JSON converter. Previously, only the JSON converter could be used. ([DBZ-29](https://issues.jboss.org/projects/DBZ/issues/DBZ-29), [DBZ-63](https://issues.jboss.org/projects/DBZ/issues/DBZ-63), [DBZ-64](https://issues.jboss.org/projects/DBZ/issues/DBZ-64))

### Backwards-incompatible changes since 0.1

* Completely redesigned the structure of event messages produced by MySQL connector and stored in Kafka topics. Events now contain an _envelope_ structure with information about the source event, the kind of operation (create/insert, update, delete, read), the time that Debezium processed the event, and the state of the row before and/or after the event. The messages written to each topic have a distinct Avro-compliant Kafka Connect schema that reflects the structure of the source table, which may vary over time independently from the schemas of all other topics. See the [documentation](http://debezium.io/docs/connectors/mysql#events) for details. This envelope structure will likely be used by future connectors. ([DBZ-50](https://issues.jboss.org/projects/DBZ/issues/DBZ-50), [DBZ-52](https://issues.jboss.org/projects/DBZ/issues/DBZ-52), [DBZ-45](https://issues.jboss.org/projects/DBZ/issues/DBZ-45), [DBZ-60](https://issues.jboss.org/projects/DBZ/issues/DBZ-60))
* MySQL connector handles deletion of a row by recording a delete event message whose value contains the state of the removed row (and other metadata), followed by a _tombstone event_ message with a null value to signal *Kafka's log compaction* that all prior messages with the same key can be garbage collected. See the [documentation](http://debezium.io/docs/connectors/mysql#events) for details. ([DBZ-44](https://issues.jboss.org/projects/DBZ/issues/DBZ-44))
* Changed the format of events that the MySQL connector writes to its schema change topic, through which consumers can access events with the DDL statements applied to the database(s). The format change makes it possible for consumers to correlate these events with the data change events. ([DBZ-43](https://issues.jboss.org/projects/DBZ/issues/DBZ-43), [DBZ-55](https://issues.jboss.org/projects/DBZ/issues/DBZ-55))

### Changes since 0.1

* DDL parsing framework identifies table affected by statements via a new listener callback. ([DBZ-38](https://issues.jboss.org/projects/DBZ/issues/DBZ-38))
* The `database.binlog` configuration property was required in version 0.1 of the MySQL connector, but in 0.2 it is no longer used because of the new snapshot feature. If provided, it will be quietly ignored. ([DBZ-31](https://issues.jboss.org/projects/DBZ/issues/DBZ-31))

### Bug fixes since 0.1

* MySQL connector now properly parses `COMMIT` statements, the `REFERENCES` clauses of `CREATE TABLE` statements, and statements with `CHARSET` shorthand of `CHARACTER SET`. ([DBZ-48](https://issues.jboss.org/projects/DBZ/issues/DBZ-48), [DBZ-49](https://issues.jboss.org/projects/DBZ/issues/DBZ-49), [DBZ-57](https://issues.jboss.org/projects/DBZ/issues/DBZ-57))
* MySQL connector properly handles binary values that are hexadecimal strings ([DBZ-61](https://issues.jboss.org/projects/DBZ/issues/DBZ-61))

## 0.1

March 17, 2016 - [Detailed release notes](https://issues.jboss.org/browse/DBZ/fixforversion/12329464)

### New features

* MySQL connector for ingesting change events from MySQL databases. ([DBZ-1](https://issues.jboss.org/projects/DBZ/issues/DBZ-1))
* Kafka Connect plugin archive for MySQL connector. ([DBZ-17](https://issues.jboss.org/projects/DBZ/issues/DBZ-17))
* Simple DDL parsing framework that can be extended and used by various connectors. ([DBZ-1](https://issues.jboss.org/projects/DBZ/issues/DBZ-1))
* Framework for embedding a single Kafka Connect connector inside an application. ([DBZ-8](https://issues.jboss.org/projects/DBZ/issues/DBZ-8))
