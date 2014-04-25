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
package io.fabric8.zookeeper.utils;

import java.io.File;
import java.net.ServerSocket;
import java.net.URL;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.apache.zookeeper.server.NIOServerCnxnFactory;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServer;
import org.apache.zookeeper.server.persistence.FileTxnSnapLog;
import org.junit.Test;

public class ZookeeperImportUtilsTest {

	@Test
	public void testNoExceptionFromImportProperties () throws Exception {
		URL url = this.getClass().getResource("/import-test.properties");
		ZookeeperImportUtils.importFromPropertiesFile(null, url.toString(), "mypid", null, null, true);
	}
	
	@Test
	public void testNoExceptionFromImportFromFileSystem () throws Exception {
        int port = findFreePort();

        CuratorFramework curator = CuratorFrameworkFactory.builder()
                .connectString("localhost:" + port)
                .retryPolicy(new RetryOneTime(1000))
                .build();
        curator.start();
        
        NIOServerCnxnFactory cnxnFactory = startZooKeeper(port);
        curator.getZookeeperClient().blockUntilConnectedOrTimedOut();
        
		String target = "/fabric/profiles/mq-base.profile/import-test.properties";
		String source = this.getClass().getResource("/import-test.properties").getFile();
		
		ZookeeperImportUtils.importFromFileSystem(curator, source, target, null, null, false, false, false);
		
		curator.close();
		cnxnFactory.shutdown();
		
	}
	
    private int findFreePort() throws Exception {
        ServerSocket ss = new ServerSocket(0);
        int port = ss.getLocalPort();
        ss.close();
        return port;
    }
    
    private NIOServerCnxnFactory startZooKeeper(int port) throws Exception {
        ServerConfig cfg = new ServerConfig();
        cfg.parse(new String[] { Integer.toString(port), "target/zk/data" });

        ZooKeeperServer zkServer = new ZooKeeperServer();
        FileTxnSnapLog ftxn = new FileTxnSnapLog(new File(cfg.getDataLogDir()), new File(cfg.getDataDir()));
        zkServer.setTxnLogFactory(ftxn);
        zkServer.setTickTime(cfg.getTickTime());
        zkServer.setMinSessionTimeout(cfg.getMinSessionTimeout());
        zkServer.setMaxSessionTimeout(cfg.getMaxSessionTimeout());
        NIOServerCnxnFactory cnxnFactory = new NIOServerCnxnFactory();
        cnxnFactory.configure(cfg.getClientPortAddress(), cfg.getMaxClientCnxns());
        cnxnFactory.startup(zkServer);
        return cnxnFactory;
    }
}
