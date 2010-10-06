/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel;


import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.NotificationsFactory;

/**
 * A Data Access Object for harvest definitions.
 * This object is a singleton to ensure thread-safety. It
 * handles the transformation from harvest definitions to persistent storage.
 *
 */
public abstract class HarvestDefinitionDAO implements Iterable<HarvestDefinition> {

    /** The one and only instance of the HarvestDefinitionDAO class to ensure
      * thread-safety.
      */
    private static HarvestDefinitionDAO instance;
    /** The log. */
    protected final Log log = LogFactory.getLog(getClass());

    /** The set of HDs (or rather their OIDs) that are currently being
     * scheduled in a separate thread.
     * This set is a SynchronizedSet
     */
    private Set<Long> harvestDefinitionsBeingScheduled = Collections.synchronizedSet(new HashSet<Long>());

    /**
     * Default constructor.
     * Does not do anything, however.
     */
    protected HarvestDefinitionDAO() {
    }

    /**
     * Creates the singleton.
     * @return the HarvestDefinitionDAO singleton.
     * @throws IOFailure if unable to create the singleton.
     */
    public static synchronized HarvestDefinitionDAO getInstance() {
        if (instance == null) {
            instance = new HarvestDefinitionDBDAO();
        }
        return instance;
    }

    /**
     * Create a harvest definition in persistent storage.
     *
     * @param harvestDefinition A new harvest definition to write out.
     * @return The harvestId for the just created harvest definition.
     */
    public abstract Long create(HarvestDefinition harvestDefinition);

    /**
     * Generates the next id of a harvest definition.
     * TODO: Maybe this method is not needed in this superclass as an abstract method.
     * It is really only a helper method for the create() method.
     * @return id The next available id.
     */
    protected abstract Long generateNextID();

    /**
     * Read the stored harvest definition for the given ID.
     *
     * @param harvestDefinitionID An ID number for a harvest definition
     * @return A harvest definition that has been read from persistent storage.
     * @throws UnknownID if no file with that ID exists
     * @throws IOFailure if the File does not exist, does not have the
     *                   correct ID, or
     *                   otherwise fails to load correctly.
     */
    public abstract HarvestDefinition read(Long harvestDefinitionID)
            throws UnknownID, IOFailure;

    /** Return a string describing the current uses of a harvest definition,
     * or null if the harvest definition is safe to delete (i.e. has never been
     * run).
     *
     * @param oid a given identifier designating a harvest definition.
     * @return the above mentioned usage-string.
     */
    public abstract String describeUsages(Long oid);

    /**
     * Delete a harvest definition from persistent storage.
     *
     * @param oid The ID of a harvest definition to delete.
     */
    public abstract void delete(Long oid);

    /**
     * Update an existing harvest definition with new info
     * in persistent storage.
     *
     * @param harvestDefinition An updated harvest definition
     *  object to be persisted.
     */
    public abstract void update(HarvestDefinition harvestDefinition);

    /**
     * Check, if there exists a HarvestDefinition identified by a given OID.
     * @param oid a given OID
     * @return true, if such a harvestdefinition exists.
     */
    public abstract boolean exists(Long oid);

    /**
     * Get a list of all existing harvest definitions.
     *
     * @return An iterator that give the existing harvest definitions in turn
     */
    public abstract Iterator<HarvestDefinition> getAllHarvestDefinitions();

    /** Get an iterator of all harvest definitions.
     * Implements the Iterable interface.
     *
     * @return Iterator of all harvest definitions, Selective and Full both.
     */
    public Iterator<HarvestDefinition> iterator() {
        return getAllHarvestDefinitions();
    }

    /**
     * Gets default configurations for all domains.
     *
     * @return Iterator containing the default DomainConfiguration
     * for all domains
     */
    public abstract Iterator<DomainConfiguration> getSnapShotConfigurations();



    /**
     * Edit the harvestdefinition.
     * @param harvestDefinitionID The ID for the harvestdefintion to be updated
     * @param harvestDefinition the HarvestDefinition object to used to update
     *        harvestdefinition with the above ID
     * @return the ID for the updated harvestdefinition
     * (this should be equal to harvestDefinitionID?)
     *
     */
    public Long editHarvestDefinition(Long harvestDefinitionID,
                                      HarvestDefinition harvestDefinition) {
        ArgumentNotValid.checkNotNull(harvestDefinitionID,
                "harvestDefinitionID");
        ArgumentNotValid.checkNotNull(harvestDefinition, "harvestDefinition");

        HarvestDefinition oldhd = read(harvestDefinitionID);
        harvestDefinition.setOid(oldhd.getOid());
        harvestDefinition.setSubmissionDate(oldhd.getSubmissionDate());
        update(harvestDefinition);
        return harvestDefinition.getOid();
    }

    /**
     * Generate new jobs for the harvestdefinitions in persistent storage.
     * @param now The current time (hopefully)
     */
    public void generateJobs(Date now) {
        ArgumentNotValid.checkNotNull(now, "now");
        // loop over all harvestdefinitions that are ready to run
        for (final Long id : getReadyHarvestDefinitions(now)) {
            // The synchronization must take place within the loop, as we don't
            // want to lock everybody out for the entire time.
            synchronized(this) {
                // Make every HD run in its own thread, but at most once.
                if (harvestDefinitionsBeingScheduled.contains(id)) {
                    // With the small importance of this logmessage, 
                    // we won't spend time looking up the corresponding name for
                    // the harvestdefinition with this id number.
                    log.debug("Not creating jobs for harvestdefinition with id #" + id
                            + " as the previous scheduling is still running");
                    continue;
                }

                // Get all heritrix-jobs that
                // the harvestdefinition consists of !
                final HarvestDefinition harvestDefinition = read(id);

                if (!harvestDefinition.runNow(now)) {
                    log.trace("The harvestdefinition '"
                            +  harvestDefinition.getName()
                            + "' should not run now.");
                    log.trace("numEvents: "
                            + harvestDefinition.getNumEvents());
                    continue;
                }
                harvestDefinitionsBeingScheduled.add(id);
                // create the jobs
                final Thread t = new Thread() {
                    public void run() {
                        try {
                            int jobsMade = harvestDefinition.createJobs();
                            log.info("Created " + jobsMade
                                    + " jobs for harvest definition '"
                                    + harvestDefinition.getName() + "'");
                            update(harvestDefinition);
                        } catch (Throwable e) {
                            try {
                                HarvestDefinition hd
                                        = read(harvestDefinition.getOid());
                                hd.setActive(false);
                                update(hd);
                                String errMsg = "Exception while scheduling"
                                        + "harvestdefinition '"
                                        + harvestDefinition.getName()
                                        + "'. The harvestdefinition has been"
                                        + " deactivated!";
                                log.warn(errMsg, e);
                                NotificationsFactory.getInstance()
                                    .errorEvent(errMsg, e);
                            } catch (Exception e1) {
                                String errMsg = "Exception while scheduling"
                                        + "harvestdefinition '"
                                        + harvestDefinition.getName()
                                        + "'. The harvestdefinition couldn't be"
                                        + " deactivated!";
                                log.warn(errMsg, e);
                                log.warn("Unable to deactivate", e1);
                                NotificationsFactory.getInstance()
                                    .errorEvent(errMsg, e);

                            }
                        } finally {
                            harvestDefinitionsBeingScheduled.
                                remove(id);
                            log.debug("Removed '" + harvestDefinition.getName()
                                    + "' from list of harvestdefinitions to be "
                                    + "scheduled. Harvestdefinitions still to "
                                    + "be scheduled: "
                                    + harvestDefinitionsBeingScheduled);
                        }
                    }
                };
                t.start();
            }
        }
    }

    /** Get the IDs of the harvest definitions that are ready to run.
     * 
     * @param now
     * @return IDs of the harvest definitions that are currently ready to
     * be scheduled.  Some of these might already be in the process of being
     * scheduled.
     */
    public abstract Iterable<Long> getReadyHarvestDefinitions(Date now);

    /**
     * Get the harvest definition that has the given name, or null,
     * if no harvestdefinition exist with this name.
     * @param name The name of a harvest definition.
     * @return The HarvestDefinition object with that name, or null if none
     * has that name.
     */
    public abstract HarvestDefinition getHarvestDefinition(String name);

    /** Returns an iterator of all snapshot harvest definitions.
     *
     * @return An iterator (possibly empty) of FullHarvests
     */
    public abstract Iterator<FullHarvest> getAllFullHarvestDefinitions();

    /** Returns an iterator of all non-snapshot harvest definitions.
     *
     * @return An iterator (possibly empty) of PartialHarvests
     */
    public abstract Iterator<PartialHarvest> getAllPartialHarvestDefinitions();

    /** Returns a list with information on the runs of a particular harvest.
     *
     * @param harvestID ID of an existing harvest
     * @return List of objects with selected information.
     */
    public abstract List<HarvestRunInfo> getHarvestRunInfo(long harvestID);

    /**
     * Reset the DAO instance. Only for use in tests.
     */
    static void reset() {
        instance = null;
    }

    /** Returns true if any harvestdefinition is in the middle of having
     * jobs scheduled.  Notice that this synchronizes with generateJobs.
     *
     * @return true if there is at least one harvestdefinition currently
     * scheduling jobs.
     */
    public boolean isGeneratingJobs() {
        return !harvestDefinitionsBeingScheduled.isEmpty();
    }

    /** Return whether the given harvestdefinition can be deleted.
     * This should be a fairly lightweight method, but is not likely to be
     * instantaneous.
     * Note that to increase speed, this method may rely on underlying systems
     * to enforce transitive invariants.  This means that if this method says
     * a harvestdefinition can be deleted, the dao may still reject a delete
     * request.  If this method returns false, deletion will however
     * definitely not be allowed.
     * @param hd A given harvestdefinition
     * @return true, if this HarvestDefinition can be deleted without problems.
     */
    public abstract boolean mayDelete(HarvestDefinition hd);

    /**
     * Get all domain,configuration pairs for a harvest definition in sparse
     * version for GUI purposes.
     *
     * @param harvestDefinitionID The ID of the harvest definition.
     * @return Domain,configuration pairs for that HD. Returns an empty iterable
     *         for unknown harvest definitions.
     * @throws ArgumentNotValid on null argument.
     */
    public abstract Iterable<SparseDomainConfiguration>
            getSparseDomainConfigurations(Long harvestDefinitionID);

    /**
     * Get a sparse version of a partial harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of partial harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparsePartialHarvest getSparsePartialHarvest(
            String harvestName);

    /**
     * Get all sparse versions of partial harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparsePartialHarvests
     */
    public abstract Iterable<SparsePartialHarvest>
            getAllSparsePartialHarvestDefinitions();

    /**
     * Get a sparse version of a full harvest for GUI purposes.
     *
     * @param harvestName Name of harvest definition.
     * @return Sparse version of full harvest or null for none.
     * @throws ArgumentNotValid on null or empty name.
     */
    public abstract SparseFullHarvest getSparseFullHarvest(String harvestName);

    /**
     * Get all sparse versions of full harvests for GUI purposes.
     *
     * @return An iterable (possibly empty) of SparseFullHarvests
     */
    public abstract Iterable<SparseFullHarvest>
            getAllSparseFullHarvestDefinitions();
}