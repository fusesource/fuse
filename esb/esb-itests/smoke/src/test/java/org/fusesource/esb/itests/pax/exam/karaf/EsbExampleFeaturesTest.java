/*
 * Copyright (C) FuseSource, Inc.
 *   http://fusesource.com
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package org.fusesource.esb.itests.pax.exam.karaf;

import java.io.File;

import org.apache.karaf.features.FeaturesService;
import org.fusesource.tooling.testing.pax.exam.karaf.FuseTestSupport;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.apache.karaf.tooling.exam.options.LogLevelOption;
import org.ops4j.pax.exam.MavenUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamReactorStrategy;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.options.DefaultCompositeOption;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFilePut;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.keepRuntimeFolder;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.logLevel;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.useOwnExamBundlesStartLevel;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;

@RunWith(JUnit4TestRunner.class)
@ExamReactorStrategy(AllConfinedStagedReactorFactory.class)
public class EsbExampleFeaturesTest extends EsbTestSupport {
    
    @Test
    public void testCbr() throws Exception {
        installQuickstartBundle("cbr");
    }

    @Test
    public void testEip() throws Exception {
        installQuickstartBundle("eip");
    }
    
    @Test
    public void testErrors() throws Exception {
        installQuickstartBundle("errors");
    }
    
    @Test
    @Ignore
    public void testJms() throws Exception {
        installUninstallCommand("quickstart-jms");
    }
        
    @Test
    public void testRest() throws Exception {
        installQuickstartBundle("rest");
    }
    
    @Test
    public void testSecureRest() throws Exception {
        installQuickstartBundle("secure-rest");
    }
    
    @Test
    public void testSoap() throws Exception {
        installQuickstartBundle("soap");
    }
    
    @Test
    public void testSecureSoap() throws Exception {
        installQuickstartBundle("secure-soap");
    }
    
    @Configuration
    public Option[] config() {
        return new Option[]{
                new DefaultCompositeOption(esbDistributionConfiguration("jboss-fuse-full")),
        };
    }

}
