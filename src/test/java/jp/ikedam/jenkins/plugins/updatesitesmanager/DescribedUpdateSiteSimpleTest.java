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

import hudson.model.UpdateSite;
import hudson.util.FormValidation;
import junit.framework.TestCase;

/**
 * Tests for DescribedUpdateSite, not concerned with Jenkins.
 */
public class DescribedUpdateSiteSimpleTest extends TestCase
{
    private static class TestDescribedUpdateSite extends DescribedUpdateSite
    {
        private static final long serialVersionUID = 1934091438438690698L;
        
        public TestDescribedUpdateSite(String id, String url)
        {
            super(id, url);
        }
        
        public static class DescriptorImpl extends DescribedUpdateSite.Descriptor
        {
            @Override
            public String getDescription()
            {
                return null;
            }
            
            @Override
            public String getDisplayName()
            {
                return null;
            }
        }
    }
    
    public void testDescribedUpdateSite()
    {
        // trimmed
        {
            String id = "testId";
            String url = "http://localhost/update-center.json";
            UpdateSite site = new TestDescribedUpdateSite(
                    "  " + id + "  ",
                    " " + url + "\t"
            );
            
            assertEquals("id is not trimmed", id, site.getId());
            assertEquals("url is not trimmed", url, site.getUrl());
        }
        
        // null
        {
            new TestDescribedUpdateSite(null, null);
        }
    }
    
    public void testGetUpdateSite()
    {
        DescribedUpdateSite site = new TestDescribedUpdateSite("id", "url");
        assertSame("getUpdateSite must return itself", site, site.getUpdateSite());
    }
    
    /*
    public void testGetAvailables()
    {
        // no way to initialize UpdateSite...
    }
    */
    
    /*
    public void testGetUpdates()
    {
        // no way to initialize UpdateSite...
    }
    */
    
    /*
    public void testIsDue()
    {
        // no way to initialize UpdateSite...
    }
    */
    
    /*
    public void testHasUpdates()
    {
        // no way to initialize UpdateSite...
    }
    */
    
    private DescribedUpdateSite.Descriptor getDescriptor()
    {
        return new TestDescribedUpdateSite.DescriptorImpl();
    }
    
    public void testDescriptorDoCheckIdOk()
    {
        DescribedUpdateSite.Descriptor descriptor = getDescriptor();
        
        {
            String id = "somevalue";
            assertEquals(
                "simple id",
                FormValidation.Kind.OK,
                descriptor.doCheckId(id).kind
            );
        }
    }
    
    public void testDescriptorDoCheckIdError()
    {
        DescribedUpdateSite.Descriptor descriptor = getDescriptor();
        
        // null
        {
            String id = null;
            assertEquals(
                "null",
                FormValidation.Kind.ERROR,
                descriptor.doCheckId(id).kind
            );
        }
        
        // empty
        {
            String id = "";
            assertEquals(
                "empty",
                FormValidation.Kind.ERROR,
                descriptor.doCheckId(id).kind
            );
        }
        
        // blank
        {
            String id = "  ";
            assertEquals(
                "blank",
                FormValidation.Kind.ERROR,
                descriptor.doCheckId(id).kind
            );
        }
    }
    
    public void testDescriptorDoCheckUrlOk()
    {
        DescribedUpdateSite.Descriptor descriptor = getDescriptor();
        
        {
            String url = "http://localhost/update-center.json";
            assertEquals(
                "simple url",
                FormValidation.Kind.OK,
                descriptor.doCheckUrl(url).kind
            );
        }
    }
    
    public void testDescriptorDoCheckUrlError()
    {
        DescribedUpdateSite.Descriptor descriptor = getDescriptor();
        
        // null
        {
            String url = null;
            assertEquals(
                "null",
                FormValidation.Kind.ERROR,
                descriptor.doCheckUrl(url).kind
            );
        }
        
        // empty
        {
            String url = "";
            assertEquals(
                "empty",
                FormValidation.Kind.ERROR,
                descriptor.doCheckUrl(url).kind
            );
        }
        
        // blank
        {
            String url = "  ";
            assertEquals(
                "blank",
                FormValidation.Kind.ERROR,
                descriptor.doCheckUrl(url).kind
            );
        }
        
        // non url
        {
            String url = "hogehoge";
            assertEquals(
                "non url",
                FormValidation.Kind.ERROR,
                descriptor.doCheckUrl(url).kind
            );
        }
    }
}
