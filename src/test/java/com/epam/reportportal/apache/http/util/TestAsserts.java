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

package com.epam.reportportal.apache.http.util;

import com.epam.reportportal.apache.http.util.Asserts;
import org.junit.Test;

/**
 * Unit tests for {@link Asserts}.
 */
public class TestAsserts {

    @Test
    public void testExpressionCheckPass() {
        Asserts.check(true, "All is well");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionCheckFail() {
        Asserts.check(false, "Oopsie");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotNullFail() {
        Asserts.notNull(null, "Stuff");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotEmptyFail1() {
        Asserts.notEmpty((String) null, "Stuff");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotEmptyFail2() {
        Asserts.notEmpty("", "Stuff");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotEmptyBlank1() {
        Asserts.notBlank((String) null, "Stuff");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotEmptyBlank2() {
        Asserts.notBlank("", "Stuff");
    }

    @Test(expected=IllegalStateException.class)
    public void testExpressionNotBlankFail3() {
        Asserts.notBlank(" \t \n\r", "Stuff");
    }

}
