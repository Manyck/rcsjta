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

package com.gsma.rcs.core.ims.service.sip.messaging;

import static com.gsma.rcs.utils.StringUtils.UTF8;

import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.SessionTimerManager;
import com.gsma.rcs.core.ims.service.sip.GenericSipSession;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import android.content.Intent;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * Terminating SIP MSRP session
 * 
 * @author jexa7410
 */
public class TerminatingSipMsrpSession extends GenericSipMsrpSession {
    private final static Logger sLogger = Logger.getLogger(TerminatingSipMsrpSession.class
            .getSimpleName());

    private final Intent mSessionInvite;

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact
     * @param sessionInvite
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     */
    public TerminatingSipMsrpSession(ImsService parent, SipRequest invite, ContactId contact,
            Intent sessionInvite, RcsSettings rcsSettings, long timestamp,
            ContactManager contactManager) {
        super(parent, contact, GenericSipSession.getIariFeatureTag(invite.getFeatureTags()),
                rcsSettings, timestamp, contactManager);

        mSessionInvite = sessionInvite;

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        final boolean logActivated = sLogger.isActivated();
        if (logActivated) {
            sLogger.info("Initiate a new MSRP session as terminating");
        }
        try {
            send180Ringing(getDialogPath().getInvite(), getDialogPath().getLocalTag());

            Collection<ImsSessionListener> listeners = getListeners();
            ContactId contact = getRemoteContact();
            for (ImsSessionListener listener : listeners) {
                ((SipSessionListener) listener).handleSessionInvited(contact, mSessionInvite);
            }

            InvitationStatus answer = waitInvitationAnswer();
            switch (answer) {
                case INVITATION_REJECTED:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected by user");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_USER);
                    }
                    return;

                case INVITATION_TIMEOUT:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected on timeout");
                    }

                    // Ringing period timeout
                    send486Busy(getDialogPath().getInvite(), getDialogPath().getLocalTag());

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_TIMEOUT);
                    }
                    return;

                case INVITATION_REJECTED_BY_SYSTEM:
                    if (logActivated) {
                        sLogger.debug("Session has been aborted by system");
                    }
                    removeSession();
                    return;

                case INVITATION_CANCELED:
                    if (logActivated) {
                        sLogger.debug("Session has been rejected by remote");
                    }

                    removeSession();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionRejected(contact,
                                TerminationReason.TERMINATION_BY_REMOTE);
                    }
                    return;

                case INVITATION_ACCEPTED:
                    setSessionAccepted();

                    for (ImsSessionListener listener : listeners) {
                        listener.handleSessionAccepted(contact);
                    }
                    break;

                case INVITATION_DELETED:
                    if (logActivated) {
                        sLogger.debug("Session has been deleted");
                    }
                    removeSession();
                    return;

                default:
                    if (logActivated) {
                        sLogger.debug("Unknown invitation answer in run; answer=".concat(String
                                .valueOf(answer)));
                    }
                    return;
            }

            // Parse the remote SDP part
            String remoteSdp = getDialogPath().getInvite().getSdpContent();
            SdpParser parser = new SdpParser(remoteSdp.getBytes(UTF8));
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.port;

            // Extract the "setup" parameter
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logActivated) {
                sLogger.debug("Remote setup attribute is " + remoteSetup);
            }

            // Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logActivated) {
                sLogger.debug("Local setup attribute is " + localSetup);
            }

            // Build SDP answer
            String sdp = generateSdp(localSetup);

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Create the MSRP server session
            if (localSetup.equals("passive")) {
                // Passive mode: client wait a connection
                MsrpSession session = getMsrpMgr().createMsrpServerSession(remotePath, this);
                session.setFailureReportOption(false);
                session.setSuccessReportOption(false);

                // Open the connection
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            // Open the MSRP session
                            getMsrpMgr().openMsrpSession();
                        } catch (IOException e) {
                            if (logActivated) {
                                sLogger.error("Can't create the MSRP server session", e);
                            }
                        }
                    }
                };
                thread.start();
            } else {
                // Active mode: client should connect
                // MSRP session without TLS
                MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost, remotePort,
                        remotePath, this, null);
                session.setFailureReportOption(false);
                session.setSuccessReportOption(false);

                // Open the MSRP session
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            getMsrpMgr().openMsrpSession();
                        } catch (IOException e) {
                            if (logActivated) {
                                sLogger.error("Can't create the MSRP server session", e);
                            }
                        }
                    }
                };
                thread.start();
            }

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logActivated) {
                    sLogger.debug("Session has been interrupted: end of processing");
                }
                return;
            }

            // Create a 200 OK response
            if (logActivated) {
                sLogger.info("Send 200 OK");
            }
            SipResponse resp = create200OKResponse();

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (logActivated) {
                    sLogger.info("ACK request received");
                }

                // The session is established
                getDialogPath().sessionEstablished();

                // Start session timer
                if (getSessionTimerManager().isSessionTimerActivated(resp)) {
                    getSessionTimerManager().start(SessionTimerManager.UAS_ROLE,
                            getDialogPath().getSessionExpireTime());
                }

                // Notify listeners
                for (int j = 0; j < getListeners().size(); j++) {
                    getListeners().get(j).handleSessionStarted(contact);
                }
            } else {
                if (logActivated) {
                    sLogger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED));
            }
        } catch (Exception e) {
            if (logActivated) {
                sLogger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new SipSessionError(SipSessionError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

}
