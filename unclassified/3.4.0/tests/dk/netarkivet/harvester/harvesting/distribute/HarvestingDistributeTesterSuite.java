/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;

import junit.framework.Test;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

public class HarvestingDistributeTesterSuite
{
    public static Test suite()
    {
        TestSuite suite;
        suite = new TestSuite("DistributeHarvestSchedulerTesterSuite");
        addToSuite(suite);
        return suite;
    }

    public static void addToSuite(TestSuite suite) {
        suite.addTestSuite(CrawlStatusMessageTester.class);
        suite.addTestSuite(DoOneCrawlMessageTester.class);
        suite.addTestSuite(HarvestControllerServerTester.class);
        suite.addTestSuite(HarvestControllerClientTester.class);
        suite.addTestSuite(MetadataEntryTester.class);
        suite.addTestSuite(PersistentJobDataTester.class);
    }

    public static void main(String args[])
    {
        String args2[] = {"-noloading", "dk.netarkivet.distribute.scheduler.DistributeHarvestSchedulerTesterSuite"};

        TestRunner.main(args2);
    }
}