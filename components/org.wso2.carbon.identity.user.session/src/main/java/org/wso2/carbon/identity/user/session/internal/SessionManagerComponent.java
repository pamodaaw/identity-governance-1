/*
 *   Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.identity.user.session.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.application.authentication.framework.UserSessionManagementService;
import org.wso2.carbon.identity.user.session.manager.SessionManager;
import org.wso2.carbon.identity.user.session.manager.impl.SessionManagerImpl;

@Component(
        name = "identity.user.session.component",
        immediate = true
)
public class SessionManagerComponent {

    private static final Log log = LogFactory.getLog(SessionManagerComponent.class);

    @Activate
    protected void activate(ComponentContext componentContext) {

        try {
            BundleContext bundleContext = componentContext.getBundleContext();
            bundleContext.registerService(SessionManager.class.getName(), new SessionManagerImpl(), null);
            if (log.isDebugEnabled()) {
                log.debug("Session Manager bundle is activated");
            }

        } catch (Throwable e) {
            log.error("Error while activating SessionManagerComponent.", e);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext componentContext) {

        if (log.isDebugEnabled()) {
            log.debug("Session Manager bundle is de-activated");
        }
    }

    @Reference(
            service = UserSessionManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetUserSessionManagementService"
    )
    protected void setUserSessionManagementService(UserSessionManagementService sessionService) {

        if (log.isDebugEnabled()) {
            log.debug("Setting Session Management Service");
        }
        SessionManagerComponentDataHolder.getInstance().setUserSessionManagementService(sessionService);
    }

    protected void unsetUserSessionManagementService(UserSessionManagementService sessionService) {

        if (log.isDebugEnabled()) {
            log.debug("Unsetting Session Management Service");
        }
        SessionManagerComponentDataHolder.getInstance().setUserSessionManagementService(null);
    }
}
