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

package com.gsma.rcs.core.ims.service;

import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Abstract IMS service
 * 
 * @author jexa7410
 */
public abstract class ImsService {
    /**
     * Terms & conditions service
     */
    public static final int TERMS_SERVICE = 0;

    /**
     * Capability service
     */
    public static final int CAPABILITY_SERVICE = 1;

    /**
     * Instant Messaging service
     */
    public static final int IM_SERVICE = 2;

    /**
     * IP call service
     */
    public static final int IPCALL_SERVICE = 3;

    /**
     * Richcall service
     */
    public static final int RICHCALL_SERVICE = 4;

    /**
     * Presence service
     */
    public static final int PRESENCE_SERVICE = 5;

    /**
     * SIP service
     */
    public static final int SIP_SERVICE = 6;

    /**
     * System request service
     */
    public static final int SYSTEM_SERVICE = 7;

    /**
     * Activation flag
     */
    private boolean activated = true;

    /**
     * Service state
     */
    private boolean started = false;

    /**
     * IMS module
     */
    private ImsModule imsModule;

    /**
     * ImsServiceSessionCache with session dialog path's CallId as key
     */
    private Map<String, ImsServiceSession> mImsServiceSessionCache = new HashMap<String, ImsServiceSession>();

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(ImsService.class.getSimpleName());

    protected final static class SharingDirection {

        public static final int UNIDIRECTIONAL = 1;

        public static final int BIDIRECTIONAL = 2;
    }

    protected static final int UNIDIRECTIONAL_SESSION_POSITION = 0;

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param activated Activation flag
     * @throws CoreException
     */
    public ImsService(ImsModule parent, boolean activated) throws CoreException {
        this.imsModule = parent;
        this.activated = activated;
    }

    /**
     * Is service activated
     * 
     * @return Boolean
     */
    public boolean isActivated() {
        return activated;
    }

    /**
     * Change the activation flag of the service
     * 
     * @param activated Activation flag
     */
    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    /**
     * Returns the IMS module
     * 
     * @return IMS module
     */
    public ImsModule getImsModule() {
        return imsModule;
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void addImsServiceSession(ImsServiceSession session) {
        mImsServiceSessionCache.put(session.getDialogPath().getCallId(), session);
    }

    /*
     * This method is by choice not synchronized here since the class extending this base-class will
     * need to handle the synchronization over a larger scope when calling this method anyway and we
     * would like to avoid double locks.
     */
    protected void removeImsServiceSession(ImsServiceSession session) {
        mImsServiceSessionCache.remove(session.getDialogPath().getCallId());
    }

    public ImsServiceSession getImsServiceSession(String callId) {
        synchronized (getImsServiceSessionOperationLock()) {
            return mImsServiceSessionCache.get(callId);
        }
    }

    protected Object getImsServiceSessionOperationLock() {
        return mImsServiceSessionCache;
    }

    /**
     * Is service started
     * 
     * @return Boolean
     */
    public boolean isServiceStarted() {
        return started;
    }

    /**
     * Set service state
     * 
     * @param state State
     */
    public void setServiceStarted(boolean state) {
        started = state;
    }

    /**
     * Start the IMS service
     */
    public abstract void start();

    /**
     * Stop the IMS service
     */
    public abstract void stop();

    /**
     * Check the IMS service
     */
    public abstract void check();

    public void abortAllSessions(int imsAbortionReason) {
        synchronized (getImsServiceSessionOperationLock()) {
            for (ImsServiceSession session : mImsServiceSessionCache.values()) {
                session.abortSession(imsAbortionReason);
            }
        }
    }

    /**
     * Send an error response to an invitation before to create a service session
     *
     * @param invite Invite request
     * @param error Error code
     */
    public void sendErrorResponse(SipRequest invite, int error) {
        try {
            if (logger.isActivated()) {
                logger.info("Send error " + error);
            }
            SipResponse resp = SipMessageFactory.createResponse(invite,
                    IdGenerator.getIdentifier(), error);

            // Send response
            getImsModule().getSipManager().sendSipResponse(resp);
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't send error " + error, e);
            }
        }
    }
}
