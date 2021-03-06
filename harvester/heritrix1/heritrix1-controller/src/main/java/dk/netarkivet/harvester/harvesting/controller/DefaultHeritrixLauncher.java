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
package dk.netarkivet.harvester.harvesting.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.HeritrixLauncher;

/**
 * Default implementation of the crawl control.
 */
public class DefaultHeritrixLauncher extends HeritrixLauncher {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(DefaultHeritrixLauncher.class);

    /** Number of milliseconds in a second. */
    private static final int MILLIS_PER_SECOND = 1000;

    /** How long to wait before aborting a request from a webserver. */
    private static long timeOutInMillisReceivedData = Long.parseLong(Settings
            .get(HarvesterSettings.CRAWLER_TIMEOUT_NON_RESPONDING)) * MILLIS_PER_SECOND;

    /** How long to wait without any activity before aborting the harvest. */
    private static long timeOutInMillis = Long.parseLong(Settings.get(HarvesterSettings.INACTIVITY_TIMEOUT_IN_SECS))
            * MILLIS_PER_SECOND;
    /** The HeritrixController instance used by the HeritrixLauncher. */
    private HeritrixController heritrixController;

    /**
     * Constructor for the DefaultHeritrixLauncher.
     *
     * @param files the Heritrix configuration.
     * @throws ArgumentNotValid
     */
    private DefaultHeritrixLauncher(HeritrixFiles files) throws ArgumentNotValid {
        super(files);
    }

    /**
     * Get instance of this class.
     *
     * @param files Object encapsulating location of Heritrix crawldir and configuration files
     * @return {@link DefaultHeritrixLauncher} object
     * @throws ArgumentNotValid If either order.xml or seeds.txt does not exist, or argument files is null.
     */
    public static DefaultHeritrixLauncher getInstance(HeritrixFiles files) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(files, "HeritrixFiles files");
        return new DefaultHeritrixLauncher(files);
    }

    /**
     * This method launches heritrix in the following way:</br> 1. copies the orderfile and the seedsfile to current
     * working directory. </br> 2. sets up the newly created copy of the orderfile </br> 3. starts the crawler </br> 4.
     * stops the crawler (Either when heritrix has finished crawling, or when heritrix is forcefully stopped due to
     * inactivity). </p> The exit from the while-loop depends on Heritrix calling the crawlEnded() method, when the
     * crawling is finished. This method is called from the HarvestControllerServer.onDoOneCrawl() method.
     *
     * @throws IOFailure - if the order.xml is invalid if unable to initialize Heritrix CrawlController if Heritrix
     * process interrupted
     */
    public void doCrawl() throws IOFailure {
        setupOrderfile(getHeritrixFiles());
        heritrixController = HeritrixControllerFactory.getDefaultHeritrixController(getControllerArguments());
        try {
            // Initialize Heritrix settings according to the order.xml
            heritrixController.initialize();
            log.debug("Starting crawl..");
            heritrixController.requestCrawlStart();
            if (heritrixController.atFinish()) {
                heritrixController.beginCrawlStop();
            } else {
                doCrawlLoop();
            }
        } catch (IOFailure e) {
            log.warn("Error during initialisation of crawl", e);
            throw (e);
        } catch (Exception e) {
            log.warn("Exception during crawl", e);
            throw new RuntimeException("Exception during crawl", e);
        } finally {
            if (heritrixController != null) {
                heritrixController.cleanup();
            }
        }
        log.debug("Heritrix is finished crawling...");
    }

    /**
     * Monitors the crawling performed by Heritrix. Regularly checks whether any progress is made. If no progress has
     * been made for too long, the crawl is ended.
     *
     * @throws IOFailure if the call to HeritrixController.requestCrawlStop() fails. Other failures in calls to the
     * controller are caught and logged.
     */
    private void doCrawlLoop() throws IOFailure {
        String errorMessage = "Non-fatal I/O error while communicating with Heritrix during crawl";
        long lastNonZeroActiveQueuesTime = System.currentTimeMillis();
        long lastTimeReceivedData = System.currentTimeMillis();
        boolean crawlIsEnded = false;
        try {
            crawlIsEnded = heritrixController.crawlIsEnded();
        } catch (IOFailure e) {
            log.debug(errorMessage, e);
        }
        while (!crawlIsEnded) {
            String harvestInformation = null;
            String progressStats = null;
            try {
                harvestInformation = heritrixController.getHarvestInformation();
                progressStats = heritrixController.getProgressStats();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }

            HeritrixFiles files = getHeritrixFiles();
            log.info("Job ID: {}, Harvest ID: {}, {}\n{}", files.getJobID(), files.getHarvestID(), harvestInformation,
                    ((progressStats == null) ? "" : progressStats));
            // Note that we don't check for timeout while paused.
            int processedKBPerSec = 0;
            boolean paused = false;
            try {
                processedKBPerSec = heritrixController.getCurrentProcessedKBPerSec();
                paused = heritrixController.isPaused();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (processedKBPerSec > 0 || paused) {
                lastTimeReceivedData = System.currentTimeMillis();
            }
            int activeToeCount = 0;
            paused = false;
            try {
                activeToeCount = heritrixController.getActiveToeCount();
                paused = heritrixController.isPaused();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (activeToeCount > 0 || paused) {
                lastNonZeroActiveQueuesTime = System.currentTimeMillis();
            }
            if ((lastNonZeroActiveQueuesTime + timeOutInMillis < System.currentTimeMillis())
                    || (lastTimeReceivedData + timeOutInMillisReceivedData < System.currentTimeMillis())) {
                final double noActiveQueuesTimeoutInSeconds = timeOutInMillis / 1000.0;
                final double noDataReceivedTimeoutInSeconds = timeOutInMillisReceivedData / 1000.0;
                long queuedUriCount = 0;
                try {
                    queuedUriCount = heritrixController.getQueuedUriCount();
                } catch (IOFailure e) {
                    log.debug(errorMessage, e);
                }
                long ctm = System.currentTimeMillis();
                log.warn(
                        "Aborting crawl because of inactivity. No active queues for the last {} seconds "
                                + "(timeout is {} seconds).No traffic for the last {} seconds (timeout is {} seconds). URLs in queue:{}",
                        ((ctm - lastNonZeroActiveQueuesTime) / 1000.0), noActiveQueuesTimeoutInSeconds,
                        ((ctm - lastTimeReceivedData) / 1000.0), noDataReceivedTimeoutInSeconds, queuedUriCount);
                // The following is the only controller command exception we
                // don't catch here. Otherwise we might loop forever.
                heritrixController.requestCrawlStop("Aborting because of inactivity");
            }

            // Optimization: don't wait if ended since beginning of the loop
            try {
                crawlIsEnded = heritrixController.crawlIsEnded();
            } catch (IOFailure e) {
                log.debug(errorMessage, e);
            }
            if (!crawlIsEnded) {
                try {
                    /*
                     * Wait for heritrix to do something. WAIT_PERIOD is the interval between checks of whether we have
                     * passed timeouts. Note that timeouts are defined in the settings, while WAIT_PERIOD (being less
                     * relevant to the user) is defined in this class.
                     */
                    synchronized (this) {
                        wait(1000 * CRAWL_CONTROL_WAIT_PERIOD);
                    }
                } catch (InterruptedException e) {
                    log.trace("Waiting thread awoken: {}", e.getMessage(), e);
                }
            }
        } // end of while (!crawlIsEnded)
    }

}
