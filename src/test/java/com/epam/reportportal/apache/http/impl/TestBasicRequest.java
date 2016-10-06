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

package com.epam.reportportal.apache.http.impl;

import org.junit.Assert;
import org.junit.Test;

import com.epam.reportportal.apache.http.HttpRequest;
import com.epam.reportportal.apache.http.HttpVersion;
import com.epam.reportportal.apache.http.message.BasicHttpRequest;

public class TestBasicRequest {

    @Test
    public void testConstructor() throws Exception {
        new BasicHttpRequest("GET", "/stuff");
        new BasicHttpRequest("GET", "/stuff", HttpVersion.HTTP_1_1);
        try {
            new BasicHttpRequest(null, "/stuff");
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
        try {
            new BasicHttpRequest("GET", null);
            Assert.fail("IllegalArgumentException should have been thrown");
        } catch (final IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testRequestLine() throws Exception {
        final HttpRequest request = new BasicHttpRequest("GET", "/stuff", HttpVersion.HTTP_1_0);
        Assert.assertEquals("GET", request.getRequestLine().getMethod());
        Assert.assertEquals("/stuff", request.getRequestLine().getUri());
        Assert.assertEquals(HttpVersion.HTTP_1_0, request.getRequestLine().getProtocolVersion());
    }

}

