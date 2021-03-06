/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

/**
 * Contains test information about all harvestdefinition test data.
 */
public class TestInfo {

	public static final File DATA_DIR = new File("tests/dk/netarkivet/harvester/harvesting/distribute/data/");
    public static ChannelID SERVER_ID = Channels.getThisReposClient();
    public static final File SERVER_DIR = new File(TestInfo.DATA_DIR, "server");
    public static final String DUMMY_SERVER_ID = "hc_test_dummy_server";
    public static final MetadataEntry sampleEntry = new MetadataEntry("metadata://netarkivet.dk", "text/plain",
            "THIS IS SOME METADATA");
    public static final List<MetadataEntry> emptyMetadata = new ArrayList<MetadataEntry>();
    public static final List<MetadataEntry> oneMetadata = new ArrayList<MetadataEntry>();
    public static final String prefix = "ID";
    public static final String suffix = "X";

    /**
     * The properties-file containing properties for logging in unit-tests.
     */
    static File WORKING_DIR = new File(TestInfo.DATA_DIR, "working");
    static File ORIGINALS_DIR = new File(TestInfo.DATA_DIR, "originals");
    static final File ARCHIVE_DIR = new File(TestInfo.WORKING_DIR, "bitarchive1");

    private static final File LEFTOVER_JOB_DIR_1 = new File(TestInfo.WORKING_DIR, "testserverdir1");
    static final File LEFTOVER_CRAWLDIR_1 = new File(TestInfo.LEFTOVER_JOB_DIR_1, "crawldir");
    static final int FILES_IN_LEFTOVER_JOB_DIR_1 = 1;
    private static final File LEFTOVER_JOB_DIR_2 = new File(TestInfo.WORKING_DIR, "testserverdir2");
    static final File LEFTOVER_CRAWLDIR_2 = new File(TestInfo.LEFTOVER_JOB_DIR_2, "crawldir");
    static final String[] FILES_IN_LEFTOVER_JOB_DIR_2 = {
            "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc",
            "42-117-20051212141240-00001-sb-test-har-001.statsbiblioteket.dk.arc",
            "42-117-20051212141242-00002-sb-test-har-001.statsbiblioteket.dk.arc"};
    static final String LEFTOVER_JOB_DIR_2_SOME_FILE_PATTERN = "(" + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[0] + "|"
            + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[1] + "|" + TestInfo.FILES_IN_LEFTOVER_JOB_DIR_2[2] + ")";
    // used by HarvestControllerServerTester#testStoreHarvestInformation()
    static final File LEFTOVER_JOB_DIR_3 = new File(TestInfo.WORKING_DIR, "testserverdir3");
    static final File LEFTOVER_CRAWLDIR_3 = new File(TestInfo.LEFTOVER_JOB_DIR_3, "crawldir");
    static final File TEST_CRAWL_DIR = new File("tests/dk/netarkivet/harvester/harvesting/data/crawldir");
    static final File CRAWL_DIR_COPY = new File("tests/dk/netarkivet/harvester/harvesting/data/copyOfCrawldir");

}
