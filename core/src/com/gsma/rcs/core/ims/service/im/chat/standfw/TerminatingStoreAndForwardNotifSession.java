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

package com.gsma.rcs.core.ims.service.im.chat.standfw;

import java.io.IOException;
import java.util.Vector;

import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpEventListener;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpManager;
import com.gsma.rcs.core.ims.protocol.msrp.MsrpSession;
import com.gsma.rcs.core.ims.protocol.sdp.MediaAttribute;
import com.gsma.rcs.core.ims.protocol.sdp.MediaDescription;
import com.gsma.rcs.core.ims.protocol.sdp.SdpParser;
import com.gsma.rcs.core.ims.protocol.sdp.SdpUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.protocol.sip.SipTransactionContext;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsServiceError;
import com.gsma.rcs.core.ims.service.ImsServiceSession;
import com.gsma.rcs.core.ims.service.im.InstantMessagingService;
import com.gsma.rcs.core.ims.service.im.chat.ChatError;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimMessage;
import com.gsma.rcs.core.ims.service.im.chat.cpim.CpimParser;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.NetworkRessourceManager;
import com.gsma.rcs.utils.PhoneUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Terminating Store & Forward session for push notifications
 * 
 * @author jexa7410
 */
public class TerminatingStoreAndForwardNotifSession extends OneToOneChatSession implements
        MsrpEventListener {
    /**
     * MSRP manager
     */
    private MsrpManager msrpMgr = null;

    /**
     * The logger
     */
    private static final Logger logger = Logger
            .getLogger(TerminatingStoreAndForwardNotifSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param invite Initial INVITE request
     * @param contact the remote ContactId
     */
    public TerminatingStoreAndForwardNotifSession(ImsService parent, SipRequest invite,
            ContactId contact) {
        super(parent, contact, PhoneUtils.formatContactIdToUri(contact));

        // Create the MSRP manager
        int localMsrpPort = NetworkRessourceManager.generateLocalMsrpPort();
        String localIpAddress = getImsService().getImsModule().getCurrentNetworkInterface()
                .getNetworkAccess().getIpAddress();
        msrpMgr = new MsrpManager(localIpAddress, localMsrpPort);
        if (parent.getImsModule().isConnectedToWifiAccess()) {
            msrpMgr.setSecured(RcsSettings.getInstance().isSecureMsrpOverWifi());
        }

        // Create dialog path
        createTerminatingDialogPath(invite);
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (logger.isActivated()) {
                logger.info("Initiate a new store & forward session for notifications");
            }

            // Parse the remote SDP part
            SdpParser parser = new SdpParser(getDialogPath().getRemoteContent().getBytes());
            Vector<MediaDescription> media = parser.getMediaDescriptions();
            MediaDescription mediaDesc = media.elementAt(0);
            MediaAttribute attr1 = mediaDesc.getMediaAttribute("path");
            String remotePath = attr1.getValue();
            String remoteHost = SdpUtils.extractRemoteHost(parser.sessionDescription, mediaDesc);
            int remotePort = mediaDesc.port;

            // Changed by Deutsche Telekom
            String fingerprint = SdpUtils.extractFingerprint(parser, mediaDesc);

            // Extract the "setup" parameter
            String remoteSetup = "passive";
            MediaAttribute attr2 = mediaDesc.getMediaAttribute("setup");
            if (attr2 != null) {
                remoteSetup = attr2.getValue();
            }
            if (logger.isActivated()) {
                logger.debug("Remote setup attribute is " + remoteSetup);
            }

            // Set setup mode
            String localSetup = createSetupAnswer(remoteSetup);
            if (logger.isActivated()) {
                logger.debug("Local setup attribute is " + localSetup);
            }

            // Set local port
            int localMsrpPort;
            if (localSetup.equals("active")) {
                localMsrpPort = getMsrpMgr().getLocalMsrpPort();
            } else {
                localMsrpPort = 9; // See RFC4145, Page 4
            }

            // Build SDP part
            String ipAddress = getDialogPath().getSipStack().getLocalIpAddress();
            String sdp = SdpUtils.buildChatSDP(ipAddress, localMsrpPort, getMsrpMgr()
                    .getLocalSocketProtocol(), getAcceptTypes(), getWrappedTypes(), localSetup,
                    getMsrpMgr().getLocalMsrpPath(), getSdpDirection());

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Test if the session should be interrupted
            if (isInterrupted()) {
                if (logger.isActivated()) {
                    logger.debug("Session has been interrupted: end of processing");
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

                            // Send an empty packet
                            sendEmptyDataChunk();
                        } catch (IOException e) {
                            if (logger.isActivated()) {
                                logger.error("Can't create the MSRP server session", e);
                            }
                        }
                    }
                };
                thread.start();
            }

            // Create a 200 OK response
            if (logger.isActivated()) {
                logger.info("Send 200 OK");
            }
            SipResponse resp = SipMessageFactory.create200OkInviteResponse(getDialogPath(),
                    InstantMessagingService.CHAT_FEATURE_TAGS, sdp);

            // The signalisation is established
            getDialogPath().sigEstablished();

            // Send response
            SipTransactionContext ctx = getImsService().getImsModule().getSipManager()
                    .sendSipMessageAndWait(resp);

            // Analyze the received response
            if (ctx.isSipAck()) {
                // ACK received
                if (logger.isActivated()) {
                    logger.info("ACK request received");
                }

                // The session is established
                getDialogPath().sessionEstablished();

                // Create the MSRP client session
                if (localSetup.equals("active")) {
                    // Active mode: client should connect
                    MsrpSession session = getMsrpMgr().createMsrpClientSession(remoteHost,
                            remotePort, remotePath, this, fingerprint);
                    session.setFailureReportOption(false);
                    session.setSuccessReportOption(false);

                    // Open the MSRP session
                    getMsrpMgr().openMsrpSession();

                    // Send an empty packet
                    sendEmptyDataChunk();
                }

                // Start the activity manager
                getActivityManager().start();

            } else {
                if (logger.isActivated()) {
                    logger.debug("No ACK received for INVITE");
                }

                // No response received: timeout
                handleError(new ChatError(ChatError.SESSION_INITIATION_FAILED));
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Session initiation has failed", e);
            }

            // Unexpected error
            handleError(new ChatError(ChatError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

    /**
     * Returns the MSRP manager
     * 
     * @return MSRP manager
     */
    public MsrpManager getMsrpMgr() {
        return msrpMgr;
    }

    /**
     * Close the MSRP session
     */
    public void closeMsrpSession() {
        if (getMsrpMgr() != null) {
            getMsrpMgr().closeSession();
            if (logger.isActivated()) {
                logger.debug("MSRP session has been closed");
            }
        }
    }

    /**
     * Handle error
     * 
     * @param error Error
     */
    public void handleError(ImsServiceError error) {
        // Error
        if (logger.isActivated()) {
            logger.info("Session error: " + error.getErrorCode() + ", reason=" + error.getMessage());
        }

        // Close media session
        closeMediaSession();

        // Remove the current session
        removeSession();
    }

    /**
     * Data has been transfered
     * 
     * @param msgId Message ID
     */
    public void msrpDataTransfered(String msgId) {
        // Not used in terminating side
    }

    /**
     * Data transfer has been received
     * 
     * @param msgId Message ID
     * @param data Received data
     * @param mimeType Data mime-type
     */
    public void msrpDataReceived(String msgId, byte[] data, String mimeType) {
        if (logger.isActivated()) {
            logger.info("Data received (type " + mimeType + ")");
        }

        // Update the activity manager
        getActivityManager().updateActivity();

        if ((data == null) || (data.length == 0)) {
            // By-pass empty data
            if (logger.isActivated()) {
                logger.debug("By-pass received empty data");
            }
            return;
        }

        if (ChatUtils.isMessageCpimType(mimeType)) {
            // Receive a CPIM message
            try {
                CpimParser cpimParser = new CpimParser(data);
                CpimMessage cpimMsg = cpimParser.getCpimMessage();
                if (cpimMsg != null) {
                    String contentType = cpimMsg.getContentType();
                    String from = cpimMsg.getHeader(CpimMessage.HEADER_FROM);

                    if (ChatUtils.isMessageImdnType(contentType)) {
                        try {
                            ContactId contact = ContactUtils.createContactId(from);
                            // Receive an IMDN report
                            receiveMessageDeliveryStatus(contact, cpimMsg.getMessageContent());
                        } catch (RcsContactFormatException e) {
                            // Receive an IMDN report
                            receiveMessageDeliveryStatus(getRemoteContact(),
                                    cpimMsg.getMessageContent());
                        }
                    }
                }
            } catch (Exception e) {
                if (logger.isActivated()) {
                    logger.error("Can't parse the CPIM message", e);
                }
            }
        } else {
            // Not supported content
            if (logger.isActivated()) {
                logger.debug("Not supported content " + mimeType + " in chat session");
            }
        }
    }

    /**
     * Data transfer in progress
     * 
     * @param currentSize Current transfered size in bytes
     * @param totalSize Total size in bytes
     */
    public void msrpTransferProgress(long currentSize, long totalSize) {
        // Not used by S&F
    }

    /**
     * Data transfer has been aborted
     */
    public void msrpTransferAborted() {
        // Not used by S&F
    }

    /**
     * Data transfer error
     *
     * @param msgId Message ID
     * @param error Error code
     */
    public void msrpTransferError(String msgId, String error) {
        if (logger.isActivated()) {
            logger.info("Data transfer error " + error);
        }
    }

    /**
     * Send an empty data chunk
     */
    public void sendEmptyDataChunk() {
        try {
            msrpMgr.sendEmptyChunk();
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Problem while sending empty data chunk", e);
            }
        }
    }

    /**
     * Receive a message delivery status (XML document)
     * 
     * @param contact Contact identifier
     * @param xml XML document
     */
    public void receiveMessageDeliveryStatus(ContactId contact, String xml) {
        try {
            ImdnDocument imdn = ChatUtils.parseDeliveryReport(xml);
            if (imdn == null) {
                return;
            }

            boolean isFileTransfer = MessagingLog.getInstance().isFileTransfer(imdn.getMsgId());
            if (isFileTransfer) {
                ((InstantMessagingService) getImsService())
                        .receiveFileDeliveryStatus(contact, imdn);

            } else {
                // Notify the message delivery outside of the chat
                // session
                getImsService().getImsModule().getCore().getListener()
                        .handleMessageDeliveryStatus(contact, imdn);

            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Can't parse IMDN document", e);
            }
        }
    }

    // Changed by Deutsche Telekom
    @Override
    public String getSdpDirection() {
        return SdpUtils.DIRECTION_RECVONLY;
    }

    @Override
    public boolean isInitiatedByRemote() {
        return true;
    }

    @Override
    public void startSession() {
        ContactId contact = getRemoteContact();
        if (logger.isActivated()) {
            logger.debug("Start OneToOneChatSession with '" + contact + "'");
        }
        InstantMessagingService imService = getImsService().getImsModule()
                .getInstantMessagingService();
        OneToOneChatSession currentSession = imService.getOneToOneChatSession(contact);
        if (currentSession != null) {
            boolean currentSessionInitiatedByRemote = currentSession.isInitiatedByRemote();
            boolean currentSessionEstablished = currentSession.getDialogPath()
                    .isSessionEstablished();
            if (!currentSessionEstablished && !currentSessionInitiatedByRemote) {
                /*
                 * Rejecting the NEW invitation since there is already a PENDING OneToOneChatSession
                 * that was locally originated with the same contact.
                 */
                if (logger.isActivated()) {
                    logger.warn("Rejecting OneToOneChatSession (session id '" + getSessionID()
                            + "') with '" + contact + "'");
                }
                rejectSession();
                return;
            }
            /*
             * If this oneToOne session does NOT already contain another oneToOne chat session which
             * in state PENDING and also LOCALLY originating we should leave (reject or abort) the
             * CURRENT rcs chat session if there is one and replace it with the new one.
             */
            if (logger.isActivated()) {
                logger.warn("Rejecting/Aborting existing OneToOneChatSession (session id '"
                        + getSessionID() + "') with '" + contact + "'");
            }
            if (currentSessionInitiatedByRemote) {
                if (currentSessionEstablished) {
                    currentSession.abortSession(ImsServiceSession.TERMINATION_BY_USER);
                } else {
                    currentSession.rejectSession();
                }
            } else {
                currentSession.abortSession(ImsServiceSession.TERMINATION_BY_USER);
            }
        }
        imService.addSession(this);
        start();
    }

    @Override
    public void removeSession() {
        getImsService().getImsModule().getInstantMessagingService().removeSession(this);
    }
}
