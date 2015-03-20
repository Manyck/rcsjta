/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.CoreListener;
import com.gsma.rcs.core.ims.network.ImsConnectionManager;
import com.gsma.rcs.core.ims.network.ImsNetworkInterface;
import com.gsma.rcs.core.ims.network.gsm.CallManager;
import com.gsma.rcs.core.ims.network.sip.SipManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpConnection;
import com.gsma.rcs.core.ims.protocol.rtp.core.RtpSource;
import com.gsma.rcs.core.ims.protocol.sip.SipEventListener;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManager;
import com.gsma.rcs.core.ims.security.cert.KeyStoreManagerException;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceDispatcher;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.capability.CapabilityService;
import com.gsma.rcs.core.ims.service.extension.ExtensionManager;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpTransferManager;
import com.gsma.rcs.core.ims.service.ipcall.IPCallService;
import com.gsma.rcs.core.ims.service.presence.PresenceService;
import com.gsma.rcs.core.ims.service.richcall.RichcallService;
import com.gsma.rcs.core.ims.service.sip.SipService;
import com.gsma.rcs.core.ims.service.system.SystemRequestService;
import com.gsma.rcs.core.ims.service.terms.TermsConditionsService;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * IMS module
 * 
 * @author JM. Auffret
 */
public class ImsModule implements SipEventListener {
    /**
     * Core
     */
    private Core core;

    /**
     * IMS user profile
     */
    public static UserProfile IMS_USER_PROFILE;

    /**
     * IMS connection manager
     */
    private ImsConnectionManager connectionManager;

    /**
     * IMS services
     */
    private ImsService services[];

    /**
     * Service dispatcher
     */
    private ImsServiceDispatcher serviceDispatcher;

    /**
     * Call manager
     */
    private CallManager callManager;

    /**
     * flag to indicate whether instantiation is finished
     */
    private boolean isReady = false;

    final private ExtensionManager mExtensionManager;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     * 
     * @param core Core
     * @throws CoreException
     */
    public ImsModule(Core core) throws CoreException {
        boolean isLoggerActive = logger.isActivated();
        this.core = core;

        if (isLoggerActive) {
            logger.info("IMS module initialization");
        }

        // Create the IMS connection manager
        try {
            connectionManager = new ImsConnectionManager(this);
        } catch (Exception e) {
            if (isLoggerActive) {
                logger.error("IMS connection manager initialization has failed", e);
            }
            throw new CoreException("Can't instanciate the IMS connection manager");
        }
        RcsSettings rcsSettings = RcsSettings.getInstance();
        // Set general parameters
        SipManager.TIMEOUT = rcsSettings.getSipTransactionTimeout();
        RtpSource.CNAME = ImsModule.IMS_USER_PROFILE.getPublicUri();
        MsrpConnection.MSRP_TRACE_ENABLED = rcsSettings.isMediaTraceActivated();
        HttpTransferManager.HTTP_TRACE_ENABLED = rcsSettings.isMediaTraceActivated();

        // Load keystore for certificates
        try {
            KeyStoreManager.loadKeyStore();
        } catch (KeyStoreManagerException e) {
            if (isLoggerActive) {
                logger.error("Can't load keystore manager", e);
            }
            throw new CoreException("Keystore manager exception");
        }

        ContactsManager contactsManager = ContactsManager.getInstance();

        MessagingLog messagingLog = MessagingLog.getInstance();

        // Get capability extension manager
        mExtensionManager = ExtensionManager.getInstance();

        // Instantiates the IMS services
        services = new ImsService[8];

        // Create terms & conditions service
        services[ImsService.TERMS_SERVICE] = new TermsConditionsService(this, rcsSettings);

        // Create capability discovery service
        services[ImsService.CAPABILITY_SERVICE] = new CapabilityService(this, rcsSettings,
                contactsManager);

        // Create IM service (mandatory)
        services[ImsService.IM_SERVICE] = new InstantMessagingService(this, core, rcsSettings,
                contactsManager, messagingLog);

        // Create IP call service (optional)
        services[ImsService.IPCALL_SERVICE] = new IPCallService(this, rcsSettings);

        // Create richcall service (optional)
        services[ImsService.RICHCALL_SERVICE] = new RichcallService(this);

        // Create presence service (optional)
        services[ImsService.PRESENCE_SERVICE] = new PresenceService(this, rcsSettings,
                contactsManager);

        // Create generic SIP service
        services[ImsService.SIP_SERVICE] = new SipService(this);

        // Create system request service
        services[ImsService.SYSTEM_SERVICE] = new SystemRequestService(this);

        // Create the service dispatcher
        serviceDispatcher = new ImsServiceDispatcher(this);

        // Create the call manager
        callManager = new CallManager(this);

        isReady = true;

        if (isLoggerActive) {
            logger.info("IMS module has been created");
        }
    }

    /**
     * Returns the SIP manager
     * 
     * @return SIP manager
     */
    public SipManager getSipManager() {
        return getCurrentNetworkInterface().getSipManager();
    }

    /**
     * Returns the current network interface
     * 
     * @return Network interface
     */
    public ImsNetworkInterface getCurrentNetworkInterface() {
        return connectionManager.getCurrentNetworkInterface();
    }

    /**
     * Is connected to a Wi-Fi access
     * 
     * @return Boolean
     */
    public boolean isConnectedToWifiAccess() {
        return connectionManager.isConnectedToWifi();
    }

    /**
     * Is connected to a mobile access
     * 
     * @return Boolean
     */
    public boolean isConnectedToMobileAccess() {
        return connectionManager.isConnectedToMobile();
    }

    /**
     * Returns the ImsConnectionManager
     * 
     * @return ImsConnectionManager
     */
    public ImsConnectionManager getImsConnectionManager() {
        return connectionManager;
    }

    /**
     * Start the IMS module
     */
    public void start() {
        if (logger.isActivated()) {
            logger.info("Start the IMS module");
        }

        mExtensionManager.updateSupportedExtensions();

        // Start the service dispatcher
        serviceDispatcher.start();

        // Start call monitoring
        callManager.startCallMonitoring();

        if (logger.isActivated()) {
            logger.info("IMS module is started");
        }
    }

    /**
     * Stop the IMS module
     */
    public void stop() {
        if (logger.isActivated()) {
            logger.info("Stop the IMS module");
        }

        mExtensionManager.stop();

        // Stop call monitoring
        callManager.stopCallMonitoring();

        // Terminate the connection manager
        connectionManager.terminate();

        // Terminate the service dispatcher
        serviceDispatcher.terminate();

        if (logger.isActivated()) {
            logger.info("IMS module has been stopped");
        }
    }

    /**
     * Start IMS services
     */
    public void startImsServices() {
        // Start each services
        for (int i = 0; i < services.length; i++) {
            if (services[i].isActivated()) {
                if (logger.isActivated()) {
                    logger.info("Start IMS service: " + services[i].getClass().getName());
                }
                services[i].start();
            }
        }

        // Send call manager event
        getCallManager().connectionEvent(true);
    }

    /**
     * Stop IMS services
     */
    public void stopImsServices() {
        // Abort all pending sessions
        abortAllSessions();

        // Stop each services
        for (int i = 0; i < services.length; i++) {
            if (services[i].isActivated()) {
                if (logger.isActivated()) {
                    logger.info("Stop IMS service: " + services[i].getClass().getName());
                }
                services[i].stop();
            }
        }

        // Send call manager event
        getCallManager().connectionEvent(false);
    }

    /**
     * Check IMS services
     */
    public void checkImsServices() {
        for (int i = 0; i < services.length; i++) {
            if (services[i].isActivated()) {
                if (logger.isActivated()) {
                    logger.info("Check IMS service: " + services[i].getClass().getName());
                }
                services[i].check();
            }
        }
    }

    /**
     * Returns the call manager
     * 
     * @return Call manager
     */
    public CallManager getCallManager() {
        return callManager;
    }

    /**
     * Returns the IMS service
     * 
     * @param id Id of the IMS service
     * @return IMS service
     */
    public ImsService getImsService(int id) {
        return services[id];
    }

    /**
     * Returns the IMS services
     * 
     * @return Table of IMS service
     */
    public ImsService[] getImsServices() {
        return services;
    }

    /**
     * Returns the terms & conditions service
     * 
     * @return Terms & conditions service
     */
    public TermsConditionsService getTermsConditionsService() {
        return (TermsConditionsService) services[ImsService.TERMS_SERVICE];
    }

    /**
     * Returns the capability service
     * 
     * @return Capability service
     */
    public CapabilityService getCapabilityService() {
        return (CapabilityService) services[ImsService.CAPABILITY_SERVICE];
    }

    /**
     * Returns the IP call service
     * 
     * @return IP call service
     */
    public IPCallService getIPCallService() {
        return (IPCallService) services[ImsService.IPCALL_SERVICE];
    }

    /**
     * Returns the rich call service
     * 
     * @return Richcall service
     */
    public RichcallService getRichcallService() {
        return (RichcallService) services[ImsService.RICHCALL_SERVICE];
    }

    /**
     * Returns the presence service
     * 
     * @return Presence service
     */
    public PresenceService getPresenceService() {
        return (PresenceService) services[ImsService.PRESENCE_SERVICE];
    }

    /**
     * Returns the Instant Messaging service
     * 
     * @return Instant Messaging service
     */
    public InstantMessagingService getInstantMessagingService() {
        return (InstantMessagingService) services[ImsService.IM_SERVICE];
    }

    /**
     * Returns the SIP service
     * 
     * @return SIP service
     */
    public SipService getSipService() {
        return (SipService) services[ImsService.SIP_SERVICE];
    }

    /**
     * Returns the system request service
     * 
     * @return System request service
     */
    public SystemRequestService getSystemRequestService() {
        return (SystemRequestService) services[ImsService.SYSTEM_SERVICE];
    }

    /**
     * Return the core instance
     * 
     * @return Core instance
     */
    public Core getCore() {
        return core;
    }

    /**
     * Return the core listener
     * 
     * @return Core listener
     */
    public CoreListener getCoreListener() {
        return core.getListener();
    }

    /**
     * Receive SIP request
     * 
     * @param request SIP request
     */
    public void receiveSipRequest(SipRequest request) {
        // Post the incoming request to the service dispatcher
        serviceDispatcher.postSipRequest(request);
    }

    /**
     * Abort all sessions
     */
    public void abortAllSessions() {
        if (logger.isActivated()) {
            logger.debug("Abort all pending sessions");
        }
        for (ImsService service : getImsServices()) {
            service.abortAllSessions(ImsServiceSession.TERMINATION_BY_SYSTEM);
        }
    }

    /**
     * Check whether ImsModule instantiation has finished
     *
     * @return true if ImsModule is completely initialized
     */
    public boolean isReady() {
        return isReady;
    }

    /**
     * @return true is device is in roaming
     */
    public boolean isInRoaming() {
        return connectionManager.isInRoaming();
    }
}
