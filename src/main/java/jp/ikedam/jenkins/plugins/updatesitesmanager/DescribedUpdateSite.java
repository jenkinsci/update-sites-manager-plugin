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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import hudson.DescriptorExtensionList;
import hudson.ExtensionPoint;
import hudson.model.Describable;
import hudson.model.UpdateSite;
import java.util.ArrayList;
import java.util.List;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;

/**
 * Base for UpdateSite that have Descriptor.
 */
public abstract class DescribedUpdateSite extends UpdateSite
        implements Describable<DescribedUpdateSite>, ExtensionPoint {
    /**
     * Constructor
     *
     * @param id  id for the site
     * @param url URL for the site
     */
    public DescribedUpdateSite(String id, String url) {
        super(StringUtils.trim(id), StringUtils.trim(url));
    }

    /**
     * Returns whether this UpdateSite is disabled.
     * <p>
     * Returning true makes Jenkins ignore the plugins in this UpdateSite.
     *
     * @return whether this UpdateSite is disabled.
     */
    public boolean isDisabled() {
        return false;
    }

    /**
     * Returns note
     * <p>
     * Provided for users to note about this UpdateSite.
     * Used only for displaying purpose.
     *
     * @return note
     */
    @SuppressWarnings("unused") /* used in config.jelly */
    public String getNote() {
        return "";
    }

    /**
     * Returns a list of plugins that should be shown in the "available" tab.
     * <p>
     * Returns nothing when disabled.
     *
     * @return list of available plugins
     * @see hudson.model.UpdateSite#getAvailables()
     */
    @Override
    public List<Plugin> getAvailables() {
        if (isDisabled()) {
            return new ArrayList<>(0);
        }
        return super.getAvailables();
    }

    /**
     * Returns the list of plugins that have updates for currently installed ones.
     * <p>
     * Returns nothing when disabled.
     *
     * @return list of plugins with updates
     * @see hudson.model.UpdateSite#getUpdates()
     */
    @Override
    public List<Plugin> getUpdates() {
        if (isDisabled()) {
            return new ArrayList<>(0);
        }
        return super.getUpdates();
    }

    /**
     * Returns true if it's time for us to check for newer versions.
     * <p>
     * Always returns false when disabled.
     *
     * @return {@code true} if time to check for newer versions.
     * @see hudson.model.UpdateSite#isDue()
     */
    @Override
    public boolean isDue() {
        if (isDisabled()) {
            return false;
        }
        return super.isDue();
    }

    /**
     * Does any of the plugin has updates?
     * <p>
     * Always returns false when disabled.
     *
     * @return any of the plugin has updates?
     * @see hudson.model.UpdateSite#hasUpdates()
     */
    @Override
    public boolean hasUpdates() {
        if (isDisabled()) {
            return false;
        }
        return super.hasUpdates();
    }

    /**
     * Returns all DescribedUpdateSite classes registered to Jenkins.
     *
     * @return the list of Descriptor of DescribedUpdateSite subclasses.
     */
    public static DescriptorExtensionList<DescribedUpdateSite, DescribedUpdateSiteDescriptor> all() {
        return Jenkins.get().getDescriptorList(DescribedUpdateSite.class);
    }

    /**
     * Returns the descriptor for this class.
     *
     * @return the descriptor for this class.
     * @see hudson.model.Describable#getDescriptor()
     */
    @Override
    public DescribedUpdateSiteDescriptor getDescriptor() {
        return (DescribedUpdateSiteDescriptor) Jenkins.get().getDescriptorOrDie(getClass());
    }
}
