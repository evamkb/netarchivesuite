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

package dk.netarkivet.viewerproxy.webinterface;

import javax.servlet.http.HttpServletRequest;

import dk.netarkivet.common.webinterface.SiteSection;
import dk.netarkivet.viewerproxy.Constants;

/**
 * Site section that creates the menu for QA.
 */
public class QASiteSection extends SiteSection {
    /** The amount of pages visible in the QA menu. */
    private static final int PAGES_VISIBLE_IN_MENU = 1;

    /**
     * Create a QA SiteSection object.
     * <p>
     * This initialises the SiteSection object with the pages that exists in QA.
     */
    public QASiteSection() {
        super("sitesection;qa", "QA", PAGES_VISIBLE_IN_MENU, new String[][] {
                {"status", "pagetitle;qa.status"},
                // Pages below is not visible in the menu
                {"getreports", "pagetitle;qa.get.reports"}, {"getfiles", "pagetitle;qa.get.files"},
                {"crawlloglines", "pagetitle;qa.crawllog.lines.for.domain"},
                {"searchcrawllog", "pagetitle;qa.crawllog.lines.matching.regexp"}}, "QA", Constants.TRANSLATIONS_BUNDLE);
    }

    /**
     * Create a return-url for the QA pages that takes one.
     * <p>
     * The current implementation is hokey, but trying to go through URL objects is a mess.
     *
     * @param request The request that we have been called with.
     * @return A URL object that leads to the QA-status page on the same machine as the request came from.
     */
    public static String createQAReturnURL(HttpServletRequest request) {
        return request.getRequestURL().toString().replaceAll("/[^/]*\\.jsp.*$", "/QA-status.jsp");
    }

    /** No initialisation necessary in this site section. */
    public void initialize() {
    }

    /** No cleanup necessary in this site section. */
    public void close() {
    }
}
