/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.zookeeper.commands;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CreateBuilder;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.ACL;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CreateTest extends Assert {

    // Fixtures

    CreateBuilder createBuilder = mock(CreateBuilder.class);

    CuratorFramework curator = mock(CuratorFramework.class);

    Create createCommand = new Create();

    @Before
    public void setUp() {
        createCommand.path = "/foo/bar";

        given(createBuilder.withMode(any(CreateMode.class))).willReturn(createBuilder);
        given(createBuilder.withACL(anyListOf(ACL.class))).willReturn(createBuilder);
        given(curator.create()).willReturn(createBuilder);
    }

    // Tests

    @Test
    public void shouldCreatePathWithoutData() throws Exception {
        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).forPath(createCommand.path);
    }

    @Test
    public void shouldCreatePathWithData() throws Exception {
        // Given
        createCommand.data = "node data";

        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).forPath(createCommand.path, createCommand.data.getBytes());
    }

    @Test
    public void shouldCreateRecursivePathWithoutData() throws Exception {
        // Given
        createCommand.recursive = true;

        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).creatingParentsIfNeeded();
        verify(createBuilder).forPath(createCommand.path);
    }

    @Test
    public void shouldCreateRecursivePathWithData() throws Exception {
        // Given
        createCommand.recursive = true;
        createCommand.data = "node data";

        // When
        createCommand.doExecute(curator);

        // Then
        verify(createBuilder).creatingParentsIfNeeded();
        verify(createBuilder).forPath(createCommand.path, createCommand.data.getBytes());
    }

}
