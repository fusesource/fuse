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
package io.fabric8.docker.api;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertNotNull;

public class DockerTest {

    @Test
    public void testDocker() {
        DockerFactory factory = new DockerFactory();
        System.out.println("using: " + factory);
        Docker docker = factory.createDocker();
        assertNotNull(docker);

        List<Container> containers = docker.containers(1, 10, null, null, 1);
        for (Container container : containers) {
            System.out.println("" + container);
        }
    }
}
