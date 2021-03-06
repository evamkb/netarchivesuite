/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;
import java.util.List;

import dk.netarkivet.common.distribute.RemoteFile;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileBatchJob.ExceptionOccurrence;

/**
 * Class for transferring batch status information.
 */
public class BatchStatus {

    /** The total number of files processed so far. */
    private final int noOfFilesProcessed;
    /** A list of files that the batch job could not process. */
    private final Collection<File> filesFailed;
    /** The application ID identifying the bitarchive, that run this batch job. */
    private final String bitArchiveAppId;
    /** The file containing the result of the batch job. */
    private RemoteFile resultFile;

    /** A list of exceptions caught during the execution of the batchJob. */
    private final List<ExceptionOccurrence> exceptions;

    /**
     * Create a new BatchStatus object for a specific bitarchive.
     *
     * @param bitArchiveAppId The application ID identifying the bitarchive, that run this batch job.
     * @param filesFailed A list of files that the batch job could not process.
     * @param noOfFilesProcessed The total number of files processed
     * @param resultFile A file containing the result of the batch job
     * @param exceptions A list of exceptions caught during the execution of the batchJob
     */
    public BatchStatus(String bitArchiveAppId, Collection<File> filesFailed, int noOfFilesProcessed,
            RemoteFile resultFile, List<FileBatchJob.ExceptionOccurrence> exceptions) {
        this.bitArchiveAppId = bitArchiveAppId;
        this.filesFailed = filesFailed;
        this.noOfFilesProcessed = noOfFilesProcessed;
        this.resultFile = resultFile;
        this.exceptions = exceptions;
    }

    /**
     * Create a new BatchStatus object for a specific bitarchive.
     *
     * @param filesFailed A list of files that the batch job could not process
     * @param noOfFilesProcessed The total number of files processed
     * @param resultFile A file containing the result of the batch job
     * @param exceptions A list of exceptions caught during the execution of the batchJob
     */
    public BatchStatus(Collection<File> filesFailed, int noOfFilesProcessed, RemoteFile resultFile,
            List<FileBatchJob.ExceptionOccurrence> exceptions) {
        this("ALL_BITARCHIVE_APPS", filesFailed, noOfFilesProcessed, resultFile, exceptions);
    }

    /**
     * Get the number of files processed by the batch job. This counts all files whether failed or not.
     *
     * @return number of files passed to processFile
     */
    public int getNoOfFilesProcessed() {
        return noOfFilesProcessed;
    }

    /**
     * Get the File objects for the files that failed.
     *
     * @return A collection containing the files that processFile returned false on.
     */
    public Collection<File> getFilesFailed() {
        return filesFailed;
    }

    /**
     * Get the appId (internal string) for the bitarchive that these are the results from. Set to ALL_BITARCHIVE_APPS if
     * this it the combined status.
     *
     * @return A uniquely identifying string that should *not* be parsed
     */
    public String getBitArchiveAppId() {
        return bitArchiveAppId;
    }

    /**
     * Get the file containing results from a batch job. This may be null, if the batch job resulted in errors.
     *
     * @return A remote file containing results in some job-specific format.
     */
    public RemoteFile getResultFile() {
        return resultFile;
    }

    /**
     * Get the list of exceptions that happened during the batch job.
     *
     * @return List of exceptions with information on where they occurred.
     */
    public List<ExceptionOccurrence> getExceptions() {
        return exceptions;
    }

    /**
     * Copy the results of a batch job into a local file. This deletes the file from the remote server as appropriate.
     * Note that this method or appendResults can only be called once on a given object. If hasResultFile() returns
     * true, this method is safe to call.
     *
     * @param targetFile File to copy the results into. This file will be overridden if hasResultFile() returns true;
     * @throws IllegalState If the results have already been copied, or there are no results to copy due to errors.
     */
    public void copyResults(File targetFile) throws IllegalState {
        ArgumentNotValid.checkNotNull(targetFile, "targetFile");
        if (resultFile != null) {
            try {
                resultFile.copyTo(targetFile);
            } finally {
                RemoteFile tmpResultFile = resultFile;
                resultFile = null;
                tmpResultFile.cleanup();
            }
        } else {
            throw new IllegalState("No results to copy into '" + targetFile + "' from batch job on '" + bitArchiveAppId
                    + "' (" + filesFailed.size() + " failures in " + noOfFilesProcessed + " processed files)");
        }
    }

    /**
     * Append the results of a batch job into a stream. This deletes the results file from the remote server, so this or
     * copyResults can only be called once. If hasResultFile() returns true, this method is safe to call.
     *
     * @param stream A stream to append results to.
     * @throws IllegalState If the results have already been copied, or there are no results to copy due to errors.
     */
    public void appendResults(OutputStream stream) throws IllegalState {
        ArgumentNotValid.checkNotNull(stream, "OutputStream stream");
        if (resultFile != null) {
            try {
                resultFile.appendTo(stream);
            } finally {
                RemoteFile tmpResultFile = resultFile;
                resultFile = null;
                tmpResultFile.cleanup();
            }
        } else {
            throw new IllegalState("No results to append to '" + stream + "' from batch job on '" + bitArchiveAppId
                    + "' (" + filesFailed.size() + " failures in " + noOfFilesProcessed + " processed files)");
        }
    }

    /**
     * Returns true if this object has a result file. There is no result file if no bitarchives succeeded in processing
     * any files, or if the result file sent has already been deleted (e.g., by calling copyResults or appendResults).
     *
     * @return True if this object has a result file.
     */
    public boolean hasResultFile() {
        return resultFile != null;
    }

    /**
     * Returns a human-readable description of this object. The value returned should not be machine-processed, as it is
     * subject to change without notice.
     *
     * @return Human-readable description of this object.
     */
    public String toString() {
        return getFilesFailed().size() + " failures in processing " + getNoOfFilesProcessed() + " files at "
                + getBitArchiveAppId();
    }

}
