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
package dk.netarkivet.harvester.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.TestResourceUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Tests of the tool to create metadata files.
 */
public class CreateCDXMetadataFileTester {
    // private static String CONTENT = "This is a test message";
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf;
    private MockupJMS mjms = new MockupJMS();
    // TestMessageListener listener;

    File job2MetadataFile = new File("2-metadata-1.arc");
    File job4MetadataFile = new File("4-metadata-1.arc");
    File job70MetadataFile = new File("70-metadata-1.arc");
    private UseTestRemoteFile utrf = new UseTestRemoteFile();

    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    @Before
    public void setUp() {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        Settings.set(HarvesterSettings.METADATA_FORMAT, "arc");
        //mtf = new MoveTestFiles(TestInfo.DATA_DIR, WORKING_DIR);

        utrf.setUp();
        mjms.setUp();
        // listener = new BatchListener();
        // JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), listener);
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }

    @After
    public void tearDown() {
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        // JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), listener);
        mjms.tearDown();
        utrf.tearDown();

        FileUtils.remove(job2MetadataFile);
        FileUtils.remove(job4MetadataFile);
        FileUtils.remove(job70MetadataFile);
    }

    /**
     * Test that arguments are handled correctly.
     */
    @Test
    @Ignore("Incorrect usage text output")
    public void testMain() {
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baosOut));
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baosErr));
        String usage = CreateCDXMetadataFile.usageString;
        // Check lack of args
        try {
            CreateCDXMetadataFile.main(new String[] {});
            fail("Should System.exit(1) on no args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout", "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringContains("Should have usage in stderr", "Missing or wrong arguments given",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // // Check too many args
        // try {
        // CreateCDXMetadataFile.main(new String[]{ "11", "42", "77"});
        // fail("Should System.exit(1) on illegal args");
        // } catch (SecurityException e) {
        // System.out.flush();
        // assertEquals("Should not write anything to stdout",
        // "", baosOut.toString());
        // System.err.flush();
        // StringAsserts.assertStringMatches(
        // "Should have usage and errors in stderr",
        // "Too many arguments: '11', '42', '77'"
        // + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
        // baosErr.toString());
        // baosOut.reset();
        // baosErr.reset();
        // }

        // Check illegal arg: negative
        // try {
        // CreateCDXMetadataFile.main(new String[]{ "-11L"});
        // fail("Should System.exit(1) on illegal args");
        // } catch (SecurityException e) {
        // System.out.flush();
        // assertEquals("Should not write anything to stdout",
        // "", baosOut.toString());
        // System.err.flush();
        // StringAsserts.assertStringMatches(
        // "Should have usage and errors in stderr",
        // "-11 is not a valid job ID"
        // + ".*Usage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile jobID",
        // baosErr.toString());
        // baosOut.reset();
        // baosErr.reset();
        // }

        // Check illegal arg: 0
        try {
            CreateCDXMetadataFile.main(new String[] {"--jobID 0 --harvestnamePrefix 0-1"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout", "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringContains("Should have usage and errors in stderr", "0 is not a valid job ID\n"
                    + "Usage: " + usage, baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }

        // Check illegal arg: non-numeral
        try {
            CreateCDXMetadataFile.main(new String[] {"--jobID foo42bar --harvestnamePrefix 0-1"});
            fail("Should System.exit(1) on illegal args");
        } catch (SecurityException e) {
            System.out.flush();
            assertEquals("Should not write anything to stdout", "", baosOut.toString());
            System.err.flush();
            StringAsserts.assertStringContains("Should have usage and errors in stderr",
                    "'foo42bar' is not a valid job ID"
                            + "\nUsage: java dk.netarkivet.harvester.tools.CreateCDXMetadataFile [-a|w] jobID",
                    baosErr.toString());
            baosOut.reset();
            baosErr.reset();
        }
    }

    @Test
    @Ignore("Does not exit normally")
    public void testRunSingleJob() {
        try {
            CreateCDXMetadataFile.main(new String[] {"--jobID 4 --harvestnamePrefix 4-1"});
        } catch (SecurityException e) {
            assertEquals("Should have exited normally", 0, pse.getExitValue());
        }
        assertTrue("Should have a result file", job4MetadataFile.exists());
        assertFalse("Should not have other results files", job2MetadataFile.exists());
        assertFalse("Should not have other results files", job70MetadataFile.exists());
        FileAsserts.assertFileContains("Should have first line",
                "http://netarkivet.dk/fase2index-da.php 130.225.27.144 20060329091238 ", job4MetadataFile);
        FileAsserts.assertFileContains("Should have some intermediate line", "dns:netarkivet.dk 130.225.24.33",
                job4MetadataFile);
        FileAsserts.assertFileContains("Should have last line",
                "http://netarkivet.dk/netarchive_alm/billeder/netarkivet_guidelines_13.gif 130.225.27.144 20060329091256",
                job4MetadataFile);
    }

    @Test
    @Ignore("was commented out")
    public void testRunFailingJob() {
        // // Test with failure
        // File outputFile = new File(TestInfo.WORKING_DIR, "tmpout");
        // outputFile.delete();
        // outputFile.mkdir(); // Force unwriteable file.
        // try {
        // CreateCDXMetadataFile.main(new String[] { "5" });
        // } catch (SecurityException e) {
        // assertEquals("Should have exited normally",
        // 0, pse.getExitValue());
        // }
        // // Should not die on errors in the batch job (null result file)
    }

    /**
     * This class is a MessageListener that responds to BatchMessage, simulating an ArcRepository.
     */
    /*
     * FIXME: Maven-migration Disabling to avoid cyclic dependency to Archive module through BatchMessage private static
     * class BatchListener extends TestMessageListener { public BatchListener() { } public void onMessage(Message o) {
     * super.onMessage(o); NetarkivetMessage nmsg = received.get(received.size() - 1); if (nmsg instanceof BatchMessage)
     * { BatchMessage m = (BatchMessage) nmsg; int count = 0; List<File> emptyList = Collections.emptyList(); RemoteFile
     * rf; try { File output = new File(TestInfo.WORKING_DIR, "tmpout"); BufferedReader reader = new BufferedReader(new
     * FileReader( new File(TestInfo.DATA_DIR, "jobs-2-4-70.cdx"))); FileWriter writer = new FileWriter(output); String
     * line; Pattern p = Pattern.compile("^(\\S+\\s+){5}" + m.getJob().getFilenamePattern().pattern() +
     * "(\\s+\\S+){2}$"); while ((line = reader.readLine()) != null) { if (p.matcher(line).matches()) {
     * writer.write(line + "\n"); count++; } } reader.close(); writer.close(); rf = new TestRemoteFile(output, false,
     * false, false); } catch (IOException e) { System.out.println(e); e.printStackTrace(); rf = null; }
     * JMSConnectionFactory.getInstance().send( new BatchReplyMessage(m.getReplyTo(), Channels.getError(), m.getID(),
     * count, emptyList, rf)); } } };
     */
}
