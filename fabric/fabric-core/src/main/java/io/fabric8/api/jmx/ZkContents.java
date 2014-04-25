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
package io.fabric8.api.jmx;

import java.util.List;

/**
 * Represents the text of a text file or a directory in ZooKeeper
 */
public class ZkContents {
    private final int dataLength;
    private List<String> children;
    private final String stringData;

    public ZkContents(int dataLength, List<String> children, String stringData) {
        this.dataLength = dataLength;
        this.children = children;
        this.stringData = stringData;
    }

    public List<String> getChildren() {
        return children;
    }

    public int getDataLength() {
        return dataLength;
    }

    public String getStringData() {
        return stringData;
    }
}
