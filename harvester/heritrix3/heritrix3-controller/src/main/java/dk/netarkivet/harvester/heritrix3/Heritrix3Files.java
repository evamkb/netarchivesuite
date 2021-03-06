/*
 * #%L
 * Netarchivesuite - harvester
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
package dk.netarkivet.harvester.heritrix3;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.PersistentJobData;

/**
 * This class encapsulates the information generated by Heritrix3 or delivered to Heritrix3 before a crawl.
 */
public class Heritrix3Files {

	/** The logger for this class. */
    private static final Logger LOG = LoggerFactory.getLogger(Heritrix3Files.class);


	private static final String HERITRIX_UNPACKDIR = "heritrix3/";
	
	private File crawlDir;
	private Long harvestID;
	private Long jobID;
	private File orderXML;
	private File indexDir;
	private String archiveFilePrefix;
	private File h3ZipBall;
	private File h3CerticateFile;
	private File h3BaseDir;
	private File h3JobDir;
	private String jobName;
	private File h3LogDir;

	private File seedsFile;

	private File orderFile;
	
	public static Heritrix3Files getH3HeritrixFiles(File crawldir, PersistentJobData harvestInfo) {
		Heritrix3Files files = new Heritrix3Files();
		files.setCrawldir(crawldir);
		files.setJobId(harvestInfo.getJobID());
		files.setHarvestID(harvestInfo.getOrigHarvestDefinitionID());
		files.setArchivePrefix(harvestInfo.getHarvestFilenamePrefix());
		files.setHeritrixZip();
		files.setCertificateFile();
		files.setHeritrixBaseDir();
		files.setHeritrixJobDir();
		return files;
	}

	public static Heritrix3Files getH3HeritrixFiles(File crawldir, Job job) { 
		Heritrix3Files files = new Heritrix3Files();
		files.setCrawldir(crawldir);
		files.setJobId(job.getJobID());
		files.setHarvestID(job.getOrigHarvestDefinitionID());
		files.setArchivePrefix(job.getHarvestFilenamePrefix());
		files.setHeritrixZip();
		files.setCertificateFile();
		files.setHeritrixBaseDir();
		files.setHeritrixJobDir();
		return files;
	}
	
	private void setHarvestID(Long origHarvestDefinitionID) {
		this.harvestID = origHarvestDefinitionID;
	}

	private void setHeritrixJobDir() {
		jobName = crawlDir.getName();
		h3JobDir = new File(h3BaseDir, "jobs/" + jobName);
		h3LogDir = new File(h3JobDir, "logs");
	}

	private void setHeritrixBaseDir() {
		h3BaseDir = new File(crawlDir, HERITRIX_UNPACKDIR);
	}

	private void setHeritrixZip() {
		h3ZipBall = Settings.getFile(HarvesterSettings.HERITRIX3_BUNDLE);
		if (!h3ZipBall.isFile()) {
			throw new IOFailure("The path to the heritrix3 zipfile '" 
					+  h3ZipBall.getAbsolutePath() + "' does not represent a proper file");
		}
	}

	private void setArchivePrefix(String harvestFilenamePrefix) {
		this.archiveFilePrefix = harvestFilenamePrefix;
		
	}

	private void setJobId(Long jobID) {
		this.jobID = jobID;
	}
	
	private void setCrawldir(File crawldir) {
		this.crawlDir = crawldir;
		this.seedsFile = new File(crawldir, "seeds.txt");
		this.orderFile = new File(crawldir, "crawler-beans.cxml");
	}

	private Heritrix3Files(){
	}

	public File getCrawlDir() {
		return this.crawlDir;
	}

	public void writeSeedsTxt(String seedListAsString) {
		ArgumentNotValid.checkNotNullOrEmpty(seedListAsString, "String seedListAsString");
		LOG.debug("Writing seeds to disk as file: " + seedsFile.getAbsolutePath());
		FileUtils.writeBinaryFile(seedsFile, seedListAsString.getBytes());
	}
	
	public File getSeedsFile() {		
		return this.seedsFile;
	}
	
	public File getOrderFile() {		
		return this.orderFile;
	}


	public void setIndexDir(File indexDir) {
		ArgumentNotValid.checkExistsDirectory(indexDir, "File indexDir");
		this.indexDir = indexDir;;
		
	}
	public void writeOrderXml(HeritrixTemplate orderXMLdoc) { 
		File destination = this.orderFile;
		
		orderXMLdoc.writeToFile(destination);
		this.orderXML = destination;	
	}

	public File getProgressStatisticsLog() {
		return new File(h3LogDir, "progress-statistics.log");
	}

	public Long getJobID() {
		return this.jobID;
	}

	public File getOrderXmlFile() {
		return this.orderXML; 
	}
	public File getSeedsTxtFile() {
		return new File(h3JobDir, "seeds.txt"); 
	}

	public Long getHarvestID() {
		return this.harvestID;
	}

	public String getArchiveFilePrefix() {
		return this.archiveFilePrefix;
	}	

	public File getIndexDir() {
		return this.indexDir;
	}

	public File getCrawlLog() {
		return new File(h3LogDir, "crawl.log");
	}
	
	public File getHeritrixZip() {
		return this.h3ZipBall;
	}

	public File getCertificateFile() {
		return h3CerticateFile;
	}	

	private void setCertificateFile() {
		try {
			h3CerticateFile = Settings.getFile(HarvesterSettings.HERITRIX3_CERTIFICATE);
		} catch (UnknownID unknownID) {
			LOG.debug("No heritrix3 certificate defined in settings, using default");
			return;
		}
		if (h3CerticateFile != null && !h3CerticateFile.isFile()) {
			throw new IOFailure("The path to the heritrix3 certificate '" 
					+  h3CerticateFile.getAbsolutePath() + "' does not represent a proper file");
		}
	}
	
	public File getHeritrixOutput() {
		return new File(crawlDir, "heritrix_out.log");
	}
	
	public File getHeritrixStderrLog() {
		return new File(crawlDir, "heritrix3_err.log");
	}
	
	public File getHeritrixStdoutLog() {
		return new File(crawlDir, "heritrix3_out.log");
	}

	public File getHeritrixJobDir() {
		return h3JobDir; 
	}
	

	public File getHeritrixBaseDir() {
		return h3BaseDir;
	}

	public String getJobname() {
		return this.jobName;
	}

	public void deleteFinalLogs() {
		try {
			FileUtils.remove(getCrawlLog());
		} catch (IOFailure e) {
			// Log harmless trouble
			LOG.debug("Couldn't delete crawl log file.", e);
		}
		try {
			FileUtils.remove(getProgressStatisticsLog());
		} catch (IOFailure e) {
			// Log harmless trouble
			LOG.debug("Couldn't delete progress statistics log file.", e);
		}
	}

	public void cleanUpAfterHarvest(File oldJobsDir) {
		 // delete disposable files
        for (File disposable : getDisposableFiles()) {
            if (disposable.exists()) {
                try {
                    FileUtils.removeRecursively(disposable);
                } catch (IOFailure e) {
                    // Log harmless trouble
                    LOG.debug("Couldn't delete leftover file '{}'", disposable.getAbsolutePath(), e);
                }
            }
        }
        // move the rest to oldjobs
        FileUtils.createDir(oldJobsDir);
        File destDir = new File(oldJobsDir, crawlDir.getName());
        boolean success = crawlDir.renameTo(destDir);
        if (!success) {
            LOG.warn("Failed to rename jobdir '{}' to '{}'", crawlDir, destDir);
        }
	}

	public File[] getDisposableFiles() {
        return new File[] {new File(h3JobDir, "state"), new File(crawlDir, "checkpoints"), new File(h3JobDir, "scratch")};
    }
}
