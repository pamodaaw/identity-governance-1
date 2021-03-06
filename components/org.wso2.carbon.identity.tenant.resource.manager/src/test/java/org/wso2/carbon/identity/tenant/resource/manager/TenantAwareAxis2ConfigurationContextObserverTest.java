/*
 * Copyright (c) 2019, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.tenant.resource.manager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.wso2.carbon.base.CarbonBaseConstants;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.event.publisher.core.config.EventPublisherConfiguration;
import org.wso2.carbon.event.publisher.core.internal.CarbonEventPublisherService;
import org.wso2.carbon.event.publisher.core.internal.EventPublisher;
import org.wso2.carbon.event.publisher.core.internal.ds.EventPublisherServiceValueHolder;
import org.wso2.carbon.event.stream.core.EventStreamConfiguration;
import org.wso2.carbon.event.stream.core.EventStreamService;
import org.wso2.carbon.identity.configuration.mgt.core.ConfigurationManager;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resource;
import org.wso2.carbon.identity.configuration.mgt.core.model.ResourceFile;
import org.wso2.carbon.identity.configuration.mgt.core.model.Resources;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManager;
import org.wso2.carbon.identity.tenant.resource.manager.core.ResourceManagerImpl;
import org.wso2.carbon.identity.tenant.resource.manager.internal.TenantResourceManagerDataHolder;
import org.wso2.carbon.identity.tenant.resource.manager.util.ResourceUtils;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@PrepareForTest({
                        TenantResourceManagerDataHolder.class, PrivilegedCarbonContext.class, ResourceUtils.class,
                        EventPublisherServiceValueHolder.class, EventPublisherServiceValueHolder.class,
                        IdentityTenantUtil.class
                })

public class TenantAwareAxis2ConfigurationContextObserverTest extends PowerMockTestCase {

    private static final String EMAIL_PUBLISHER = "EmailPublisher";
    private static final String SAMPLE_RESOURCE_FILE_TXT = "sample-resource-file.txt";
    private static final String TENANT_DOMAIN = "abc.com";
    private static final String TENANT_SPECIFIC_EMAIL_PUBLISHER = "TENANT_SPECIFIC_EMAIL_PUBLISHER";
    private static final int TENANT_ID = 1;
    private CustomCarbonEventPublisherService carbonEventPublisherService = new CustomCarbonEventPublisherService();

    @Mock
    TenantResourceManagerDataHolder tenantResourceManagerDataHolder;
    @Mock
    EventStreamService eventStreamService;
    @Mock
    ConfigurationManager configurationManager;
    @Mock
    StreamDefinition streamDefinition;
    @Mock
    EventPublisher eventPublisher;

    @BeforeMethod
    public void setUp() throws Exception {

        String carbonHome = Paths.get(System.getProperty("user.dir"), "target", "test-classes").toString();
        System.setProperty(CarbonBaseConstants.CARBON_HOME, carbonHome);
        System.setProperty(CarbonBaseConstants.CARBON_CONFIG_DIR_PATH, Paths.get(carbonHome, "conf").toString());
        mockStatic(TenantResourceManagerDataHolder.class);
        mockStatic(ResourceUtils.class);
        mockStatic(EventPublisherServiceValueHolder.class);
        mockStatic(EventPublisherServiceValueHolder.class);
        mockStatic(IdentityTenantUtil.class);
        prepareConfigs();
    }

    private void mockCarbonContext() {

        mockStatic(PrivilegedCarbonContext.class);
        PrivilegedCarbonContext privilegedCarbonContext = mock(PrivilegedCarbonContext.class);
        when(PrivilegedCarbonContext.getThreadLocalCarbonContext()).thenReturn(privilegedCarbonContext);
        when(privilegedCarbonContext.getTenantDomain()).thenReturn(TENANT_DOMAIN);
        when(privilegedCarbonContext.getTenantId()).thenReturn(TENANT_ID);
    }

    class CustomCarbonEventPublisherService extends CarbonEventPublisherService {

        private HashMap<String, EventPublisher> tenantSpecificEventPublisherConfigurationMap = new HashMap<>();
        EventPublisherConfiguration tenantSpecificEventPublisherConfiguration = new EventPublisherConfiguration();

        public EventPublisherConfiguration getEventPublisherConfiguration(InputStream eventPublisherConfiguration) {
            tenantSpecificEventPublisherConfiguration.setEventPublisherName(TENANT_SPECIFIC_EMAIL_PUBLISHER);
            return tenantSpecificEventPublisherConfiguration;
        }

        public void addEventPublisherConfiguration(EventPublisherConfiguration eventPublisherConfiguration) {

            tenantSpecificEventPublisherConfigurationMap.put(EMAIL_PUBLISHER, eventPublisher);
        }

        public EventPublisherConfiguration getActiveEventPublisherConfiguration(
                String eventPublisherConfigurationName) {
            return tenantSpecificEventPublisherConfiguration;
        }

        public void removeEventPublisherConfigurationFile(String fileName, int tenantId) {
        }

        private EventPublisher getEventPublisherConfigurationFromMap() {
            return tenantSpecificEventPublisherConfigurationMap
                    .get(TenantAwareAxis2ConfigurationContextObserverTest.EMAIL_PUBLISHER);
        }
    }

    private void prepareConfigs() throws Exception {

        mockCarbonContext();
        when(TenantResourceManagerDataHolder.getInstance()).thenReturn(tenantResourceManagerDataHolder);
        when(IdentityTenantUtil.getTenantDomain(anyInt())).thenReturn(TENANT_DOMAIN);
        ResourceFile resourceFile = new ResourceFile();
        resourceFile.setName(EMAIL_PUBLISHER);
        List<ResourceFile> resourceFiles = new ArrayList<>();
        resourceFiles.add(resourceFile);

        ResourceManager resourceManager = new ResourceManagerImpl();
        when(tenantResourceManagerDataHolder.getResourceManager()).thenReturn(resourceManager);

        when(tenantResourceManagerDataHolder.getCarbonEventPublisherService()).thenReturn(carbonEventPublisherService);
        when(tenantResourceManagerDataHolder.getCarbonEventStreamService()).thenReturn(eventStreamService);
        when(tenantResourceManagerDataHolder.getConfigurationManager()).thenReturn(configurationManager);
        when(tenantResourceManagerDataHolder.getCarbonEventPublisherService()).thenReturn(carbonEventPublisherService);

        File sampleResourceFile = new File(getSamplesPath());
        InputStream fileStream = FileUtils.openInputStream(sampleResourceFile);
        when(configurationManager.getFileById(anyString(), anyString(), anyString())).thenReturn(fileStream);
        Resources resources = new Resources();
        Resource resource = new Resource();
        resource.setFiles(resourceFiles);
        List<Resource> resourceList = new ArrayList<Resource>();
        resourceList.add(resource);
        resources.setResources(resourceList);
        when(configurationManager.getResourcesByType(anyString())).thenReturn(resources);
        when(eventStreamService.getStreamDefinition(anyString(), anyString())).thenReturn(streamDefinition);
        List<EventStreamConfiguration> eventStreamConfigurationsList = new ArrayList<>();
        EventStreamConfiguration eventStreamConfiguration = new EventStreamConfiguration();
        eventStreamConfiguration.setFileName(EMAIL_PUBLISHER);
        when(TenantResourceManagerDataHolder.getInstance().getCarbonEventStreamService()
                .getAllEventStreamConfigurations()).thenReturn(eventStreamConfigurationsList);
    }

    @Test
    public void testCreatingConfigurationContext() {

        new TenantAwareAxis2ConfigurationContextObserver().creatingConfigurationContext(TENANT_ID);
        Assert.assertNotNull(TENANT_SPECIFIC_EMAIL_PUBLISHER,
                carbonEventPublisherService.getEventPublisherConfigurationFromMap());
    }

    private static String getSamplesPath() {

        if (StringUtils.isNotBlank(TenantAwareAxis2ConfigurationContextObserverTest.SAMPLE_RESOURCE_FILE_TXT)) {
            return Paths.get(System.getProperty("user.dir"), "src", "test", "resources", "sample",
                    TenantAwareAxis2ConfigurationContextObserverTest.SAMPLE_RESOURCE_FILE_TXT).toString();
        }
        throw new IllegalArgumentException("Sample name cannot be empty.");
    }
}
