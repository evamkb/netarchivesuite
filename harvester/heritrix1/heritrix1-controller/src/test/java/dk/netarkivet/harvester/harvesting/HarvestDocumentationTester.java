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
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.archive.ArchiveProfile;
import dk.netarkivet.common.utils.cdx.CDXUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFileWriter;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.LogbackRecorder;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.TestResourceUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

public class HarvestDocumentationTester {

	//ReloadSettings rs = new ReloadSettings();

    @Rule public TestName test = new TestName();
    private File WORKING_DIR;
    
    private JobInfo harvestJob = new JobInfoTestImpl(Long.parseLong(Heritrix1ControllerTestInfo.ARC_JOB_ID), Heritrix1ControllerTestInfo.HARVEST_ID);
    //private JobInfo harvestJob = new JobInfoTestImpl(Long.parseLong(TestInfo.ARC_JOB_ID), TestInfo.HARVEST_ID);

    //FIXME hardwired to H1 heritrixFiles
    //private HeritrixFiles Okfiles = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(TestInfo.WORKING_DIR, harvestJob);
    //private IngestableFiles OkIngestables = null;
    //FIXME hardwired to H1 heritrixFiles
    private HeritrixFiles filesWithNonexistingDir = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(new File("foodoesnotexist"), harvestJob);
    //FIXME hardwired to H1 heritrixFiles
    private HeritrixFiles filesWithExistingFileInsteadOfDir = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(Heritrix1ControllerTestInfo.ORDER_FILE, harvestJob);

    @Before
    public void initialize() {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);
        Settings.set(HarvesterSettings.METADATA_FORMAT, "arc");
    }

    /**
     * Unit test of method HarvestDocumentation.documentHarvest(). For simplicity, this test only covers the normal
     * case; error cases are tested by the method below.
     * <p>
     * Verifies that after calling the method, a new ARC file is created with the appropriate name (see
     * getMetadataARCFileName()). Verifies that this ARC file contains one record of MIME type "application/cdx" per
     * original ARC file in the dir (and no other records of that type). Verifies that the CDX records are named
     * appropriately (see getCDXURI()).
     */
    @Test
    public void testDocumentHarvestOrdinaryCase() throws IOException {
        /*
         * Run the method on a working-dir that mirrors TestInfo.METADATA_TEST_DIR.
         */
        WORKING_DIR.mkdirs();
        FileUtils.copyDirectory(Heritrix1ControllerTestInfo.METADATA_TEST_DIR, WORKING_DIR);

        // Add preharvest metadata to "metadata" subdirectory of the crawldir
        MetadataEntry me = new MetadataEntry(
                "metadata://netarkivet.dk/crawl/setup/aliases?version=1&harvestid=42&harvestnum=1&jobid=117",
                "text/plain", "tv2.dk is an alias of statsbiblioteket.dk\n");
        List<MetadataEntry> l = new ArrayList<>();
        l.add(me);
        File metadatadir = new File(WORKING_DIR, IngestableFiles.METADATA_SUB_DIR);
        metadatadir.mkdir();
        MetadataEntry.storeMetadataToDisk(l, metadatadir);

        JobInfo harvestJob = new JobInfoTestImpl(Long.parseLong(Heritrix1ControllerTestInfo.ARC_JOB_ID), 117L);
        //FIXME hardwired to a H1 HeritrixFiles
        HeritrixFiles files = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, harvestJob);

        IngestableFiles inf = new IngestableFiles(files);

        HarvestDocumentation.documentHarvest(inf);

        // Verify that the new file exists.
        MetadataFileWriter.getMetadataArchiveFileName(Heritrix1ControllerTestInfo.ARC_JOB_ID, Long.parseLong(Heritrix1ControllerTestInfo.ARC_HARVEST_ID));

        List<File> fs = inf.getMetadataArcFiles();
        assertEquals("Should have created exactly one file ", 1, fs.size());
        File f = fs.get(0);
        // System.out.println(f.getAbsolutePath());
        assertTrue("The file should exist: " + f.toString(), f.exists());
        // Put an ARCReader on top of the file.
        ARCReader r = ARCReaderFactory.get(f);
        Iterator<ArchiveRecord> it = r.iterator();
        // Read each record, checking content-type and URI.
        Set<String> cdxURISet = new HashSet<String>();
        cdxURISet.add(MetadataFileWriter.getCDXURI(Heritrix1ControllerTestInfo.ARC_HARVEST_ID, Heritrix1ControllerTestInfo.ARC_JOB_ID,
                Heritrix1ControllerTestInfo.FST_FILENAME).toASCIIString());
        cdxURISet.add(MetadataFileWriter.getCDXURI(Heritrix1ControllerTestInfo.ARC_HARVEST_ID, Heritrix1ControllerTestInfo.ARC_JOB_ID,
                Heritrix1ControllerTestInfo.SND_FILENAME).toASCIIString());

        String aliasFound = null;
        while (it.hasNext()) {
            ARCRecord record = (ARCRecord) it.next();
            ARCRecordMetaData meta = record.getMetaData();
            // System.out.println("Url: " + meta.getUrl());
            // System.out.println("mimetype: " + meta.getMimetype());
            if (meta.getMimetype().equals("application/x-cdx")) {
                // System.out.println("Cdxuri: " + meta.getUrl());
                assertTrue("Bad URI in metadata: " + meta.getUrl(), cdxURISet.contains(meta.getUrl()));
                cdxURISet.remove(meta.getUrl());
            }
            if (meta.getUrl().startsWith("metadata://netarkivet.dk/crawl/setup/aliases")) {
                aliasFound = ARCTestUtils.readARCRecord(record);
            }
        }
        assertTrue("Metadata should not contain CDX records: " + cdxURISet, cdxURISet.isEmpty());
        assertNotNull("Should have found some alias information", aliasFound);
        assertEquals("Should have found the right alias metadata", "tv2.dk is an alias of statsbiblioteket.dk\n",
                aliasFound);
    }

    @Test(expected = ArgumentNotValid.class)
    public void testDocumentHarvestNonexistingCrawlDir() {
        HeritrixFiles filesWithNonexistingDir = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(new File("foodoesnotexist"), harvestJob);
        IngestableFiles ingestablesWithNonexistingCrawlDir = new IngestableFiles(filesWithNonexistingDir);
        HarvestDocumentation.documentHarvest(ingestablesWithNonexistingCrawlDir);
    }

    @Test(expected = PermissionDenied.class)
    public void testDocumentHarvestNonDir() {
        HeritrixFiles filesWithExistingFileInsteadOfDir = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(Heritrix1ControllerTestInfo.ORDER_FILE, harvestJob);
        IngestableFiles ingestablesWithFileAsCrawlDir = new IngestableFiles(filesWithExistingFileInsteadOfDir);
        HarvestDocumentation.documentHarvest(ingestablesWithFileAsCrawlDir);
    }

    /**
     * Unit test of method HarvestDocumentation.documentHarvest(). This unit test covers the following error cases: -
     * dir does not exist or is not dir -> IOFailure - the ARC files in dir do not share harvestID and jobID -> throw
     * UnknownID
     */
    @Test
    public void testDocumentHarvestExceptionalCases() {
        // the ARC files in dir do not share harvestID and jobID
        WORKING_DIR.mkdirs();
        File arcsDir = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(Heritrix1ControllerTestInfo.METADATA_TEST_DIR_INCONSISTENT, arcsDir);
        // JobInfo for harvestId 117
        JobInfo harvestJob = new JobInfoTestImpl(Long.parseLong(Heritrix1ControllerTestInfo.ARC_JOB_ID), 117L);
        //FIXME hardwired to a H1 heritrixFiles
        HeritrixFiles files117 = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, harvestJob);
        IngestableFiles OkIngestables = new IngestableFiles(files117);

        HarvestDocumentation.documentHarvest(OkIngestables);
        List<File> arcFiles = Arrays.asList(arcsDir.listFiles(FileUtils.ARCS_FILTER));
        assertEquals("Should have exactly 1 ARC file (no metadata here)", 1, arcFiles.size());
        // System.out.println(Okfiles.getArchiveFilePrefix());
        assertEquals("Should only have consistently named arc file left. Expected " + Heritrix1ControllerTestInfo.ARC_FILE_0.getName()
                + " but got " + arcFiles.get(0).getName(), Heritrix1ControllerTestInfo.ARC_FILE_0.getName(), arcFiles.get(0).getName());

        List<File> metadataFiles = OkIngestables.getMetadataArcFiles();
        File metadataDir = new File(WORKING_DIR, "metadata");
        File target1 = new File(metadataDir, MetadataFileWriter.getMetadataArchiveFileName(Long
                .toString(Heritrix1ControllerTestInfo.JOB_ID), Heritrix1ControllerTestInfo.HARVEST_ID));
        assertEquals("Should generate exactly one metadata file", 1, metadataFiles.size());
        assertTrue("Should generate file " + target1 + " but found only " + metadataFiles.toString(),
                metadataFiles.contains(target1));
    }

    @Test (expected = ArgumentNotValid.class)
    public void testGetMetadataARCNullFileName() {
        MetadataFileWriter.getMetadataArchiveFileName(null, null);
    }

    /**
     * Unit test for HarvestDocumentation.getMetadataARCFileName() Verifies that the name of the new ARC file ends on
     * .arc and that the parameter is part of the file name. Also verifies that null parameters are not accepted.
     */
    @Test
    public void testGetMetadataARCFileName() {
        String job = "7";
        Long harvestId = 43L;
        String fn = MetadataFileWriter.getMetadataArchiveFileName(job, harvestId);
        assertTrue("File name should end on '-1.arc' - was " + fn, fn.endsWith("-1.arc"));
        assertTrue("File name should contain jobID - was " + fn, fn.contains(job));
        assertTrue("File name should contain the string 'metadata' - was " + fn, fn.contains("metadata"));
    }

    /**
     * Unit test for HarvestDocumentation.getCDXURI() Verfies that the URI begins with
     * "metadata://netarkivet.dk/crawl/index/cdx?" and that it contains all four of the method's parameters. Also
     * verifies that null parameters are not accepted.
     */
    @Test
    public void testGetCDXURI() {
        String harv = "42";
        String job = "7";
        // String time = "99999999999999";
        // String serial = "000000001";
        String filename = "7-42-mymachine.mydomain-001.arc";
        try {
        	MetadataFileWriter.getCDXURI(null, job, filename);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        try {
        	MetadataFileWriter.getCDXURI(harv, null, filename);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        try {
        	MetadataFileWriter.getCDXURI(harv, job, null);
            fail("Should have thrown ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        String uri = MetadataFileWriter.getCDXURI(harv, job, filename).toString();
        String prefix = "metadata://netarkivet.dk/crawl/index/cdx?";
        assertTrue("Should name the CDX URI following the official pattern - was " + uri, uri.startsWith(prefix));
        assertTrue("CDX URI should contain harvestID - was " + uri, uri.contains(harv));
        assertTrue("CDX URI should contain jobID - was " + uri, uri.contains(job));
        assertTrue("CDX URI should contain filename - was " + uri, uri.contains(filename));
    }

    /**
     * Unit test method for generating a CDX index of an Arc file. FIXME Broken by
     * http://sbforge.org/jira/browse/NAS-1918
     */
    @Test
    @Ignore("Contains dashes instead of actual ip-numbers")
    public void failingtestCreateCDXFile() throws IOException {
        OutputStream cdxstream = new ByteArrayOutputStream();
        cdxstream.write("BEFORE\n".getBytes());
        CDXUtils.writeCDXInfo(Heritrix1ControllerTestInfo.ARC_FILE_1, cdxstream);
        assertEquals("Stream should have expected content", "BEFORE\n" + FileUtils.readFile(Heritrix1ControllerTestInfo.CDX_FILE),
                cdxstream.toString());

        // Testing on a non-arc file to see results
        cdxstream = new ByteArrayOutputStream();
        cdxstream.write("Start\n".getBytes());
        CDXUtils.writeCDXInfo(Heritrix1ControllerTestInfo.SEEDS_FILE, cdxstream);
        assertEquals("Stream should have no new content", "Start\n", cdxstream.toString());

        // Testing on a gzipped file to see results
        cdxstream = new ByteArrayOutputStream();
        cdxstream.write("Begin\n".getBytes());
        CDXUtils.writeCDXInfo(Heritrix1ControllerTestInfo.ARC_FILE2, cdxstream);
        assertEquals("Stream should have expected new content", "Begin\n" + FileUtils.readFile(
                Heritrix1ControllerTestInfo.CDX_FILE2),
                cdxstream.toString());
    }


    @Test (expected = ArgumentNotValid.class)
    public void testGenerateCDXNullProfile() {
        CDXUtils.generateCDX(null, Heritrix1ControllerTestInfo.ARC_REAL_DIR, Heritrix1ControllerTestInfo.CDX_WORKING_DIR);
    }
    @Test (expected = ArgumentNotValid.class)
    public void testGenerateNullArcDir() {
        CDXUtils.generateCDX(ArchiveProfile.ARC_PROFILE, null, Heritrix1ControllerTestInfo.CDX_WORKING_DIR);
    }
    @Test (expected = ArgumentNotValid.class)
     public void testGenerateNullCDXDir() {
        CDXUtils.generateCDX(ArchiveProfile.ARC_PROFILE, Heritrix1ControllerTestInfo.ARC_REAL_DIR, null);
    }
    @Test (expected = ArgumentNotValid.class)
    public void testGenerateOnvalidArcDir() {
        CDXUtils.generateCDX(ArchiveProfile.ARC_PROFILE, Heritrix1ControllerTestInfo.ARC_FILE_1, Heritrix1ControllerTestInfo.CDX_WORKING_DIR);
    }
    @Test (expected = ArgumentNotValid.class)
    public void testGenerateOnvalidCDXDir() {
        CDXUtils.generateCDX(ArchiveProfile.ARC_PROFILE, Heritrix1ControllerTestInfo.ARC_REAL_DIR, Heritrix1ControllerTestInfo.ARC_FILE_1);
    }

    @Test
    public void testGenerateCDX() throws IOException {
        Heritrix1ControllerTestInfo.CDX_WORKING_DIR.mkdirs();
        CDXUtils.generateCDX(ArchiveProfile.ARC_PROFILE, Heritrix1ControllerTestInfo.ARC_REAL_DIR, Heritrix1ControllerTestInfo.CDX_WORKING_DIR);
        File[] originalFiles = Heritrix1ControllerTestInfo.ARC_REAL_DIR.listFiles(FileUtils.ARCS_FILTER);
        File[] generatedFiles = Heritrix1ControllerTestInfo.CDX_WORKING_DIR.listFiles();
        assertEquals(
                "Should have generated the right number of files, but " + "found " + Arrays.asList(generatedFiles),
                originalFiles.length, generatedFiles.length);
        for (File original : originalFiles) {
            File cdxfile = new File(Heritrix1ControllerTestInfo.CDX_WORKING_DIR, original.getName() + FileUtils.CDX_EXTENSION);
            assertTrue("Should be a cdx file with correct name for '" + original + "'", cdxfile.isFile());
            OutputStream content = new ByteArrayOutputStream();
            CDXUtils.writeCDXInfo(original, content);
            assertEquals("File '" + cdxfile.getAbsolutePath() + "' should contain expected content",
                    content.toString(), FileUtils.readFile(cdxfile));
        }

    }

    /**
     * Test that any files not part of the current harvest are moved away to the oldjobsdir during postprocessing. This
     * test includes a check for issue https://sbforge.org/jira/browse/NAS-2270 which resulted in at most one old file
     * being movied away.
     *
     * @throws Exception
     */
    @Test
    public void testMoveAwayForeignFiles() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        Method m = ReflectUtils.getPrivateMethod(HarvestDocumentation.class, "moveAwayForeignFiles",
                ArchiveProfile.class, File.class, IngestableFiles.class);
        // Set oldjobs place to a different name to check use of setting.
        Settings.set(HarvesterSettings.HARVEST_CONTROLLER_OLDJOBSDIR, new File(WORKING_DIR, "oddjobs").getAbsolutePath());
        WORKING_DIR.mkdirs();
        File arcsDir = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(Heritrix1ControllerTestInfo.METADATA_TEST_DIR_INCONSISTENT, arcsDir);
        FileUtils.copyFile(new File(Heritrix1ControllerTestInfo.METADATA_TEST_DIR, "arcs/not-an-arc-file.txt"), new File(arcsDir,
                "43-metadata-1.arc"));

        // JobInfo for harvestId 117
        JobInfo harvestJob = new JobInfoTestImpl(42L, 117L);
        
        HeritrixFiles files117 = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, harvestJob);
        IngestableFiles OkIngestables = new IngestableFiles(files117);

        m.invoke(null, ArchiveProfile.ARC_PROFILE, arcsDir, OkIngestables);
        // Check that one file got moved.
        lr.assertLogContains("Should have found foreign files", "Found files not belonging");
        String badFile = "43-117-20051212141241-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        String goodFile = "42-117-20051212141240-00000-sb-test-har-001.statsbiblioteket.dk.arc";
        System.out.println("Locating lost-files directory");
        File oddjobsDir = new File(WORKING_DIR, "oddjobs");
        File[] oddDirs = oddjobsDir.listFiles();
        assertTrue(oddDirs.length == 1);
        for (File oddDir : oddDirs) {
            System.out.println(oddDir.getAbsolutePath());
        }
        File oldJobsDir43 = oddDirs[0];
        assertTrue("Lostfiles dir should begin with 'lostfiles-'", oldJobsDir43.getName().startsWith("lost-files"));

        File movedFile = new File(oldJobsDir43, badFile);
        assertTrue("Moved file " + movedFile + " should exist", movedFile.exists());
        File goneFile = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME + "/" + badFile);
        assertFalse("Moved file " + goneFile + " should have gone from crawldir", goneFile.exists());

        File keptFile = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME + "/" + goodFile);
        assertTrue("Good file " + keptFile + " should not have moved", keptFile.exists());

        File badMetadataFile = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME + "/43-metadata-1.arc");
        assertFalse("Metadata file " + badMetadataFile + " should have moved", badMetadataFile.exists());
        File movedMetadataFile = new File(oldJobsDir43, badMetadataFile.getName());
        assertTrue("Metadata file " + movedMetadataFile + " should be in oldjobs", movedMetadataFile.exists());
        lr.stopRecorder();
    }

    /**
     * This tests, that bug 722 is solved. It should document, that we 1) generate a metadata-arc-file for the
     * harvestjob, if one does not exist 2) Don't generate metadata-arc file, it already exists but issue a warning
     * instead. 3) [not testable in this class] only upload the metadata-arc-file after all other arc-files have been
     * uploaded.
     *
     * @throws Exception
     */
    @Test
    public void testDocumentHarvestBug722() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        WORKING_DIR.mkdirs();
        File arcsDir = new File(WORKING_DIR, Constants.ARCDIRECTORY_NAME);
        TestFileUtils.copyDirectoryNonCVS(Heritrix1ControllerTestInfo.METADATA_TEST_DIR, WORKING_DIR);
        // IngestableFiles ingestableFiles = new IngestableFiles(
        // WORKING_DIR,
        // TestInfo.JOB_ID);

        IngestableFiles OkIngestables = new IngestableFiles(HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, harvestJob));
        IngestableFiles ingestableFiles = OkIngestables;

        // Test 1:we generate a metadata-file, if it does not exist
        // HarvestDocumentation.documentHarvest(WORKING_DIR,
        // TestInfo.JOB_ID,
        // TestInfo.HARVEST_ID);
        HarvestDocumentation.documentHarvest(ingestableFiles);
        assertTrue("MetadataFile should exist now", ingestableFiles.isMetadataReady());
        String fileContent = FileUtils.readFile(ingestableFiles.getMetadataArcFiles().get(0));

        // test 2: Don't generate metadata-arc file, it already exists
        // but issue a warning instead.

        HarvestDocumentation.documentHarvest(OkIngestables);
        FileUtils.remove(new File(arcsDir, "42-117-20051212141241-00001-sb-test-har-001.statsbiblioteket.dk.arc"));

        String metadataDirPath = new File(WORKING_DIR, IngestableFiles.METADATA_SUB_DIR).getAbsolutePath();
        String filename = MetadataFileWriter.getMetadataArchiveFileName("" + Heritrix1ControllerTestInfo.JOB_ID, Heritrix1ControllerTestInfo.HARVEST_ID);

        lr.assertLogContains("Should have issued warning about existing metadata-arcfile", "The metadata-file '"
                + metadataDirPath + "/" + filename + "' already exists, so we don't make another one!");

        String newFileContent = FileUtils.readFile(ingestableFiles.getMetadataArcFiles().get(0));
        assertEquals("File contents should be unchanged", fileContent, newFileContent);
        lr.stopRecorder();
    }

    /**
     * Test that all necessary harvest logs, reports and configurations are stored in the proper way.
     */
    @Test
    public void testWriteHarvestDetails() throws IOException {
        ReloadSettings rs = new ReloadSettings(new File(Heritrix1ControllerTestInfo.ORIGINALS_DIR, "metadata_settings.xml"));
        rs.setUp();

        WORKING_DIR.mkdirs();
        TestFileUtils.copyDirectoryNonCVS(Heritrix1ControllerTestInfo.CRAWLDIR_ORIGINALS_DIR, WORKING_DIR);

        IngestableFiles OkIngestables = new IngestableFiles(HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(WORKING_DIR, harvestJob));
        HarvestDocumentation.documentHarvest(OkIngestables);

        // Ensure that now exists arc-file containing <jobid>-metadata-1.arc
        IngestableFiles iF = OkIngestables;
        assertFalse("MetadataARC should have generated by now", iF.getMetadataArcFiles().isEmpty());

        assertEquals("Only one metadata-1.arc expected", 1, iF.getMetadataArcFiles().size());
        // Go through all the records, and check if all relevant data is being stored
        // Replace WORKING_DIR by TestInfo.CRAWLDIR_ORIGINALS_DIR, because the testing will not work after
        // we now remove the files.
        // File ORIGINAL_CRAWLDIR = TestInfo.CRAWLDIR_ORIGINALS_DIR;
        File metadataArcFile = iF.getMetadataArcFiles().get(0);
        String URL = "metadata://netarkivet.dk/crawl/setup/order.xml?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid="
                + Heritrix1ControllerTestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(WORKING_DIR, "order.xml"));

        URL = "metadata://netarkivet.dk/crawl/setup/seeds.txt?heritrixVersion=" + Constants.getHeritrixVersionString()
                + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid=" + Heritrix1ControllerTestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(WORKING_DIR, "seeds.txt"));

        URL = "metadata://netarkivet.dk/crawl/reports/hosts-report.txt?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid="
                + Heritrix1ControllerTestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(WORKING_DIR, "hosts-report.txt"));

        // This section necessitated by bug 778
        URL = "metadata://netarkivet.dk/crawl/setup/harvestInfo.xml?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid="
                + Heritrix1ControllerTestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        // We need to force the filename to be non-interned, to check that
        // .equals() is called.
        checkThatStillExist(new File(WORKING_DIR, new String("harvestInfo.xml".toCharArray())));

        URL = "metadata://netarkivet.dk/crawl/reports/seeds-report.txt?heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid="
                + Heritrix1ControllerTestInfo.JOB_ID;
        findAndVerifyMetadata(metadataArcFile, URL);
        checkThatNoLongerExist(new File(WORKING_DIR, "seeds-report.txt"));

        URL = "metadata://netarkivet.dk/crawl/logs/crawl.log?heritrixVersion=" + Constants.getHeritrixVersionString()
                + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid=" + Heritrix1ControllerTestInfo.JOB_ID;
        // File logDir = new File(ORIGINAL_CRAWLDIR, "logs");
        findAndVerifyMetadata(metadataArcFile, URL);
        File logDir = new File(WORKING_DIR, "logs");
        // We need to force the filename to be non-interned, to check that
        // .equals() is called.
        checkThatStillExist(new File(logDir, new String("crawl.log".toCharArray())));

        // Test that domain-specific settings (a.k.a overrides) are
        // stored in the proper way.
        URL = "metadata://netarkivet.dk/crawl/setup/settings.xml?" + "heritrixVersion="
                + Constants.getHeritrixVersionString() + "&harvestid=" + Heritrix1ControllerTestInfo.HARVEST_ID + "&jobid="
                + Heritrix1ControllerTestInfo.JOB_ID + "&domain=kb.dk";
        findAndVerifyMetadata(metadataArcFile, URL);

    }

    @Test
    public void testMetadataFilters() {
        String heritrixFilePattern = Settings.get(HarvesterSettings.METADATA_HERITRIX_FILE_PATTERN);

        String[] heritrixFiles = {"order.xml", "harvestInfo.xml", "seeds.txt", "crawl-report.txt",
                "frontier-report.txt", "hosts-report.txt", "mimetype-report.txt", "processors-report.txt",
                "responsecode-report.txt," + "seeds-report.txt", "crawl.log", "local-errors.log",
                "progress-statistics.log", "runtime-errors.log", "uri-errors.log", "heritrix.out"};

        for (String f : heritrixFiles) {
            assertTrue(f + " does not match " + heritrixFilePattern, f.matches(heritrixFilePattern));
        }

        String reportFilePattern = Settings.get(HarvesterSettings.METADATA_REPORT_FILE_PATTERN);

        String[] reportFiles = {"crawl-report.txt", "frontier-report.txt", "hosts-report.txt", "mimetype-report.txt",
                "processors-report.txt", "responsecode-report.txt," + "seeds-report.txt"};

        for (String f : reportFiles) {
            assertTrue(f + " does not match " + heritrixFilePattern, f.matches(heritrixFilePattern));
            assertTrue(f + " does not match " + reportFilePattern, f.matches(reportFilePattern));
        }

        String logFilePattern = Settings.get(HarvesterSettings.METADATA_LOG_FILE_PATTERN);

        String[] logFiles = {"crawl.log", "local-errors.log", "progress-statistics.log", "runtime-errors.log",
                "uri-errors.log", "heritrix.out"};

        for (String f : logFiles) {
            assertTrue(f + " does not match " + heritrixFilePattern, f.matches(heritrixFilePattern));
            assertTrue(f + " does not match " + logFilePattern, f.matches(logFilePattern));
        }
    }

    // ////////////////////////////////////////////////////////////////
    // Utility methods
    // ///////////////////////////////////////////////////////////////

    private void checkThatStillExist(File fileThatStillShouldExist) {
        if (!fileThatStillShouldExist.exists()) {
            fail("This file should still exist: " + fileThatStillShouldExist.getAbsolutePath());
        }

    }

    private void checkThatNoLongerExist(File fileThatNoLongerShouldExist) {
        if (fileThatNoLongerShouldExist.exists()) {
            fail("This file should no longer exist: " + fileThatNoLongerShouldExist.getAbsolutePath());
        }
    }

    private void findAndVerifyMetadata(File metadataArcFile, String url) throws IOException {
        ARCReader ar = null;
        try {
            ar = ARCReaderFactory.get(metadataArcFile);
            Iterator<ArchiveRecord> it = ar.iterator();
            while (it.hasNext()) {
                ARCRecord record = (ARCRecord) it.next();
                String thisUrl = record.getMetaData().getUrl();
                if (thisUrl.equals(url)) {
                    // System.out.println("Found needed URL: " + thisUrl);
                    // TODO: Compare the contents of the record with the contents
                    // of the argument file.
                    return;
                } else {
                    // System.out.println("Needs URL: " + url);
                    // System.out.println("Found URL: " + thisUrl);
                }

            }
            fail(url + " not found in metadatafile: " + metadataArcFile.getAbsolutePath());
        } catch (IOException e) {
            fail("IOException not expected: " + e.toString());
        } finally {
            if (ar != null) {
                ar.close();
            }
        }
    }
}
