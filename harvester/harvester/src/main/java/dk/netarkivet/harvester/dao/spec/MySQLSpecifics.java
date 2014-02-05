/* File:        $Id: MySQLSpecifics.java 2804 2013-11-01 16:06:07Z svc $
 * Revision:    $Revision: 2804 $
 * Author:      $Author: svc $
 * Date:        $Date: 2013-11-01 17:06:07 +0100 (Fri, 01 Nov 2013) $
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
 package dk.netarkivet.harvester.dao.spec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.harvester.datamodel.Constants;

/**
 * MySQL-specific implementation of DB methods.
 */
public class MySQLSpecifics extends DBSpecifics {
	
    /** The log. */
    Log log = LogFactory.getLog(MySQLSpecifics.class);
    
    /**
     * Get an instance of the MySQL specifics class.
     * @return Instance of the MySQL specifics class.
     */
    public static DBSpecifics getInstance() {
        return new MySQLSpecifics();
    }

    /** Get a temporary table for short-time use.  The table should be
     * disposed of with dropTemporaryTable.  The table has two columns
     * domain_name varchar(Constants.MAX_NAME_SIZE)
     * config_name varchar(Constants.MAX_NAME_SIZE)
     * 
     * @return The name of the created table
     */
    public String getJobConfigsTmpTable() {
        getDao().executeUpdate(
        		"CREATE TEMPORARY TABLE jobconfignames"
        		+ " (domain_name varchar(" + Constants.MAX_NAME_SIZE + ")"
        		+ ", config_name varchar(" + Constants.MAX_NAME_SIZE + ")"
        		+ ")",
        		false);
        return "jobconfignames";
    }

    /**
     * Dispose of a temporary table created with getTemporaryTable.  This can be
     * expected to be called from within a finally clause, so it mustn't throw
     * exceptions.
     *
     * @param tableName The name of the temporary table
     */
    public void dropJobConfigsTmpTable(String tableName) {
        ArgumentNotValid.checkNotNullOrEmpty(tableName, "String tableName");
        getDao().executeUpdate("DROP TEMPORARY TABLE " +  tableName, false);
    }

    /**
     * Get the name of the JDBC driver class that handles interfacing
     * to this server.
     *
     * @return The name of a JDBC driver class
     */
    public String getDriverClassName() {
        return "com.mysql.jdbc.Driver";
    }

    /** Migrates the 'jobs' table from version 3 to version 4
     * consisting of a change of the field forcemaxbytes from int to bigint
     * and setting its default to -1.
     * Furthermore the default value for field num_configs is set to 0.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv3tov4() {
        String[] sqlStatements = {
            "ALTER TABLE jobs CHANGE COLUMN forcemaxbytes forcemaxbytes"
            + " bigint not null default -1",
            "ALTER TABLE jobs CHANGE COLUMN num_configs num_configs"
            + " int not null default 0"
        };
        getDao().updateTable("jobs", 4, sqlStatements);
    }

    /** Migrates the 'jobs' table from version 4 to version 5
     * consisting of adding new fields 'resubmitted_as_job' and 'submittedDate'.
     * @throws IOFailure in case of problems in interacting with the database
     */
    protected synchronized void migrateJobsv4tov5() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN submitteddate datetime "
                    + "AFTER enddate",
                "ALTER TABLE jobs ADD COLUMN resubmitted_as_job bigint"
            };
        getDao().updateTable("jobs", 5, sqlStatements);
    }

    /** Migrates the 'configurations' table from version 3 to version 4.
     * This consists of altering the default value of field 'maxbytes' to -1.
     */
    protected synchronized void migrateConfigurationsv3ov4() {
     // Update configurations table to version 4
        String[] sqlStatements = {
                "ALTER TABLE configurations ALTER maxbytes SET DEFAULT -1"
            };
        getDao().updateTable("configurations", 4, sqlStatements);
    }

    /** Migrates the 'fullharvests' table from version 2 to version 3.
     * This consists of altering the default value of field 'maxbytes' to -1
     */
    protected synchronized void migrateFullharvestsv2tov3() {
        // Update fullharvests table to version 3
        String[] sqlStatements = {
                "ALTER TABLE fullharvests ALTER maxbytes SET DEFAULT -1"
        };
        getDao().updateTable("fullharvests", 3, sqlStatements);
    }

    /** Creates the initial (version 1) of table 'global_crawler_trap_lists'. */
    protected void createGlobalCrawlerTrapLists() {
        String createStatement = "CREATE TABLE global_crawler_trap_lists(\n"
                                 + "  global_crawler_trap_list_id INT NOT NULL "
                                 + "AUTO_INCREMENT PRIMARY KEY,\n"
                                 + "  name VARCHAR(300) NOT NULL UNIQUE, "
                                 + "  description VARCHAR(20000), "
                                 + "  isActive INT NOT NULL )";
        getDao().updateTable("global_crawler_trap_lists", 1, createStatement);
    }

    /** Creates the initial (version 1) of
     * table 'global_crawler_trap_expressions'. */
    protected void createGlobalCrawlerTrapExpressions() {
        String createStatement = "CREATE TABLE global_crawler_trap_expressions("
                                 + "    id bigint not null AUTO_INCREMENT "
                                 + "primary key,"
                                 + "    crawler_trap_list_id INT NOT NULL, "
                                 + "    trap_expression VARCHAR(1000) )";
        getDao().updateTable("global_crawler_trap_expressions", 1,
                              createStatement);
    }

    @Override
    public boolean supportsClob() {
        return true;
    }

    @Override
    public String getOrderByLimitAndOffsetSubClause(long limit, long offset) {
        return "LIMIT " + offset + ", " + limit;
    }

    @Override
    public void createFrontierReportMonitorTable() {
        String createStatement = "CREATE TABLE frontierReportMonitor ("
             + "jobId bigint NOT NULL,"
             + "filterId varchar(200) NOT NULL,"
             + "tstamp timestamp NOT NULL,"
             + "domainName varchar(300) NOT NULL,"
             + "currentSize bigint NOT NULL,"
             + "totalEnqueues bigint NOT NULL,"
             + "sessionBalance bigint NOT NULL,"
             + "lastCost numeric NOT NULL,"
             + "averageCost numeric NOT NULL,"
             + "lastDequeueTime varchar(100) NOT NULL,"
             + "wakeTime varchar(100) NOT NULL,"
             + "totalSpend bigint NOT NULL,"
             + "totalBudget bigint NOT NULL,"
             + "errorCount bigint NOT NULL,"
             + "lastPeekUri varchar(1000) NOT NULL,"
             + "lastQueuedUri varchar(1000) NOT NULL,"
             // NB see http://bugs.mysql.com/bug.php?id=6604 about index key length.
             + "UNIQUE (jobId, filterId(100), domainName(100))"
             + ")";
        getDao().updateTable("frontierreportmonitor", 1, createStatement);

    }

    @Override
    public void createRunningJobsHistoryTable() {
        String createStatement = "CREATE TABLE runningJobsHistory ("
             + "jobId bigint NOT NULL, "
             + "harvestName varchar(300) NOT NULL,"
             + "hostUrl varchar(300) NOT NULL,"
             + "progress numeric NOT NULL,"
             + "queuedFilesCount bigint NOT NULL,"
             + "totalQueuesCount bigint NOT NULL,"
             + "activeQueuesCount bigint NOT NULL,"
             + "exhaustedQueuesCount bigint NOT NULL,"
             + "elapsedSeconds bigint NOT NULL,"
             + "alertsCount bigint NOT NULL,"
             + "downloadedFilesCount bigint NOT NULL,"
             + "currentProcessedKBPerSec int NOT NULL,"
             + "processedKBPerSec int NOT NULL,"
             + "currentProcessedDocsPerSec numeric NOT NULL,"
             + "processedDocsPerSec numeric NOT NULL,"
             + "activeToeCount integer NOT NULL,"
             + "status integer NOT NULL,"
             + "tstamp timestamp NOT NULL, "
             + "PRIMARY KEY (jobId, harvestName, elapsedSeconds, tstamp)"
             + ")";
        getDao().updateTable("runningjobshistory", 1, createStatement);

        getDao().executeUpdates(
                "CREATE INDEX runningJobsHistoryCrawlJobId on runningJobsHistory (jobId)",
                "CREATE INDEX runningJobsHistoryCrawlTime on runningJobsHistory (elapsedSeconds)",
                "CREATE INDEX runningJobsHistoryHarvestName on runningJobsHistory (harvestName)",
                "GRANT SELECT,INSERT,UPDATE,DELETE ON TABLE runningJobsHistory TO netarchivesuite"
        		);
    }

    @Override
    public void createRunningJobsMonitorTable() {
        String createStatement = "CREATE TABLE runningJobsMonitor ("
             + "jobId bigint NOT NULL,"
             + "harvestName varchar(300) NOT NULL,"
             + "hostUrl varchar(300) NOT NULL,"
             + "progress numeric NOT NULL,"
             + "queuedFilesCount bigint NOT NULL,"
             + "totalQueuesCount bigint NOT NULL,"
             + "activeQueuesCount bigint NOT NULL,"
             + "exhaustedQueuesCount bigint NOT NULL,"
             + "elapsedSeconds bigint NOT NULL,"
             + "alertsCount bigint NOT NULL,"
             + "downloadedFilesCount bigint NOT NULL,"
             + "currentProcessedKBPerSec integer NOT NULL,"
             + "processedKBPerSec integer NOT NULL,"
             + "currentProcessedDocsPerSec numeric NOT NULL,"
             + "processedDocsPerSec numeric NOT NULL,"
             + "activeToeCount integer NOT NULL,"
             + "status integer NOT NULL,"
             + "tstamp timestamp NOT NULL,"
             + "PRIMARY KEY (jobId, harvestName)"
             + ")";
        getDao().updateTable("runningjobsmonitor", 1, createStatement);

        getDao().executeUpdates(
                    "CREATE INDEX runningJobsMonitorJobId on runningJobsMonitor (jobId)",
                    "CREATE INDEX runningJobsMonitorHarvestName on runningJobsMonitor (harvestName)"
        		);
    }

    // Below DB changes introduced with development release 3.15
    // with changes to tables 'runningjobshistory', 'runningjobsmonitor',
    // 'configurations', 'fullharvests', and 'jobs'.

    /**
     * Migrates the 'runningjobshistory' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    @Override
    protected void migrateRunningJobsHistoryTableV1ToV2() {
        String[] sqlStatements = {
                "ALTER TABLE runningJobsHistory "
                + "ADD COLUMN retiredQueuesCount bigint not null"
        };
        getDao().updateTable("runningjobshistory", 2, sqlStatements);
    }

    /**
     * Migrates the 'runningjobsmonitor' table from version 1 to version 2. This
     * consists of adding the new column 'retiredQueuesCount'.
     */
    @Override
    protected void migrateRunningJobsMonitorTableV1ToV2() {
        String[] sqlStatements = {
                "ALTER TABLE runningJobsMonitor "
                + "ADD COLUMN retiredQueuesCount bigint not null"
        };
        getDao().updateTable("runningjobsmonitor", 2, sqlStatements);
    }


    @Override
    protected void migrateDomainsv2tov3() {
        String[] sqlStatements = {"ALTER TABLE domains MODIFY crawlertraps LONGTEXT "};
        getDao().updateTable("domains", 3, sqlStatements);
    }

    @Override
    protected void migrateConfigurationsv4tov5() {
     // Update configurations table to version 5
        String[] sqlStatements
            = {"ALTER TABLE configurations MODIFY maxobjects bigint" };
        getDao().updateTable("configurations", 5, sqlStatements);
    }

    @Override
    protected void migrateFullharvestsv3tov4() {
        // Update fullharvests table to version 4
        String[] sqlStatements
            = {"ALTER TABLE fullharvests ADD COLUMN maxjobrunningtime bigint NOT NULL DEFAULT 0"};
        getDao().updateTable("fullharvests", 4, sqlStatements);

    }

    @Override
    protected void migrateJobsv5tov6() {
     // Update jobs table to version 6
        String[] sqlStatements
            = {"ALTER TABLE jobs ADD COLUMN forcemaxrunningtime bigint NOT NULL DEFAULT 0 AFTER forcemaxcount"};
        getDao().updateTable("jobs", 6, sqlStatements);

    }
    
    @Override
    protected void migrateFullharvestsv4tov5() {
        // Update fullharvests table to version 4
        String[] sqlStatements
            = {"ALTER TABLE fullharvests ADD COLUMN isindexready int NOT NULL DEFAULT 0"};
        getDao().updateTable("fullharvests", 5, sqlStatements);
    }

    @Override
    protected void createExtendedFieldTypeTable() {
        String[] statements = new String[3];
        statements[0] = "" + "CREATE TABLE extendedfieldtype " + "  ( "
                + "     extendedfieldtype_id BIGINT NOT NULL PRIMARY KEY, "
                + "     name             VARCHAR(50) NOT NULL " + "  )";

        statements[1] =
            "INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )"
            + " VALUES ( 1, 'domains')";
        statements[2] = 
            "INSERT INTO extendedfieldtype ( extendedfieldtype_id, name )"
            + " VALUES ( 2, 'harvestdefinitions')";

        getDao().updateTable("extendedfieldtype", 1, statements);
    }

    
    @Override
    protected void createExtendedFieldTable() {
        String createStatement = "" + "CREATE TABLE extendedfield " + "  ( "
                + "     extendedfield_id BIGINT NOT NULL PRIMARY KEY, "
                + "     extendedfieldtype_id BIGINT NOT NULL, "
                + "     name             VARCHAR(50) NOT NULL, "
                + "     format           VARCHAR(50) NOT NULL, "
                + "     defaultvalue     VARCHAR(50) NOT NULL, "
                + "     options          VARCHAR(50) NOT NULL, "
                + "     datatype         INT NOT NULL, "
                + "     mandatory        INT NOT NULL, "
                + "     sequencenr       INT " + "  )";

        getDao().updateTable("extendedfield", 1, createStatement);
    }

    @Override
    protected void createExtendedFieldValueTable() {
        String createStatement = "" + "CREATE TABLE extendedfieldvalue "
                + "  ( "
                + "     extendedfieldvalue_id BIGINT NOT NULL PRIMARY KEY, "
                + "     extendedfield_id      BIGINT NOT NULL, "
                + "     instance_id           BIGINT NOT NULL, "
                + "     content               VARCHAR(100) NOT NULL " + "  )";

        getDao().updateTable("extendedfieldvalue", 1,
                createStatement);
    }
    @Override
    protected synchronized void migrateJobsv6tov7() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN continuationof BIGINT DEFAULT NULL"
        };
        getDao().updateTable("jobs", 7, sqlStatements);
    }
    
    @Override
    protected void migrateJobsv7tov8() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN creationdate TIMESTAMP"
        };
        getDao().updateTable("jobs", 8, sqlStatements);
    }

    @Override
    protected void migrateJobsv8tov9() {
        String[] sqlStatements = {
        "ALTER TABLE jobs ADD COLUMN harvestname_prefix VARCHAR(100)"};
        getDao().updateTable("jobs", 9, sqlStatements);
    }
    
    @Override
    protected void migrateHarvestdefinitionsv2tov3() {
        String[] sqlStatements = {
                "ALTER TABLE harvestdefinitions ADD COLUMN audience VARCHAR(100) DEFAULT NULL"
        };
        getDao().updateTable("harvestdefinitions", 3, sqlStatements);
    }
    
    @Override
    protected void migrateHarvestdefinitionsv3tov4() {
        String[] sqlStatements = {
                "ALTER TABLE harvestdefinitions ADD COLUMN channel_id BIGINT DEFAULT NULL"
        };
        getDao().updateTable("harvestdefinitions", 4, sqlStatements);
    }
    
    @Override
    protected void migrateJobsv9tov10() {
        String[] sqlStatements = {
                "ALTER TABLE jobs ADD COLUMN channel VARCHAR(300) DEFAULT NULL",
                "ALTER TABLE jobs ADD COLUMN snapshot BOOL",
        };
        getDao().updateTable("jobs", 10, sqlStatements);   
    }

    @Override
    protected void createHarvestChannelTable() {
        String createStatement = "CREATE TABLE harvestchannel ("
                + "id BIGINT NOT NULL PRIMARY KEY, "
                + "name VARCHAR(300) NOT NULL UNIQUE,"
                + "snapshot BOOL NOT NULL,"
                + "isdefault BOOL NOT NULL,"
                + "comments VARCHAR(30000)"
                + ")";
        getDao().updateTable("harvestchannel", 1, createStatement);
    }
    
}