/*
 * The MIT License
 *
 * Copyright (c) 2013 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jp.ikedam.jenkins.plugins.updatesitesmanager;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.Descriptor.FormException;
import hudson.model.ManagementLink;
import hudson.model.UpdateSite;
import hudson.util.FormApply;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jenkins.model.Jenkins;
import jp.ikedam.jenkins.plugins.updatesitesmanager.internal.Sites;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.interceptor.RequirePOST;

/**
 * Page for manage UpdateSites.
 *
 * Provides following page.
 * <ul>
 * <li>Manage UpdateSites, shown in Manage Jenkins page</li>
 * </ul>
 */
@Extension(ordinal = Integer.MAX_VALUE - 410) // show just after Manage Plugins (1.489 and later)
public class UpdateSitesManager extends ManagementLink {

    public static final String URL = "updatesites";

    /**
     * Return the name of the link shown in Manage Jenkins page.
     *
     * @return the name of the link.
     * @see hudson.model.Action#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return Messages.UpdateSitesManager_DisplayName();
    }

    /**
     * Return the icon file name shown in Manage Jenkins page.
     *
     * @return icon file name
     * @see hudson.model.ManagementLink#getIconFileName()
     */
    @Override
    public String getIconFileName() {
        return "/plugin/update-sites-manager/images/update-sites-manager.svg";
    }

    @NonNull
    @Override
    public Category getCategory() {
        return Category.CONFIGURATION;
    }

    /**
     * Return the description shown in Manage Jenkins page.
     *
     * @return the description
     * @see hudson.model.ManagementLink#getDescription()
     */
    @Override
    public String getDescription() {
        return Messages.UpdateSitesManager_Description();
    }

    /**
     * Return the name used in url.
     *
     * @return the name used in url.
     * @see hudson.model.ManagementLink#getUrlName()
     */
    @Override
    public String getUrlName() {
        return URL;
    }

    /**
     * Return a list of custom UpdateSites registered in Jenkins.
     *
     * @return a list of UpdateSites
     */
    public List<UpdateSite> getManagedUpdateSiteList() {
        return Jenkins.get().getUpdateCenter().getSites().stream()
                .filter(site -> site instanceof DescribedUpdateSite)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Return a list of not custom UpdateSites registered in Jenkins.
     *
     * @return a list of UpdateSites
     */
    public List<UpdateSite> getNotManagedUpdateSiteList() {
        return Jenkins.get().getUpdateCenter().getSites().stream()
                .filter(site -> !(site instanceof DescribedUpdateSite))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Returns all the registered DescribedUpdateSite.
     *
     * @return a list of Descriptor of DescribedUpdateSite.
     */
    public List<DescribedUpdateSiteDescriptor> getUpdateSiteDescriptorList() {
        return DescribedUpdateSite.all();
    }

    /**
     * Update all registered sites with concatenation of managed and not managed
     * @param req the request
     * @param rsp the response
     * @param managed managed sites form submitted form
     * @throws ServletException thrown when failed to generate response
     * @throws IOException thrown when failed to generate response or to save configurations
     * @throws FormException thrown when inappropriate configurations
     */
    @RequirePOST
    @SuppressWarnings("unused")
    public void doUpdate(StaplerRequest2 req, StaplerResponse2 rsp, @Sites List<UpdateSite> managed)
            throws IOException, FormException, ServletException {

        Jenkins.get().checkPermission(Jenkins.ADMINISTER);

        List<UpdateSite> newSitesList = Stream.concat(getNotManagedUpdateSiteList().stream(), managed.stream())
                .collect(Collectors.toCollection(ArrayList::new));

        shouldNotContainDuplicatedIds(newSitesList);
        shouldNotContainBlankIds(newSitesList);

        Jenkins.get().getUpdateCenter().getSites().replaceBy(newSitesList);
        Jenkins.get().getUpdateCenter().save();

        FormApply.success(req.getContextPath() + "/manage").generateResponse(req, rsp, null);
    }

    /**
     * Check method for duplicated ids of submitted sites
     */
    private static void shouldNotContainDuplicatedIds(List<UpdateSite> sites) throws FormException {
        Set<String> set = sites.stream().map(UpdateSite::getId).collect(Collectors.toSet());

        if (set.size() != sites.size()) {
            throw new FormException("id is duplicated", "id");
        }
    }

    /**
     * Check method for blank ids of submitted sites
     */
    private static void shouldNotContainBlankIds(List<UpdateSite> sites) throws FormException {
        if (sites.stream().map(UpdateSite::getId).anyMatch(StringUtils::isBlank)) {
            throw new FormException("id is empty", "id");
        }
    }
}
