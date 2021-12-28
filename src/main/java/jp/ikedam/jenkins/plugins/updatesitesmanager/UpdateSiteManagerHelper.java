/*
 * The MIT License
 * 
 * Copyright (c) 2021 Falco Nikolas
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

import java.io.IOException;
import java.net.URLConnection;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import hudson.security.ACL;
import jenkins.model.Jenkins;

/* package*/ class UpdateSiteManagerHelper {

    @Nullable
    public static <C extends IdCredentials> C findCredentialsById(String id, @Nonnull Class<C> type) {
        List<C> credentials = CredentialsProvider.lookupCredentials(type, Jenkins.get(), ACL.SYSTEM, Collections.emptyList());
        return CredentialsMatchers.firstOrNull(credentials, CredentialsMatchers.withId(id));
    }

    public static void addAuthorisationHeader(URLConnection connection, @Nullable String credentialsId) throws IOException {
        if (credentialsId == null) {
            return;
        }

        StandardUsernamePasswordCredentials credential = UpdateSiteManagerHelper.findCredentialsById(credentialsId, StandardUsernamePasswordCredentials.class);
        if (credential != null) {
            String token = credential.getUsername() + ':' + credential.getPassword().getPlainText();
            String basicAuth = "Basic " + Base64.getEncoder().encodeToString(token.getBytes("UTF-8"));
            connection.setRequestProperty ("Authorization", basicAuth);
        } else {
            throw new IOException(Messages.UpdateSiteManagerHelper_invalidCredentials(credentialsId));
        }
    }
}
