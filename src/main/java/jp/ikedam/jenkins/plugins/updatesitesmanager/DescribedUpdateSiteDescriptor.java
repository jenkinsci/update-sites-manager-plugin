/*
 * The MIT License
 *
 * Copyright (c) 2016 IKEDA Yasuyuki
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

import hudson.model.Descriptor;
import hudson.util.FormValidation;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.QueryParameter;

/**
 * Base class for Descriptor of subclass of DescribedUpdateSite
 */
public abstract class DescribedUpdateSiteDescriptor extends Descriptor<DescribedUpdateSite> {
    /**
     * Validate id
     *
     * @param id id to validate
     * @return the validation result
     */
    public FormValidation doCheckId(@QueryParameter String id) {
        if (StringUtils.isBlank(id)) {
            return FormValidation.error(Messages.DescribedupdateSite_id_required());
        }
        return FormValidation.ok();
    }

    /**
     * Validate url
     *
     * @param url the URL to validate
     * @return the validation result
     */
    public FormValidation doCheckUrl(@QueryParameter String url) {
        if (StringUtils.isBlank(url)) {
            return FormValidation.error(Messages.DescribedupdateSite_url_required());
        }

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            return FormValidation.error(Messages.DescribedupdateSite_url_invalid(e.getLocalizedMessage()));
        }

        if (StringUtils.isBlank(uri.getScheme()) || StringUtils.isBlank(uri.getHost())) {
            return FormValidation.error(Messages.DescribedupdateSite_url_invalid("incomplete URI"));
        }
        return FormValidation.ok();
    }
}
