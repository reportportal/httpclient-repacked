/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.epam.reportportal.apache.http.impl.client.integration;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

import com.epam.reportportal.apache.http.localserver.LocalTestServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.epam.reportportal.apache.http.Consts;
import com.epam.reportportal.apache.http.HttpEntity;
import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.HttpResponseInterceptor;
import com.epam.reportportal.apache.http.HttpStatus;
import com.epam.reportportal.apache.http.auth.AUTH;
import com.epam.reportportal.apache.http.auth.AuthScheme;
import com.epam.reportportal.apache.http.auth.AuthSchemeProvider;
import com.epam.reportportal.apache.http.auth.AuthScope;
import com.epam.reportportal.apache.http.auth.Credentials;
import com.epam.reportportal.apache.http.auth.UsernamePasswordCredentials;
import com.epam.reportportal.apache.http.client.CredentialsProvider;
import com.epam.reportportal.apache.http.client.config.RequestConfig;
import com.epam.reportportal.apache.http.client.methods.HttpGet;
import com.epam.reportportal.apache.http.client.protocol.HttpClientContext;
import com.epam.reportportal.apache.http.config.Registry;
import com.epam.reportportal.apache.http.config.RegistryBuilder;
import com.epam.reportportal.apache.http.entity.StringEntity;
import com.epam.reportportal.apache.http.impl.auth.BasicScheme;
import com.epam.reportportal.apache.http.impl.auth.BasicSchemeFactory;
import com.epam.reportportal.apache.http.impl.client.HttpClients;
import com.epam.reportportal.apache.http.impl.client.TargetAuthenticationStrategy;
import com.epam.reportportal.apache.http.localserver.RequestBasicAuth;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.protocol.HttpProcessor;
import com.epam.reportportal.apache.http.protocol.HttpProcessorBuilder;
import com.epam.reportportal.apache.http.protocol.HttpRequestHandler;
import com.epam.reportportal.apache.http.protocol.ResponseConnControl;
import com.epam.reportportal.apache.http.protocol.ResponseContent;
import com.epam.reportportal.apache.http.protocol.ResponseDate;
import com.epam.reportportal.apache.http.protocol.ResponseServer;
import com.epam.reportportal.apache.http.util.EntityUtils;

public class TestClientReauthentication extends IntegrationTestBase {

    public class ResponseBasicUnauthorized implements HttpResponseInterceptor {

        public void process(
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                response.addHeader(AUTH.WWW_AUTH, "MyBasic realm=\"test realm\"");
            }
        }

    }

    @Before
    public void setUp() throws Exception {
        final HttpProcessor httpproc = HttpProcessorBuilder.create()
            .add(new ResponseDate())
            .add(new ResponseServer(LocalTestServer.ORIGIN))
            .add(new ResponseContent())
            .add(new ResponseConnControl())
            .add(new RequestBasicAuth())
            .add(new ResponseBasicUnauthorized()).build();

        this.localServer = new LocalTestServer(httpproc, null);
        startServer();
    }

    static class AuthHandler implements HttpRequestHandler {

        private final AtomicLong count = new AtomicLong(0);

        public void handle(
                final HttpRequest request,
                final HttpResponse response,
                final HttpContext context) throws HttpException, IOException {
            final String creds = (String) context.getAttribute("creds");
            if (creds == null || !creds.equals("test:test")) {
                response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
            } else {
                // Make client re-authenticate on each fourth request
                if (this.count.incrementAndGet() % 4 == 0) {
                    response.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
                } else {
                    response.setStatusCode(HttpStatus.SC_OK);
                    final StringEntity entity = new StringEntity("success", Consts.ASCII);
                    response.setEntity(entity);
                }
            }
        }

    }

    static class TestCredentialsProvider implements CredentialsProvider {

        private final Credentials creds;
        private AuthScope authscope;

        TestCredentialsProvider(final Credentials creds) {
            super();
            this.creds = creds;
        }

        public void clear() {
        }

        public Credentials getCredentials(final AuthScope authscope) {
            this.authscope = authscope;
            return this.creds;
        }

        public void setCredentials(final AuthScope authscope, final Credentials credentials) {
        }

        public AuthScope getAuthScope() {
            return this.authscope;
        }

    }

    @Test
    public void testBasicAuthenticationSuccess() throws Exception {
        this.localServer.register("*", new AuthHandler());

        final BasicSchemeFactory myBasicAuthSchemeFactory = new BasicSchemeFactory() {

            @Override
            public AuthScheme create(final HttpContext context) {
                return new BasicScheme() {

                    @Override
                    public String getSchemeName() {
                        return "MyBasic";
                    }

                };
            }

        };

        final TargetAuthenticationStrategy myAuthStrategy = new TargetAuthenticationStrategy() {

            @Override
            protected boolean isCachable(final AuthScheme authScheme) {
                return "MyBasic".equalsIgnoreCase(authScheme.getSchemeName());
            }

        };

        final TestCredentialsProvider credsProvider = new TestCredentialsProvider(
                new UsernamePasswordCredentials("test", "test"));

        final RequestConfig config = RequestConfig.custom()
            .setTargetPreferredAuthSchemes(Arrays.asList("MyBasic"))
            .build();
        final Registry<AuthSchemeProvider> authSchemeRegistry = RegistryBuilder.<AuthSchemeProvider>create()
            .register("MyBasic", myBasicAuthSchemeFactory)
            .build();
        this.httpclient = HttpClients.custom()
            .setDefaultAuthSchemeRegistry(authSchemeRegistry)
            .setTargetAuthenticationStrategy(myAuthStrategy)
            .setDefaultCredentialsProvider(credsProvider)
            .build();

        final HttpClientContext context = HttpClientContext.create();
        for (int i = 0; i < 10; i++) {
            final HttpGet httpget = new HttpGet("/");
            httpget.setConfig(config);
            final HttpResponse response = this.httpclient.execute(getServerHttp(), httpget, context);
            final HttpEntity entity = response.getEntity();
            Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
            Assert.assertNotNull(entity);
            EntityUtils.consume(entity);
        }
    }

}