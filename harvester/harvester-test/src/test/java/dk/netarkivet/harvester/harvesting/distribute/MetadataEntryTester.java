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
package dk.netarkivet.harvester.harvesting.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.AliasInfo;
import dk.netarkivet.harvester.datamodel.Constants;
import dk.netarkivet.harvester.harvesting.metadata.MetadataEntry;

/**
 * Tests for class MetadataEntry. Note that this class is also tested in other test-classes.
 */
public class MetadataEntryTester {
    private String aRealURL;
    private String anEmptyURL;
    final private String aNullURL = null;

    private String aRealMimetype;
    private String anEmptyMimetype;
    final private String aNullMimetype = null;

    private String realData;
    final private String nullData = null;

    private String anInvalidUrl;
    private String anInvalidMimetype;

    @Before
    public void setUp() {
        aRealURL = "metadata://netarkivet.dk/crawl/" + "setup/order.xml?version=1.7.1&harvestid=1&jobid=1";
        aRealMimetype = "text/plain";
        realData = "bla bla bla bla bla bla bla bla " + "a bla bla bla bla bla bla bla bla bla bla bla";
        anEmptyURL = "";
        anEmptyMimetype = "";
        anInvalidUrl = "http:/aninvalidUrl";
        anInvalidMimetype = "textplain";
    }

    /**
     * Monkey Tests method for MetadataEntry constructor.
     */
    @Test
    public void testMetadataEntry() {

        // check, that null & empty arguments are not accepted. (1)
        try {
            new MetadataEntry(aNullURL, aRealMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that null & empty arguments are not accepted. (2)
        try {
            new MetadataEntry(anEmptyURL, aRealMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that null & empty arguments are not accepted. (3)
        try {
            new MetadataEntry(aRealURL, aNullMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that null & empty arguments are not accepted. (4)
        try {
            new MetadataEntry(aRealURL, anEmptyMimetype, realData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }
        // check, that null & empty arguments are not accepted. (5)
        try {
            new MetadataEntry(aRealURL, aRealMimetype, nullData);
            fail("ArgumentNotValid exception expected for null/empty arguments");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // check, that valid arguments are accepted.
        try {
            new MetadataEntry(aRealURL, aRealMimetype, realData);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected for valid arguments");
        }

        // Check, that invalid url throw ArgumentNotValid exception
        try {
            new MetadataEntry(anInvalidUrl, aRealMimetype, realData);
            fail("ArgumentNotValid exception expected for invalid URL");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        // Check, that invalid mimetype throw ArgumentNotValid exception
        try {
            new MetadataEntry(aRealURL, anInvalidMimetype, realData);
            fail("ArgumentNotValid exception expected for invalid mimetype");
        } catch (ArgumentNotValid e) {
            // Expected
        }

    }

    /**
     * Test method for getMethods.
     */
    @Test
    public void testGetterAndSetters() {
        MetadataEntry md = new MetadataEntry(aRealURL, aRealMimetype, realData);
        assertEquals("getData() returns wrong value", realData, new String(md.getData()));
        assertEquals("getURL() returns wrong value", aRealURL, md.getURL());
        assertEquals("getMimeType() returns wrong value", aRealMimetype, md.getMimeType());
    }

    /**
     * Test isDuplicateReductionMetadataEntry.
     */
    @Test
    public void testIsDuplicateReductionMetadataEntry() {
        MetadataEntry md = new MetadataEntry(
                "metadata://netarkivet.dk/crawl/setup/duplicatereductionjobs?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s",
                aRealMimetype, realData);
        MetadataEntry md1 = new MetadataEntry(
                "metadata://netarkivet.dk/crawl/setup/aliases?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s",
                aRealMimetype, realData);
        assertFalse("md1 should not be recognized as a duplicatereduction metadataEntry",
                md1.isDuplicateReductionMetadataEntry());
        assertTrue("md should be recognized as a duplicatereduction metadataEntry",
                md.isDuplicateReductionMetadataEntry());
    }

    /** Test toString method. */
    @Test
    public void testToString() {
        MetadataEntry md = new MetadataEntry(aRealURL, aRealMimetype, realData);
        String expectedToString = "URL= " + aRealURL + " ; mimetype= " + aRealMimetype + " ; data= " + realData;
        assertEquals("toString() returns unexpected result", expectedToString, md.toString());
    }

    /**
     * Test makeAliasMetadataEntry() returns null if the aliases in the list of aliases are expired.
     */
    @Test
    public void testMakeAliasMetadataEntryReturnsNullWithOnlyExpiredAliases() {
        Long origHarvestdefinitionId = 1L;
        Long jobId = 1L;
        int harvestNum = 1;
        List<AliasInfo> aliases = new ArrayList<AliasInfo>();
        Date expiredDate = new Date(System.currentTimeMillis() - (Constants.ALIAS_TIMEOUT_IN_MILLISECONDS + 10L));

        AliasInfo expiredAlias = new AliasInfo("netarkivet.dk", "alias.dk", expiredDate);
        assertTrue("The alias should be expired", expiredAlias.isExpired());
        aliases.add(expiredAlias);
        MetadataEntry md = MetadataEntry.makeAliasMetadataEntry(aliases, origHarvestdefinitionId, harvestNum, jobId);
        assertTrue("The returned MetadataEntry should be null", md == null);
    }

    /**
     * Test serializability.
     */
    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        // Take an object:

        MetadataEntry fooOriginal = new MetadataEntry(aRealURL, aRealMimetype, realData);
        // serialize and deserialize the study object:
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream ous = new ObjectOutputStream(baos);
        ous.writeObject(fooOriginal);
        ous.close();
        baos.close();
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        MetadataEntry fooCopy;
        fooCopy = (MetadataEntry) ois.readObject();
        // Finally, compare their visible states:
        assertEquals("After serialization the states differed:\n" + relevantState(fooOriginal) + "\n"
                + relevantState(fooCopy), relevantState(fooOriginal), relevantState(fooCopy));
    }

    private String relevantState(MetadataEntry fooOriginal) {
        return fooOriginal.getURL() + fooOriginal.getMimeType() + new String(fooOriginal.getData());
    }
}
