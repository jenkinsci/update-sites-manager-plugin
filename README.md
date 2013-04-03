UpdateSites Manager plugin
===========================

Japanese version of this document is README_ja.md

Jenkins plugin to manage update sites, where Jenkins accesses in order to retrieve plugins.

What's this?
------------

UpdateSites Manager is a [Jenkins](http://jenkins-ci.org/) plugin.

Sites where Jenkins finds and downloads new plugins, which are called update sites, are listed in hudson.model.UpdateCenter.xml.
This plugin enables you to manage update sites in the Jenkins configuration page with web browsers.
Following features are available:

* "Manage UpdateSites" link is added to "Manage Jenkins" page.
* You can list, add, edit, or delete an update site registered with Jenkins.
* When adding a new update site, you specify following fields:
	* Disable this site
		* Check if you want to disable the update site temporary.
	* ID
		* The ID of the update site. Value to specify here is specified by the update site.
	* URL
		* The URL of the update site. Usually, the URL to update-center.json.
	* Note
		* A note. It is not used by Jenkins, and you can note anything here.
	* CA Certificate
		* CA Certificate for this site. This is useful for a update site which is signed with a self-signed certificate.

How to create a new update site
-------------------------------

Refer to [How to create your own Jenkins Update Center] (https://github.com/ikedam/backend-update-center2/wiki/How-to-create-your-own-Jenkins-Update-Center)


Limitations
-----------

* Following update sites are listed in "Manage UpdateSites" page, but cannot be edited nor deleted.
	* Default update site (its ID is "default"). This can be configured in "Manage Plugins" page.
	* Update sites provided by other plugins. These plugins must provide their own configuration pages for their update sites.

TODO
----

* Create a menu icon.


