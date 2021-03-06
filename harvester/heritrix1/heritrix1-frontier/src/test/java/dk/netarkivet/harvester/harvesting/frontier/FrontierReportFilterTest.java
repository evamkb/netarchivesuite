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
package dk.netarkivet.harvester.harvesting.frontier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class FrontierReportFilterTest {

    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() throws Exception {
        rs.setUp();

        Settings.set(CommonSettings.CACHE_DIR, TestInfo.WORKDIR.getAbsolutePath());
    }

    @After
    public void tearDown() throws Exception {

        File[] testDirs = TestInfo.WORKDIR.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File dir : testDirs) {
            FileUtils.removeRecursively(dir);
        }
        rs.tearDown();
    }

    /**
     * Test the extraction of 200 top queues out of a much larger report.
     *
     * @throws IOException FIXME this test fails, and is therefore disabled by renaming it (by changing the method
     * prefix from test to TEST);
     */
    @Test
    @Ignore("test disabled before migration")
    public final void TESTTopTotalEnqueuesFilter1() throws IOException {

        File testSample = new File(TestInfo.BASEDIR, "frontierReport_all_sample_atlas502.txt");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        TopTotalEnqueuesFilter filter = new TopTotalEnqueuesFilter();
        InMemoryFrontierReport filtered = filter.process(full);

        File actualResultsFile = new File(TestInfo.WORKDIR, System.currentTimeMillis() + ".top200.txt");

        PrintWriter pw = new PrintWriter(actualResultsFile);
        // Add header line
        pw.println("queue currentSize totalEnqueues sessionBalance "
                + "lastCost(averageCost) lastDequeueTime wakeTime " + "totalSpend/totalBudget errorCount "
                + "lastPeekUri lastQueuedUri");
        for (FrontierReportLine l : filtered.getLines()) {
            pw.println(FrontierTestUtils.toString(l));
        }
        pw.close();

        File expectedResults = new File(TestInfo.BASEDIR, "frontierReport_all_sample_atlas502_top200.txt");
        assertEquals(FileUtils.readFile(expectedResults), FileUtils.readFile(actualResultsFile));

        if (!actualResultsFile.delete()) {
            actualResultsFile.deleteOnExit();
        }
    }

    /**
     * Test the extraction of 200 top queues out of a much smaller report.
     *
     * @throws IOException
     */
    @Test
    public final void testTopTotalEnqueuesFilter2() throws IOException {

        File testSample = new File(TestInfo.BASEDIR, "frontierReport_all_sample_small.txt");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        TopTotalEnqueuesFilter filter = new TopTotalEnqueuesFilter();
        InMemoryFrontierReport filtered = filter.process(full);

        File actualResultsFile = new File(TestInfo.WORKDIR, System.currentTimeMillis() + ".top200.txt");

        PrintWriter pw = new PrintWriter(actualResultsFile);
        // Add header line
        pw.println("queue currentSize totalEnqueues sessionBalance "
                + "lastCost(averageCost) lastDequeueTime wakeTime " + "totalSpend/totalBudget errorCount "
                + "lastPeekUri lastQueuedUri");
        for (FrontierReportLine l : filtered.getLines()) {
            pw.println(FrontierTestUtils.toString(l));
        }
        pw.close();

        File expectedResults = new File(TestInfo.BASEDIR, "frontierReport_all_sample_small_sorted.txt");
        assertEquals(FileUtils.readFile(expectedResults), FileUtils.readFile(actualResultsFile));

        if (!actualResultsFile.delete()) {
            actualResultsFile.deleteOnExit();
        }
    }

    public final void testRetiredQueuesFilter() throws IOException {
        File testSample = new File(TestInfo.BASEDIR, "frontierReport_all_sample_small.txt");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        RetiredQueuesFilter filter = new RetiredQueuesFilter();
        filter.init(new String[] {"200"});
        InMemoryFrontierReport filtered = filter.process(full);

        String result = "";
        for (FrontierReportLine l : filtered.getLines()) {
            result += FrontierTestUtils.toString(l);
        }

        assertTrue(result.isEmpty());
    }

    public final void testExhaustedQueuesFilter() throws IOException {
        File testSample = new File(TestInfo.BASEDIR, "frontierReport_all_sample_small.txt");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        ExhaustedQueuesFilter filter = new ExhaustedQueuesFilter();
        filter.init(new String[] {"200"});
        InMemoryFrontierReport filtered = filter.process(full);

        File actualResultsFile = new File(TestInfo.WORKDIR, System.currentTimeMillis() + ".top200.txt");

        PrintWriter pw = new PrintWriter(actualResultsFile);
        // Add header line
        pw.println("queue currentSize totalEnqueues sessionBalance "
                + "lastCost(averageCost) lastDequeueTime wakeTime " + "totalSpend/totalBudget errorCount "
                + "lastPeekUri lastQueuedUri");
        for (FrontierReportLine l : filtered.getLines()) {
            pw.println(FrontierTestUtils.toString(l));
        }
        pw.close();

        File expectedResults = new File(TestInfo.BASEDIR, "frontierReport_all_sample_atlas502_exhausted.txt");
        assertEquals(FileUtils.readFile(expectedResults), FileUtils.readFile(actualResultsFile));

        if (!actualResultsFile.delete()) {
            actualResultsFile.deleteOnExit();
        }
    }

    /**
     * Test CVS export. FIXME is disabled because it fails (by changing the method prefix from test to TEST);
     */
    public final void TESTCsvExport() throws IOException {

        File testSample = new File(TestInfo.BASEDIR, "frontierReport_all_sample_atlas502.txt");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        InMemoryFrontierReport topQueues = new TopTotalEnqueuesFilter().process(full);

        File actualResultsFile = new File(TestInfo.WORKDIR, System.currentTimeMillis() + ".csv");
        PrintWriter pw = new PrintWriter(actualResultsFile);

        FrontierReportCsvExport.outputAsCsv(topQueues, pw, ";");
        pw.close();

        File expectedResults = new File(TestInfo.BASEDIR, "atlas502_topQueues.csv");
        assertEquals(FileUtils.readFile(expectedResults), FileUtils.readFile(actualResultsFile));

        if (!actualResultsFile.delete()) {
            actualResultsFile.deleteOnExit();
        }

    }

    public final void testTopTotalEnqueuesFilter3() throws IOException {

        File testSample = new File(TestInfo.BASEDIR, "atlas201.fr.csv");

        FullFrontierReport full = FullFrontierReport.parseContentsAsString("test-" + System.currentTimeMillis(),
                FileUtils.readFile(testSample));

        TopTotalEnqueuesFilter filter = new TopTotalEnqueuesFilter();
        InMemoryFrontierReport filtered = filter.process(full);

        File actualResultsFile = new File(TestInfo.WORKDIR, System.currentTimeMillis() + ".top200.txt");

        PrintWriter pw = new PrintWriter(actualResultsFile);
        // Add header line
        pw.println("queue currentSize totalEnqueues sessionBalance "
                + "lastCost(averageCost) lastDequeueTime wakeTime " + "totalSpend/totalBudget errorCount "
                + "lastPeekUri lastQueuedUri");
        for (FrontierReportLine l : filtered.getLines()) {
            pw.println(FrontierTestUtils.toString(l));
        }
        pw.close();

        File expectedResults = new File(TestInfo.BASEDIR, "atlas201.fr.top200.txt");
        assertEquals(FileUtils.readFile(expectedResults), FileUtils.readFile(actualResultsFile));

        if (!actualResultsFile.delete()) {
            actualResultsFile.deleteOnExit();
        }
    }

}
