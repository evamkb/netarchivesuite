/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils.cdx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.archive.ArchiveProfile;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;

/**
 * Utility class for creating CDX-files.
 * The CDX-format is described here:
 * http://www.archive.org/web/researcher/cdx_file_format.php
 */
public class CDXUtils {

    /** The logger. */
    private static Log log = LogFactory.getLog(CDXUtils.class.getName());

    /**
     * Add cdx info for a given archive file to a given OutputStream.
     * Note, any exceptions are logged on level FINE but otherwise ignored.
     *
     * @param archivefile A file with archive records
     * @param cdxstream An output stream to add CDX lines to
     */
    public static void writeCDXInfo(File archivefile, OutputStream cdxstream) {
        ArchiveExtractCDXJob job = new ArchiveExtractCDXJob();
        BatchLocalFiles runner = new BatchLocalFiles(new File [] {archivefile});
        runner.run(job, cdxstream);
        log.trace("Created index for " + job.noOfRecordsProcessed()
                     + " records on file '" + archivefile + "'");
        Exception[] exceptions = job.getExceptionArray();
        if (exceptions.length > 0) {
            StringBuilder msg = new StringBuilder();
            for (Exception e : exceptions) {
                msg.append(ExceptionUtils.getStackTrace(e));
                msg.append('\n');
            }
            log.debug("Exceptions during generation of index on file '"
                        + archivefile + "': " + msg.toString());
        }
        log.debug("Created index of " + job.noOfRecordsProcessed()
                    + " records on file '" + archivefile + "'");

    }

   /**
     * Applies createCDXRecord() to all ARC/WARC files in a directory, creating
     * one CDX file per ARC/WARC file.
     * Note, any exceptions during index generation are logged at level FINE
     * but otherwise ignored.
     * Exceptions creating any cdx file are logged at level WARNING but
     * otherwise ignored.
     * CDX files are named as the ARC/WARC files except ".(w)arc" or
     * ".(w)arc.gz" is replaced with ".cdx"
     *
     * @param archiveProfile archive profile including filters, patterns, etc.
     * @param archiveFileDirectory A directory with archive files to generate
     * index for
     * @param cdxFileDirectory A directory to generate CDX files in
     * @throws ArgumentNotValid if any of directories are null or is not an
     * existing directory, or if cdxFileDirectory is not writable.
     */
    public static void generateCDX(ArchiveProfile archiveProfile,
            File archiveFileDirectory, File cdxFileDirectory) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(archiveProfile,
                "ArchiveProfile archiveProfile");
        ArgumentNotValid.checkNotNull(archiveFileDirectory,
                                      "File archiveFileDirectory");
        ArgumentNotValid.checkNotNull(cdxFileDirectory,
                                      "File cdxFileDirectory");
        if (!archiveFileDirectory.isDirectory() || !archiveFileDirectory.canRead()) {
            throw new ArgumentNotValid("The directory for arc files '"
                                       + archiveFileDirectory
                                       + "' is not a readable directory");
        }
        if (!cdxFileDirectory.isDirectory() || !cdxFileDirectory.canWrite()) {
            throw new ArgumentNotValid("The directory for cdx files '"
                                       + archiveFileDirectory
                                       + "' is not a writable directory");
        }
        Map<File, Exception> exceptions
                = new HashMap<File, Exception>();
        File[] filesToProcess = archiveFileDirectory.listFiles(
                archiveProfile.filename_filter);
        if (filesToProcess.length == 0) {
            log.warn("Found no related arcfiles to process in the archive dir '" 
                    + archiveFileDirectory.getAbsolutePath() + "'.");
        } else {
            log.debug("Found " + filesToProcess.length 
                    + " related arcfiles to process in the archive dir '" 
                    + archiveFileDirectory.getAbsolutePath() + "'.");
        } 
        for (File arcfile : filesToProcess) {
            File cdxfile = new File(cdxFileDirectory, arcfile.getName()
                    .replaceFirst(archiveProfile.filename_pattern,
                                  FileUtils.CDX_EXTENSION));
            if (cdxfile.getName().equals(arcfile.getName())) {
                // If for some reason the file is not renamed (should never
                // happen), simply add the .cdx extension to avoid overwriting
                // existing file
                cdxfile = new File(cdxFileDirectory,
                                   cdxfile.getName() + FileUtils.CDX_EXTENSION);
            }
            try {
                OutputStream cdxstream = null;
                try {
                    cdxstream = new FileOutputStream(cdxfile);
                    writeCDXInfo(arcfile, cdxstream);
                } finally {
                    if (cdxstream != null) {
                        cdxstream.close();
                    }
                }
            } catch (Exception e) {
                exceptions.put(cdxfile, e);
            }
        }
        // Log any errors
        if (exceptions.size() > 0) {
            StringBuilder errorMsg = new StringBuilder(
                    "Exceptions during cdx file generation:\n");
            for (Map.Entry<File, Exception> fileException: exceptions.entrySet()) {
                errorMsg.append("Could not create cdxfile '");
                errorMsg.append(fileException.getKey().getAbsolutePath());
                errorMsg.append("':\n");
                errorMsg.append(ExceptionUtils.getStackTrace(
                        fileException.getValue()));
                errorMsg.append('\n');
            }
            log.debug(errorMsg.toString());
        }
    }
}
