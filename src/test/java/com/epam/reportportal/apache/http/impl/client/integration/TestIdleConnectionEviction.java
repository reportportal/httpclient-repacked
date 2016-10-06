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

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import com.epam.reportportal.apache.http.localserver.LocalTestServer;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.epam.reportportal.apache.http.HttpHost;
import com.epam.reportportal.apache.http.HttpResponse;
import com.epam.reportportal.apache.http.client.ClientProtocolException;
import com.epam.reportportal.apache.http.client.HttpClient;
import com.epam.reportportal.apache.http.client.methods.HttpGet;
import com.epam.reportportal.apache.http.client.methods.HttpUriRequest;
import com.epam.reportportal.apache.http.conn.HttpClientConnectionManager;
import com.epam.reportportal.apache.http.impl.client.HttpClients;
import com.epam.reportportal.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import com.epam.reportportal.apache.http.localserver.LocalServerTestBase;
import com.epam.reportportal.apache.http.util.EntityUtils;

public class TestIdleConnectionEviction extends LocalServerTestBase {

    @Before
    public void setUp() throws Exception {
        this.localServer = new LocalTestServer(null, null);
        this.localServer.registerDefaultHandlers();
        this.localServer.start();
    }

    @Test
    @Ignore("We have a concurrency bug in HttpCore which will be addressed after HttpClient 4.3.2 is released")
    public void testIdleConnectionEviction() throws Exception {
        final PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(10);
        cm.setMaxTotal(50);

        final HttpClient httpclient = HttpClients.custom().setConnectionManager(cm).build();

        final IdleConnectionMonitor idleConnectionMonitor = new IdleConnectionMonitor(cm);
        idleConnectionMonitor.start();

        final InetSocketAddress address = this.localServer.getServiceAddress();
        final HttpHost target = new HttpHost(address.getHostName(), address.getPort());
        final HttpGet httpget = new HttpGet("/random/1024");
        final WorkerThread[] workers = new WorkerThread[5];
        for (int i = 0; i < workers.length; i++) {
            workers[i] = new WorkerThread(httpclient, target, httpget, 200);
        }
        for (final WorkerThread worker : workers) {
            worker.start();
        }
        for (final WorkerThread worker : workers) {
            worker.join();
            final Exception ex = worker.getException();
            if (ex != null) {
                throw ex;
            }
        }
        idleConnectionMonitor.shutdown();
    }

    static class WorkerThread extends Thread {

        private final HttpClient httpclient;
        private final HttpHost target;
        private final HttpUriRequest request;
        private final int count;

        private volatile Exception ex;

        public WorkerThread(
                final HttpClient httpclient,
                final HttpHost target,
                final HttpUriRequest request,
                final int count) {
            super();
            this.httpclient = httpclient;
            this.target = target;
            this.request = request;
            this.count = count;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < this.count; i++) {
                    final HttpResponse response = this.httpclient.execute(this.target, this.request);
                    final int status = response.getStatusLine().getStatusCode();
                    if (status != 200) {
                        this.request.abort();
                        throw new ClientProtocolException("Unexpected status code: " + status);
                    }
                    EntityUtils.consume(response.getEntity());
                    Thread.sleep(10);
                }
            } catch (final Exception ex) {
                this.ex = ex;
            }
        }

        public Exception getException() {
            return ex;
        }

    }

    public static class IdleConnectionMonitor extends Thread {

        private final HttpClientConnectionManager cm;
        private volatile boolean shutdown;

        public IdleConnectionMonitor(final HttpClientConnectionManager cm) {
            super();
            this.cm = cm;
            setDaemon(true);
        }

        @Override
        public void run() {
            try {
                while (!this.shutdown) {
                    synchronized (this) {
                        wait(250);
                        this.cm.closeIdleConnections(1, TimeUnit.MILLISECONDS);
                    }
                }
            } catch (final InterruptedException ex) {
                // terminate
            }
        }

        public void shutdown() {
            this.shutdown = true;
            synchronized (this) {
                notifyAll();
            }
        }

    }

}
