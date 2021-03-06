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

import java.io.File;

import dk.netarkivet.common.utils.FileUtils;

/**
 * This testing tool runs a single harvest similar to what would happen when a Job arrives. It takes as arguments the
 * name of an order.xml file and a seeds.txt file. The order.xml file must not use deduplication. A crawldir is created
 * in the current directory. A settings.xml file is required in a directory named 'conf'
 */
public class StandaloneHarvester {
    public static void main(String[] argv) {
        if (argv.length != 2 || !new File(argv[0]).exists() || !new File(argv[1]).exists()) {
            System.out.println("Usage: java " + StandaloneHarvester.class.getName()
                    + " <order.xml file> <seeds.txt file>");
            System.exit(1);
        }
        File crawlDir = new File("CrawlDir" + System.currentTimeMillis());
        crawlDir.mkdirs();
        FileUtils.copyFile(new File(argv[0]), new File(crawlDir, "order.xml"));
        FileUtils.copyFile(new File(argv[1]), new File(crawlDir, "seeds.txt"));
        System.out.println("Attempting crawl in " + crawlDir);
        // Hardwired to H1
        HeritrixFiles files = HeritrixFiles.getH1HeritrixFilesWithDefaultJmxFiles(
        		crawlDir, new JobInfoTestImpl(1L, 1L));
        
        
        HeritrixLauncher launcher = HeritrixLauncherFactory.getInstance(files);
        launcher.doCrawl();
        System.out.println("Crawl ended, results are in " + crawlDir);
    }
}
