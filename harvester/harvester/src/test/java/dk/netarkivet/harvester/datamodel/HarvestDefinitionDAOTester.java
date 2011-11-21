/*$Id$
 *$Revision$
 *$Author$
 *$Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2011 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;

/**
 * Unit tests for the class HarvestDefinitionDAO class.
 */
public class HarvestDefinitionDAOTester extends DataModelTestCase {
    public HarvestDefinitionDAOTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Check that creation of a new HarvestDefinition instance succeeds.
     */
    public void testCreateAndRead() {
        HarvestDefinitionDAO hdDAO = HarvestDefinitionDAO.getInstance();

        /* Test with PartialHarvest */
        PartialHarvest harvestDef = makePartialHarvestInstance();
        harvestDef.setSubmissionDate(new Date(1000 * (System
                .currentTimeMillis() / 1000)));
        harvestDef.setNextDate(new Date(
                1000 * (System.currentTimeMillis() / 1000)));
        harvestDef.setNumEvents(7);
        hdDAO.create(harvestDef);

        HarvestDefinitionDAO hdDAO2 = HarvestDefinitionDAO.getInstance();
        PartialHarvest harvestDef2 = (PartialHarvest) hdDAO2.read(harvestDef
                .getOid());

        /* Verify that saved and loaded data identical */
        assertEquals("Retrieved data must match stored data", harvestDef
                .getName(), harvestDef2.getName());
        assertEquals("Retrieved data must match stored data", harvestDef
                .getComments(), harvestDef2.getComments());
        assertEquals("Retrieved data must match stored data", harvestDef
                .getSubmissionDate(), harvestDef2.getSubmissionDate());
        CollectionAsserts.assertIteratorNamedEquals(
                "Retrieved data must match stored data", harvestDef
                .getDomainConfigurations(), harvestDef2
                .getDomainConfigurations());

        assertEquals("Retrieved data must match stored data", harvestDef
                .getSchedule(), harvestDef2.getSchedule());
        assertEquals("Retrieved data must match stored data", harvestDef
                .getNextDate(), harvestDef2.getNextDate());
        assertEquals("Retrieved data must match stored data", harvestDef
                .getNumEvents(), harvestDef2.getNumEvents());

        /* Test with FullHarvest */
        FullHarvest fHD1 = HarvestDefinition.createFullHarvest(
                "Full Harvest 1", "Test of full harvest", null, 2000,
                Constants.DEFAULT_MAX_BYTES, 
                Constants.DEFAULT_MAX_JOB_RUNNING_TIME);
        fHD1.setSubmissionDate(new Date(
                1000 * (System.currentTimeMillis() / 1000)));
        fHD1.setNumEvents(7);
        hdDAO.create(fHD1);

        FullHarvest fHD2 = HarvestDefinition.createFullHarvest(
                "Full Harvest 2", "Test of full harvest", fHD1.getOid(), 2000,
                Constants.DEFAULT_MAX_BYTES, Constants.DEFAULT_MAX_JOB_RUNNING_TIME);
        fHD2.setSubmissionDate(new Date(
                1000 * (System.currentTimeMillis() / 1000)));
        fHD2.setNumEvents(7);
        hdDAO.create(fHD2);

        FullHarvest fHD2_2 = (FullHarvest) hdDAO2.read(fHD2.getOid());

        /* Verify that saved and loaded data identical */
        assertEquals("Retrieved data must match stored data", fHD2.getName(),
                     fHD2_2.getName());
        assertEquals("Retrieved data must match stored data", fHD2
                .getComments(), fHD2_2.getComments());
        assertEquals("Retrieved data must match stored data", fHD2
                .getSubmissionDate(), fHD2_2.getSubmissionDate());
        CollectionAsserts.assertIteratorNamedEquals(
                "Retrieved data must match stored data", fHD2
                .getDomainConfigurations(), fHD2_2
                .getDomainConfigurations());
        assertEquals("Retrieved data must match stored data", fHD2
                .getNumEvents(), fHD2_2.getNumEvents());
        assertEquals("Retrieved data must match stored data", fHD2
                .getPreviousHarvestDefinition(), fHD2_2
                .getPreviousHarvestDefinition());
    }

    /**
     * Verify that updating an already modified harvestdefinition throws an
     * exception.
     */
    public void testOptimisticLocking() {
        HarvestDefinitionDAO hdDAO = HarvestDefinitionDAO.getInstance();

        // create the definition
        HarvestDefinition harvestDef = makePartialHarvestInstance();
        harvestDef.setSubmissionDate(new Date(1000 * (System
                .currentTimeMillis() / 1000)));
        hdDAO.create(harvestDef);

        // load 2 instances of the definition
        HarvestDefinitionDAO hdDAO2 = HarvestDefinitionDAO.getInstance();
        HarvestDefinition harvestDef2 = hdDAO2.read(harvestDef.getOid());
        HarvestDefinition harvestDef3 = hdDAO2.read(harvestDef.getOid());

        assertEquals("The 2 instances should be identical editions",
                     harvestDef2.getEdition(), harvestDef3.getEdition());

        // the first update should succeed
        // Notice that the edition number is incremented even though no changes
        // were actually made to the object
        hdDAO2.update(harvestDef2);

        try {
            hdDAO2.update(harvestDef3);
            fail("The edition number of harvestDef3 has expired and the update should fail");
        } catch (PermissionDenied e) {
            // expected
        }
    }

    /**
     * Check updating of an existing entry.
     */
    public void testUpdate() {
        HarvestDefinitionDAO hdDAO = HarvestDefinitionDAO.getInstance();
        Long oid = Long.valueOf(42);
        HarvestDefinition hd = hdDAO.read(oid);
        assertEquals("Name should be the original before changing",
                     "Testhøstning", hd.getName());
        assertEquals("Comments should be the original before changing",
                     "Test dette", hd.getComments());
        assertTrue("Expecting active definition (default)", hd.getActive());

        hd.setComments("Nu ny og bedre");
        hd.setActive(false);

        // Set the date to one month later
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(hd.getSubmissionDate());
        cal.add(Calendar.MONTH, 1);
        Date testDate = cal.getTime();
        int numEvents = -1;

        if (!hd.isSnapShot()) {
            // If not a snapshot harvest, it is a partial harvest:
            ((PartialHarvest) hd).setNextDate(testDate);
        }
        // Set the number of events to one more
        numEvents = hd.getNumEvents() + 1;
        hd.setNumEvents(numEvents);

        hdDAO.update(hd);

        HarvestDefinition hd2 = hdDAO.read(oid);
        assertEquals("Name should be the same after changing", "Testhøstning",
                     hd2.getName());
        assertEquals("Comments should be changed after changing",
                     "Nu ny og bedre", hd2.getComments());
        assertFalse("Changed to inactive", hd2.getActive());

        if (!hd.isSnapShot()) {
            // Check that date was changed
            assertEquals("nextDate should be changed after changing", testDate,
                         ((PartialHarvest) hd2).getNextDate());
        }
        // Check that number of events were changed
        assertEquals("numEvents should be changed after changing", numEvents,
                     hd2.getNumEvents());
    }

    /**
     * Test for bug #120: HD's seem to be missing after creating a new HD.
     */
    public void testMissingHarvestDefinitionAfterCreate() {
        HarvestDefinitionDAO hdDAO = HarvestDefinitionDAO.getInstance();

        HarvestDefinition harvestDef = makePartialHarvestInstance();
        harvestDef.setSubmissionDate(new Date(1000 * (System
                .currentTimeMillis() / 1000)));
        hdDAO.create(harvestDef);

        Iterator<HarvestDefinition> i = hdDAO.getAllHarvestDefinitions();
        List<HarvestDefinition> l = new ArrayList<HarvestDefinition>();
        while (i.hasNext()) {
            l.add(i.next());
        }
        assertEquals(
                "Should find the harvest definitions available (2 old, 1 new) in "
                + l, 3, l.size());
    }

    private PartialHarvest makePartialHarvestInstance() {
        Domain wd = TestInfo.getDefaultDomain();
        DomainConfiguration cfg1 = TestInfo.getDefaultConfig(wd);
        // cfg1.addSeedList(wd.getSeedList(TestInfo.SEEDLISTNAME));
        wd.addConfiguration(cfg1);
        cfg1.setOrderXmlName(TestInfo.ORDER_XML_NAME);

        List<DomainConfiguration> webDomainConfigs
                = new ArrayList<DomainConfiguration>();
        webDomainConfigs.add(cfg1);

        DomainDAO.getInstance().create(wd);

        ScheduleDAO scheduledao = ScheduleDAO.getInstance();
        Schedule schedule = scheduledao.read("DefaultSchedule");

        return HarvestDefinition.createPartialHarvest(webDomainConfigs,
                                                      schedule, "Election 2005",
                                                      "Event harvest");
    }

    /**
     * Reset the current instance of the DAO.
     */
    public static void resetDAO() {
        HarvestDefinitionDAO.reset();
    }

    public void testGetHarvestDefinition() throws Exception {
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();
        try {
            dao.getHarvestDefinition(null);
            fail("Should throw exception on null");
        } catch (ArgumentNotValid e) {
            // expected
        }
        HarvestDefinition hd1 = dao.getHarvestDefinition("Testhøstning");
        HarvestDefinition hd2 = dao.read(Long.valueOf(42L));
        assertEquals("Should read correct harvest defintition", hd2, hd1);
        hd2 = dao.read(Long.valueOf(43L));
        hd1 = dao.getHarvestDefinition("Tværhøstning");
        assertEquals("Should read correct harvest defintition", hd2, hd1);
        hd1 = dao.getHarvestDefinition("Ukendt");
        assertNull("Should get null on no known harvest definition", hd1);
    }

    /**
     * Test that we obey the editions and doesn't allow update of non-existing HDs.
     * Tests bug #468 as well.
     */
    public void testUpdateEditions() {
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();

        HarvestDefinition hd1 = dao.read(42L);
        HarvestDefinition hd2 = dao.read(42L);

        // Test concurrent updates
        hd1.setNumEvents(hd1.getNumEvents() + 1);
        hd2.setNumEvents(hd2.getNumEvents() + 23);

        dao.update(hd1);

        HarvestDefinition hd3 = dao.read(42L);
        assertEquals("HD should have updated numevents", 1, hd3.getNumEvents());

        try {
            dao.update(hd2);
            fail("Should have refused to update with old edition");
        } catch (PermissionDenied e) {
            // Expected
        }

        // Check that no rollback errors were logged
        LogUtils.flushLogs(HarvestDefinitionDBDAO.class.getName());
        FileAsserts.assertFileNotContains("Log contains file after storing.",
                                          TestInfo.LOG_FILE, "rollback");

        // Check that you cannot update a non-existing HD.
        HarvestDefinition newhd = new PartialHarvest(TestInfo
                .getAllDefaultConfigurations(), TestInfo.getDefaultSchedule(),
                                                "notfound", "");
        try {
            dao.update(newhd);
            fail("Should not allow update of non-existing HD");
        } catch (PermissionDenied e) {
            // Expected
        }
    }

    /**
     * Test that HDs with null next date can be read and updated. Test for bug
     * #478.
     */
    public void testNullNextDate() {
        HarvestDefinitionDAO dao = HarvestDefinitionDAO.getInstance();

        PartialHarvest hd1 = (PartialHarvest) dao.read(42L);

        hd1.setNextDate(null);

        dao.update(hd1);

        PartialHarvest hd2 = (PartialHarvest) dao.read(42L);

        assertEquals("Should be the same HD before and after saving", hd1, hd2);
    }

    public void testGetHarvestRunInfo() throws Exception {
        // enforce migration of domain database.
        DomainDAO dao = DomainDAO.getInstance();
        assertNotNull("dao should never be null", dao);
        // Migrate
        TemplateDAO tdao = TemplateDAO.getInstance();
        assertNotNull("tdao should never be null", tdao);
        ScheduleDAO sdao = ScheduleDAO.getInstance();
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        JobDAO jdao = JobDAO.getInstance();

        List<DomainConfiguration> dcs = IteratorUtils.toList(hddao
                .getSnapShotConfigurations());
        HarvestDefinition newHd = HarvestDefinition.createPartialHarvest(
                dcs,
                sdao.read("DefaultSchedule"),
                "RunInfo",
                "");
        newHd.setActive(false); // Can't have it doing stuff behind our backs
        hddao.create(newHd);

        Job j1 = addRunInfo(newHd, 2, "netarkivet.dk", 10, 100, JobStatus.DONE);
        Job j2 = addRunInfo(newHd, 2, "dr.dk", 11, 100, JobStatus.DONE);
        j1.setActualStart(getDate(2005, 2, 13));
        j1.setActualStop(getDate(2005, 2, 18));
        jdao.update(j1);
        j2.setActualStart(getDate(2005, 2, 11));
        j2.setActualStop(getDate(2005, 2, 17));
        jdao.update(j2);
        addRunInfo(newHd, 3, "netarkivet.dk", 12, 100, JobStatus.DONE);
        addRunInfo(newHd, 3, "statsbiblioteket.dk", 13, 100, JobStatus.FAILED);

        // This run only has unfinished jobs
        Job j3 = newHd.getNewJob(dcs.get(0));
        j3.setHarvestNum(4);
        j3.setStatus(JobStatus.SUBMITTED);
        jdao.create(j3);

        // This run has a mix of finished and unfinished
        j2 = addRunInfo(newHd, 5, "kb.dk", 17, 100, JobStatus.DONE);
        j2.setActualStart(getDate(2005, 2, 11));
        j2.setActualStop(getDate(2005, 2, 17));
        jdao.update(j2);

        // An unfinished job
        j3 = newHd.getNewJob(dcs.get(0));
        j3.setHarvestNum(5);
        j3.setStatus(JobStatus.STARTED);
        jdao.create(j3);

        // Should have no info for unharvested HD
        List<HarvestRunInfo> runInfos = hddao.getHarvestRunInfo(43L);
        assertEquals("Should have no runinfos on unrun hd, not " + runInfos, 0,
                     runInfos.size());

        runInfos = hddao.getHarvestRunInfo(newHd.getOid());
        assertEquals("Should have info on 4 runs", 4, runInfos.size());

        // Sanity checks
        
        for (HarvestRunInfo runInfo : runInfos) {
            assertEquals("Should be for hd " + newHd, (Long) newHd.getOid(),
                         (Long) runInfo.getHarvestID());
            assertEquals("Should have right HD name ", newHd.getName(), runInfo
                    .getHarvestName());
            assertEquals("Job states should sum up", runInfo.getJobCount(),
                         runInfo.getJobCount(JobStatus.NEW)
                         + runInfo.getJobCount(JobStatus.SUBMITTED)
                         + runInfo.getJobCount(JobStatus.STARTED)
                         + runInfo.getJobCount(JobStatus.DONE)
                         + runInfo.getJobCount(JobStatus.FAILED)
                         + runInfo.getJobCount(JobStatus.RESUBMITTED));
            assertFalse("Should not have end date without start date", 
                    runInfo.getStartDate() == null 
                    && runInfo.getEndDate() != null);
            assertTrue("If done, stop should be after start", 
                    runInfo.getEndDate() == null 
                    || runInfo.getStartDate().before(runInfo.getEndDate()));
        }

        HarvestRunInfo i2 = runInfos.get(3);
        assertEquals("Should have 21 docs", 21, i2.getDocsHarvested());
        assertEquals("Should have 200 bytes", 200, i2.getBytesHarvested());
        assertEquals("Should have 2 done jobs", 2, i2
                .getJobCount(JobStatus.DONE));
        assertEquals("Should have no failed jobs", 0, i2
                .getJobCount(JobStatus.FAILED));
        assertEquals("Should have 2 jobs total", 2, i2.getJobCount());
        assertEquals("Should start at minimum date", getDate(2005, 2, 11), i2
                .getStartDate());
        assertEquals("Should end at maximum date", getDate(2005, 2, 18), i2
                .getEndDate());

        HarvestRunInfo i4 = runInfos.get(1);
        assertEquals("Should have 1 jobs total", 1, i4.getJobCount());
        assertEquals("One job should be submitted", 1, i4
                .getJobCount(JobStatus.SUBMITTED));
        assertNull("Should not be marked as started", i4.getStartDate());
        assertNull("Should not be marked as ended", i4.getEndDate());

        HarvestRunInfo i5 = runInfos.get(0);
        assertEquals("Should have 2 jobs total", 2, i5.getJobCount());
        assertEquals("One job should be started", 1, i5
                .getJobCount(JobStatus.STARTED));
        assertEquals("Should start at minimum date", getDate(2005, 2, 11), i5
                .getStartDate());
        assertNull("Should not be marked as ended", i5.getEndDate());
    }

    /**
     * Add some hokey harvest info for a new job.
     *
     * @param hd
     * @param run
     * @param domain
     * @param docs
     * @param bytes  Number of bytes collected.
     * @param status What status the job should have
     * @return the newly created job
     */
    private Job addRunInfo(HarvestDefinition hd, int run, String domain,
                           int docs, int bytes, JobStatus status) {
        final DomainDAO ddao = DomainDAO.getInstance();
        Domain d = ddao.read(domain);
        final DomainConfiguration dc = d.getDefaultConfiguration();
        Job j = hd.getNewJob(dc);
        j.setHarvestNum(run);
        JobDAO jdao = JobDAO.getInstance();
        jdao.create(j);

        j.setStatus(status);
        jdao.update(j);

        d.getHistory().addHarvestInfo((new HarvestInfo(hd.getOid(), j.getJobID(), domain, dc
                .getName(), new Date(), bytes, docs,
                            StopReason.DOWNLOAD_COMPLETE)));

        d.updateConfiguration(dc);
        ddao.update(d);
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        hddao.update(hd);
        return j;
    }

    public void testGetSparseDomainConfigurations() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        try {
            hddao.getSparseDomainConfigurations(null);
            fail("should throw ArgumentNotValid on null");
        } catch (ArgumentNotValid e) {
            // expected
        }

        Iterator<SparseDomainConfiguration> it = hddao
                .getSparseDomainConfigurations(12345L).iterator();
        assertFalse("Should return empty iterator on unknown HD", it.hasNext());
        it = hddao.getSparseDomainConfigurations(42L).iterator();
        assertTrue("Should return the config from known HD", it.hasNext());
        SparseDomainConfiguration sdc = it.next();
        assertEquals("Should be the right domain", "netarkivet.dk", sdc
                .getDomainName());
        assertEquals("Should be the right config", "Dansk_netarkiv_fuld_dybde",
                     sdc.getConfigurationName());
        assertFalse("Should return no more configs", it.hasNext());
    }

    public void testGetSparsePartialHarvest() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        SparsePartialHarvest sph = hddao
                .getSparsePartialHarvest("Testhøstning");
        assertTrue("Should find a SparsePartialHarvest for harvestname: "
                   + "Tværhøstning", sph != null);
        assertEquals("Should be the right partial harvest", Long.valueOf(42L), 
                sph.getOid());
        assertEquals("Should be the right partial harvest", "Testhøstning",
                     sph.getName());
        assertEquals("Should be the right partial harvest", true, sph
                .isActive());
        assertEquals("Should be the right partial harvest", 1,
                     sph.getEdition());
        assertEquals("Should be the right partial harvest", null, sph
                .getNextDate());
        assertEquals("Should be the right partial harvest", 0, sph
                .getNumEvents());
        assertEquals("Should be the right partial harvest", "DefaultSchedule",
                     sph.getScheduleName());
        assertEquals("Should be the right partial harvest", 1129043502426L, sph
                .getSubmissionDate().getTime());
        assertNull("Should be null on unknown", hddao
                .getSparsePartialHarvest("Fnord"));
    }

    public void testGetAllSparsePartialHarvestDefinitions() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        Iterator<SparsePartialHarvest> it = hddao
                .getAllSparsePartialHarvestDefinitions().iterator();
        assertTrue("Should return iterator of known partial HDs", it.hasNext());
        SparsePartialHarvest sph = it.next();
        assertEquals("Should be the right partial harvest", Long.valueOf(42L), 
                sph.getOid());
        assertEquals("Should be the right partial harvest", "Testhøstning",
                     sph.getName());
        assertEquals("Should be the right partial harvest", true, sph
                .isActive());
        assertEquals("Should be the right partial harvest", 1,
                     sph.getEdition());
        assertEquals("Should be the right partial harvest", null, sph
                .getNextDate());
        assertEquals("Should be the right partial harvest", 0, sph
                .getNumEvents());
        assertEquals("Should be the right partial harvest", "DefaultSchedule",
                     sph.getScheduleName());
        assertEquals("Should be the right partial harvest", 1129043502426L, sph
                .getSubmissionDate().getTime());
        assertFalse("Should return no more hds", it.hasNext());
    }

    public void testGetSparseFullHarvest() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        final String harvestName = "Tværhøstning";
        SparseFullHarvest sph = hddao.getSparseFullHarvest(harvestName);
        assertTrue("Should find a SparseFullHarvest for harvestname: "
                   + harvestName, sph != null);
        assertEquals("Should be the right full harvest", Long.valueOf(43L), sph
                .getOid());
        assertEquals("Should be the right full harvest", harvestName,
                     sph.getName());
        assertEquals("Should be the right full harvest", true, sph
                .isActive());
        assertEquals("Should be the right full harvest", 1,
                     sph.getEdition());
        assertEquals("Should be the right full harvest", 0, sph
                .getNumEvents());
        assertEquals("Should be the right object limit", 2000L, sph
                .getMaxCountObjects());
        assertEquals("Should be the right byte limit",
                     500000000, sph.getMaxBytes());
        assertEquals("Should be the right previous harvest", null, sph
                .getPreviousHarvestDefinitionOid());
        assertEquals("Should be the right full harvest", "Test dette",
                sph.getComments());
        
        
        assertNull("Should be null on unknown harvestdefinition", hddao
                .getSparseFullHarvest("Fnord"));
    }

    public void testGetAllSparseFullHarvestDefinitions() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        Iterator<SparseFullHarvest> it = hddao
                .getAllSparseFullHarvestDefinitions().iterator();
        assertTrue("Should return iterator of known full HDs", it.hasNext());
        SparseFullHarvest sph = it.next();
        assertEquals("Should be the right partial harvest", Long.valueOf(43L), 
                sph.getOid());
        assertEquals("Should be the right partial harvest", "Tværhøstning",
                     sph.getName());
        assertEquals("Should be the right partial harvest", true, sph
                .isActive());
        assertEquals("Should be the right partial harvest", 1,
                     sph.getEdition());
        assertEquals("Should be the right partial harvest", 0, sph
                .getNumEvents());
        assertEquals("Should be the right object limit", 2000L, sph
                .getMaxCountObjects());
        assertEquals("Should be the right byte limit",
                     500000000, sph.getMaxBytes());
        assertEquals("Should be the right previous harvest", null, sph
                .getPreviousHarvestDefinitionOid());
        assertFalse("Should return no more hds", it.hasNext());
    }

    /**
     * Get a Date(year, month, year). replaces deprecated Date constructor by this
     * private method.
     */
    private Date getDate(int year, int month, int day) {
        Calendar cal = new GregorianCalendar(year, month, day);
        return cal.getTime();
    }

    public void testGetHarvestName() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        assertEquals("Should find Tværhøstning for ID 42",
                     hddao.read(42L).getName(), hddao.getHarvestName(42L));
        assertEquals("Should find Tværhøstning for ID 43",
                     hddao.read(43L).getName(), hddao.getHarvestName(43L));
        try {
            hddao.getHarvestName(44L);
            fail("Should have thrown UnknownID on missing HD");
        } catch (UnknownID e) {
            // Expected
        }
        try {
            hddao.getHarvestName(null);
            fail("Should have thrown ArgumentNotValid on null id");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }

    public void testIsSnapshot() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        assertFalse("Should find selective harvest for ID 42",
                     hddao.isSnapshot(42L));
        assertTrue("Should find snapshot harvest for ID 43",
                     hddao.isSnapshot(43L));
        try {
            hddao.isSnapshot(44L);
            fail("Should have thrown UnknownID on missing HD");
        } catch (UnknownID e) {
            // Expected
        }
        try {
            hddao.isSnapshot(null);
            fail("Should have thrown ArgumentNotValid on null id");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
    
    public void testGetDomains() throws Exception {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();        
        List<String> domains = hddao.getListOfDomainsOfHarvestDefinition(
        		TestInfo.DEFAULT_HARVEST_NAME);
        assertTrue("List of domains in harvestdefinition '" 
        		+ TestInfo.DEFAULT_HARVEST_NAME +  "' shouldn't be null", 
        		domains != null);
        assertTrue("List of domains in harvestdefinition '" 
        		+ TestInfo.DEFAULT_HARVEST_NAME +  "' should be 0 but was "
        		+ domains.size(), domains.size() == 0);
    }
    
    /** Test both implementations of the exist function.
     * @throws Exception
     */
    public void testExists() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        long harvestexistsid = 43L;
        long harvestexistsnotid = 44L;
        String nameShouldExist = hddao.getHarvestName(harvestexistsid);
        String nameShouldNotExist = "Ciceros collected works";
        String nullName = null;
        String emptyName = "";
        assertTrue("Should exist harvest w/ name " + nameShouldExist,
                     hddao.exists(nameShouldExist));
        
        assertFalse("Should not exist harvest w/ name " + nameShouldNotExist,
                     hddao.exists(nameShouldNotExist));
        
        assertTrue("Should exist harvest w/ id " + harvestexistsid,
        hddao.exists(harvestexistsid));
        
        assertFalse("Should not exist harvest w/ id " + harvestexistsnotid,
                hddao.exists(harvestexistsnotid)); 
        
        try {
            hddao.exists(nullName);
            fail("Should throw ArgumentNotvalid when given null arg");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        try {
            hddao.exists(emptyName);
            fail("Should throw ArgumentNotvalid when given empty arg");
        } catch (ArgumentNotValid e) {
            // Expected
        }
    }
    
    /**
     *  Test the {@link HarvestDefinitionDAO#removeDomainConfiguration(
     *  PartialHarvest, DomainConfigurationKey)} method.
     */
    public void testRemoveDomainConfiguration() {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        long partialharvestId = 42L;
        HarvestDefinition hd = hddao.read(partialharvestId);
        if (!(hd instanceof PartialHarvest)) {
            fail("harvest w/ id=" + partialharvestId 
                    + " is not partialHarvest");
        }
        PartialHarvest ph = (PartialHarvest) hd;
        List<DomainConfiguration> configList =
            IteratorUtils.toList(ph.getDomainConfigurations());
        assertTrue("Should exist domainfigurations, but doesn't", 
                configList.size() > 0);
        int configsize = configList.size();
        DomainConfiguration dc = configList.get(0);
        hddao.removeDomainConfiguration(ph.getOid(), new SparseDomainConfiguration(dc));
        PartialHarvest ph1 = (PartialHarvest) hddao.read(partialharvestId);
        configList =
            IteratorUtils.toList(ph1.getDomainConfigurations());
        assertTrue("DC should have been removed", 
                configList.size() == configsize - 1);
        }
        
    /**
     *  Test the {@link HarvestDefinitionDAO#updateNextdate(
     *  PartialHarvest, Date)} method.
     */
    public void testUpdateNextDate() {
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        long partialharvestId = 42L;
        HarvestDefinition hd = hddao.read(partialharvestId);
        if (!(hd instanceof PartialHarvest)) {
            fail("harvest w/ id=" + partialharvestId 
                    + " is not partialHarvest");
        }
        PartialHarvest ph = (PartialHarvest) hd;
        Date now = new Date();
        hddao.updateNextdate(ph, now);
        PartialHarvest ph1 = (PartialHarvest) hddao.read(partialharvestId);
        assertTrue("The date should have been updated in the database", 
                ph1.getNextDate().equals(now));
        }     
}
