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

package dk.netarkivet.harvester.indexserver;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.ZipUtils;
import dk.netarkivet.common.utils.archive.GetMetadataArchiveBatchJob;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class GetMetadataArchiveBatchJobTester {

    MoveTestFiles mtf;
    File metadataDir = new File(TestInfo.WORKING_DIR, "metadata");

    @Before
    public void setUp() {
        TestInfo.WORKING_DIR.mkdir();
        File metadataDir = new File(TestInfo.WORKING_DIR, "metadata");
        metadataDir.mkdir();
        mtf = new MoveTestFiles(TestInfo.METADATA_DIR, metadataDir);
        mtf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    @Test
    public void testBatchJob() throws FileNotFoundException {

        File ZipOne = new File(metadataDir, "1-metadata-1.warc.zip");
        File ZipTwo = new File(metadataDir, "1-metadata-1.arc.zip");

        ZipUtils.unzip(ZipOne, TestInfo.WORKING_DIR);
        ZipUtils.unzip(ZipTwo, TestInfo.WORKING_DIR);

        File f1 = new File(TestInfo.WORKING_DIR, "1-metadata-1.warc");
        File f = new File(TestInfo.WORKING_DIR, "1-metadata-1.arc");

        Pattern CrawlLogUrlpattern = Pattern.compile(MetadataFile.CRAWL_LOG_PATTERN);
        Pattern textPlainMimepattern = Pattern.compile("text/plain");

        Pattern CdxUrlpattern = Pattern.compile(MetadataFile.CDX_PATTERN);
        Pattern xCDXMimepattern = Pattern.compile("application/x-cdx");

        GetMetadataArchiveBatchJob job = new GetMetadataArchiveBatchJob(CrawlLogUrlpattern, textPlainMimepattern);

        File[] files = new File[] {f1, f};
        BatchLocalFiles blf = new BatchLocalFiles(files);
        OutputStream os = new FileOutputStream("tmp");
        blf.run(job, os);
        // System.out.println(job.getNoOfFilesProcessed());

        assertEquals("Expected no files to fail, but " + job.getFilesFailed().size() + " failed", 0, job
                .getFilesFailed().size());

        job = new GetMetadataArchiveBatchJob(CdxUrlpattern, xCDXMimepattern);

        blf = new BatchLocalFiles(files);
        OutputStream os1 = new FileOutputStream("tmp");
        blf.run(job, os1);

        // System.out.println(job.getNoOfFilesProcessed());
        assertEquals("Expected no files to fail, but " + job.getFilesFailed().size() + " failed", 0, job
                .getFilesFailed().size());
    }

    //@Test
    public void testCompressedArcfile() throws FileNotFoundException {
        File f = new File(metadataDir, "2-metadata-4.arc.gz");
        Pattern CdxUrlpattern = Pattern.compile(MetadataFile.CDX_PATTERN);
        Pattern xCDXMimepattern = Pattern.compile("application/x-cdx");

        Pattern CrawlLogUrlpattern = Pattern.compile(MetadataFile.CRAWL_LOG_PATTERN);
        Pattern textPlainMimepattern = Pattern.compile("text/plain");


        GetMetadataArchiveBatchJob job = new GetMetadataArchiveBatchJob(CrawlLogUrlpattern, textPlainMimepattern);

        File[] files = new File[] {f};
        BatchLocalFiles blf = new BatchLocalFiles(files);
        OutputStream os = new FileOutputStream("tmp");
        blf.run(job, os);
        assertEquals("Expected no files to fail, but " + job.getFilesFailed().size() + " failed", 0, job
                     .getFilesFailed().size());
    }

}
