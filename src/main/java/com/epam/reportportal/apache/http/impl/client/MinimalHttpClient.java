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

package com.epam.reportportal.apache.http.impl.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.epam.reportportal.apache.http.client.ClientProtocolException;
import com.epam.reportportal.apache.http.conn.HttpClientConnectionManager;
import com.epam.reportportal.apache.http.params.BasicHttpParams;
import com.epam.reportportal.apache.http.HttpException;
import com.epam.reportportal.apache.http.HttpHost;
import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.annotation.ThreadSafe;
import com.epam.reportportal.apache.http.client.config.RequestConfig;
import com.epam.reportportal.apache.http.client.methods.CloseableHttpResponse;
import com.epam.reportportal.apache.http.client.methods.Configurable;
import com.epam.reportportal.apache.http.client.methods.HttpExecutionAware;
import com.epam.reportportal.apache.http.client.methods.HttpRequestWrapper;
import com.epam.reportportal.apache.http.client.protocol.HttpClientContext;
import com.epam.reportportal.apache.http.conn.ClientConnectionManager;
import com.epam.reportportal.apache.http.conn.ClientConnectionRequest;
import com.epam.reportportal.apache.http.conn.ManagedClientConnection;
import com.epam.reportportal.apache.http.conn.routing.HttpRoute;
import com.epam.reportportal.apache.http.conn.scheme.SchemeRegistry;
import com.epam.reportportal.apache.http.impl.DefaultConnectionReuseStrategy;
import com.epam.reportportal.apache.http.impl.execchain.MinimalClientExec;
import com.epam.reportportal.apache.http.params.HttpParams;
import com.epam.reportportal.apache.http.protocol.BasicHttpContext;
import com.epam.reportportal.apache.http.protocol.HttpContext;
import com.epam.reportportal.apache.http.protocol.HttpRequestExecutor;
import com.epam.reportportal.apache.http.util.Args;

/**
 * Internal class.
 *
 * @since 4.3
 */
@ThreadSafe
@SuppressWarnings("deprecation")
class MinimalHttpClient extends CloseableHttpClient {

    private final HttpClientConnectionManager connManager;
    private final MinimalClientExec requestExecutor;
    private final HttpParams params;

    public MinimalHttpClient(
            final HttpClientConnectionManager connManager) {
        super();
        this.connManager = Args.notNull(connManager, "HTTP connection manager");
        this.requestExecutor = new MinimalClientExec(
                new HttpRequestExecutor(),
                connManager,
                DefaultConnectionReuseStrategy.INSTANCE,
                DefaultConnectionKeepAliveStrategy.INSTANCE);
        this.params = new BasicHttpParams();
    }

    @Override
    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(target, "Target host");
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
            final HttpClientContext localcontext = HttpClientContext.adapt(
                context != null ? context : new BasicHttpContext());
            final HttpRoute route = new HttpRoute(target);
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            return this.requestExecutor.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    public HttpParams getParams() {
        return this.params;
    }

    public void close() {
        this.connManager.shutdown();
    }

    public ClientConnectionManager getConnectionManager() {

        return new ClientConnectionManager() {

            public void shutdown() {
                connManager.shutdown();
            }

            public ClientConnectionRequest requestConnection(
                    final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            public void releaseConnection(
                    final ManagedClientConnection conn,
                    final long validDuration, final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }

        };

    }

}
