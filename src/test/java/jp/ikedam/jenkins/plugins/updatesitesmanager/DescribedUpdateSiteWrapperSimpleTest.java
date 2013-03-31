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
import junit.framework.TestCase;

/**
 * Test for DescribedUpdateSiteWrapper, not concerned with Jenkins
 *
 */
public class DescribedUpdateSiteWrapperSimpleTest extends TestCase
{
    public void testDescribedUpdateSiteWrapper()
    {
        String id = "testId";
        String url = "http://localhost/update-center.json";
        
        {
            DescribedUpdateSite target = new DescribedUpdateSiteWrapper(
                    "  " + id + "\t",
                    "\t" + url + "  "
            );
            
            assertEquals("id must be trimmed", id, target.getId());
            assertEquals("url must be trimmed", url, target.getUrl());
            assertEquals("id must be configured", id, target.getUpdateSite().getId());
            assertEquals("url must be configured", url, target.getUpdateSite().getUrl());
        }
        
        {
            UpdateSite site = new UpdateSite(id, url);
            DescribedUpdateSite target = new DescribedUpdateSiteWrapper(site);
            
            assertEquals("id must be trimmed", id, target.getId());
            assertEquals("url must be trimmed", url, target.getUrl());
            assertSame("Must return same UpdateSite", site, target.getUpdateSite());
        }
    }
    
    static private class DerivedUpdateSite extends UpdateSite
    {
        public DerivedUpdateSite(String id, String url)
        {
            super(id, url);
        }
    }
    
    public void testIsEditable()
    {
        {
            UpdateSite site = new UpdateSite("test", "http://localhost/");
            DescribedUpdateSite target = new DescribedUpdateSiteWrapper(site);
            
            assertTrue("non default UpdateSite must be editable", target.isEditable());
        }
        
        {
            UpdateSite site = new UpdateSite("default", "http://localhost/");
            DescribedUpdateSite target = new DescribedUpdateSiteWrapper(site);
            
            assertFalse("default UpdateSite must not be editable", target.isEditable());
        }
        
        {
            UpdateSite site = new DerivedUpdateSite("test", "http://localhost/");
            DescribedUpdateSite target = new DescribedUpdateSiteWrapper(site);
            
            assertFalse("derived UpdateSite must not be editable", target.isEditable());
        }
    }
}
