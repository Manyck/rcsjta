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

package com.gsma.rcs.core.ims.service.im;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.core.CoreException;
import com.gsma.rcs.core.content.ContentManager;
import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.network.sip.FeatureTags;
import com.gsma.rcs.core.ims.network.sip.SipMessageFactory;
import com.gsma.rcs.core.ims.network.sip.SipUtils;
import com.gsma.rcs.core.ims.protocol.sip.SipDialogPath;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.capability.Capabilities;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatSession;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.core.ims.service.im.chat.GroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.OriginatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RejoinGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.RestartGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingAdhocGroupChatSession;
import com.gsma.rcs.core.ims.service.im.chat.TerminatingOneToOneChatSession;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.StoreAndForwardManager;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatMessageSession;
import com.gsma.rcs.core.ims.service.im.chat.standfw.TerminatingStoreAndForwardOneToOneChatNotificationSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.core.ims.service.im.filetransfer.ImsFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.DownloadFromInviteFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.FileTransferHttpInfoDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.HttpFileTransferSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.http.OriginatingHttpGroupFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.OriginatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.im.filetransfer.msrp.TerminatingMsrpFileSharingSession;
import com.gsma.rcs.core.ims.service.upload.FileUploadSession;
import com.gsma.rcs.provider.eab.ContactsManager;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData.FileTransferProtocol;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.IdGenerator;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.GroupChat.ParticipantStatus;
import com.gsma.services.rcs.chat.GroupChat.ReasonCode;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax2.sip.header.ContactHeader;
import javax2.sip.message.Response;

/**
 * Instant messaging services (1-1 chat, group chat and file transfer)
 * 
 * @author Jean-Marc AUFFRET
 */
public class InstantMessagingService extends ImsService {

    private final Core mCore;

    private final RcsSettings mRcsSettings;

    private final ContactsManager mContactsManager;

    private final MessagingLog mMessagingLog;

    /**
     * OneToOneChatSessionCache with ContactId as key
     */
    private Map<ContactId, OneToOneChatSession> mOneToOneChatSessionCache = new HashMap<ContactId, OneToOneChatSession>();

    /**
     * StoreAndForwardMsgSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatMessageSession> mStoreAndForwardMsgSessionCache = new HashMap<ContactId, TerminatingStoreAndForwardOneToOneChatMessageSession>();

    /**
     * StoreAndForwardNotifSessionCache with ContactId as key
     */
    private Map<ContactId, TerminatingStoreAndForwardOneToOneChatNotificationSession> mStoreAndForwardNotifSessionCache = new HashMap<ContactId, TerminatingStoreAndForwardOneToOneChatNotificationSession>();

    /**
     * GroupChatSessionCache with ChatId as key
     */
    private Map<String, GroupChatSession> mGroupChatSessionCache = new HashMap<String, GroupChatSession>();

    /**
     * FileSharingSessionCache with FileTransferId as key
     */
    private Map<String, FileSharingSession> mFileTransferSessionCache = new HashMap<String, FileSharingSession>();

    /**
     * FileUploadSessionCache with UploadId as key
     */
    private Map<String, FileUploadSession> mFileUploadSessionCache = new HashMap<String, FileUploadSession>();

    /**
     * GroupChatConferenceSubscriberCache with Conference subscriber's dialog path CallId as key
     */
    private Map<String, GroupChatSession> mGroupChatConferenceSubscriberCache = new HashMap<String, GroupChatSession>();

    /**
     * Chat features tags
     */
    public final static String[] CHAT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    /**
     * File transfer features tags
     */
    public final static String[] FT_FEATURE_TAGS = {
        FeatureTags.FEATURE_OMA_IM
    };

    /**
     * IMDN manager
     */
    private ImdnManager mImdnMgr;

    /**
     * Store & Forward manager
     */
    private final StoreAndForwardManager mStoreAndFwdMgr = new StoreAndForwardManager(this);

    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(InstantMessagingService.class.getName());

    /**
     * Constructor
     * 
     * @param parent IMS module
     * @param core Core
     * @param rcsSettings RcsSettings
     * @param contactsManager ContactsManager
     * @param messagingLog MessagingLog
     * @throws CoreException
     */
    public InstantMessagingService(ImsModule parent, Core core, RcsSettings rcsSettings,
            ContactsManager contactsManager, MessagingLog messagingLog) throws CoreException {
        super(parent, true);
        mCore = core;
        mRcsSettings = rcsSettings;
        mContactsManager = contactsManager;
        mMessagingLog = messagingLog;
    }

    private void handleFileTransferInvitationRejected(SipRequest invite,
            FileTransfer.ReasonCode reasonCode) {
        ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
        MmContent content = ContentManager.createMmContentFromSdp(invite, mRcsSettings);
        MmContent fileIcon = FileTransferUtils.extractFileIcon(invite, mRcsSettings);
        getImsModule().getCore().getListener()
                .handleFileTransferInvitationRejected(contact, content, fileIcon, reasonCode);
    }

    private void handleGroupChatInvitationRejected(SipRequest invite, ReasonCode reasonCode) {
        String chatId = ChatUtils.getContributionId(invite);
        ContactId contact = ChatUtils.getReferredIdentityAsContactId(invite);
        String subject = ChatUtils.getSubject(invite);
        Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(invite,
                ParticipantStatus.FAILED);
        getImsModule()
                .getCore()
                .getListener()
                .handleGroupChatInvitationRejected(chatId, contact, subject, participants,
                        reasonCode);
    }

    /**
     * Start the IMS service
     */
    public synchronized void start() {
        if (isServiceStarted()) {
            // Already started
            return;
        }
        setServiceStarted(true);

        // Start IMDN manager
        mImdnMgr = new ImdnManager(this, mRcsSettings, mMessagingLog);
        mImdnMgr.start();

        mCore.getListener().tryToStartImServiceTasks(this);
    }

    /**
     * Stop the IMS service
     */
    public synchronized void stop() {
        if (!isServiceStarted()) {
            // Already stopped
            return;
        }
        setServiceStarted(false);

        // Stop IMDN manager
        mImdnMgr.terminate();
        mImdnMgr.interrupt();
    }

    /**
     * Check the IMS service
     */
    public void check() {
    }

    /**
     * Returns the IMDN manager
     * 
     * @return IMDN manager
     */
    public ImdnManager getImdnManager() {
        return mImdnMgr;
    }

    /**
     * Get Store & Forward manager
     */
    public StoreAndForwardManager getStoreAndForwardManager() {
        return mStoreAndFwdMgr;
    }

    public void addSession(OneToOneChatSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mOneToOneChatSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final OneToOneChatSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mOneToOneChatSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public OneToOneChatSession getOneToOneChatSession(ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get OneToOneChatSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mOneToOneChatSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardMsgSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final TerminatingStoreAndForwardOneToOneChatMessageSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mStoreAndForwardMsgSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public TerminatingStoreAndForwardOneToOneChatMessageSession getStoreAndForwardMsgSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardMsgSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardMsgSessionCache.get(contact);
        }
    }

    public void addSession(TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add StoreAndForwardNotifSessionCache with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mStoreAndForwardNotifSessionCache.put(contact, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(
            final TerminatingStoreAndForwardOneToOneChatNotificationSession session) {
        final ContactId contact = session.getRemoteContact();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder(
                    "Remove StoreAndForwardNotifSessionCache with contact '").append(contact)
                    .append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mStoreAndForwardNotifSessionCache.remove(contact);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public TerminatingStoreAndForwardOneToOneChatNotificationSession getStoreAndForwardNotifSession(
            ContactId contact) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get StoreAndForwardNotifSession with contact '")
                    .append(contact).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mStoreAndForwardNotifSessionCache.get(contact);
        }
    }

    public void addSession(GroupChatSession session) {
        String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatSessionCache.put(chatId, session);
            addImsServiceSession(session);
        }
    }

    public void removeSession(final GroupChatSession session) {
        final String chatId = session.getContributionID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    SipDialogPath conferenceSubscriberDialogPath = session
                            .getConferenceEventSubscriber().getDialogPath();
                    if (conferenceSubscriberDialogPath != null) {
                        mGroupChatConferenceSubscriberCache.remove(conferenceSubscriberDialogPath
                                .getCallId());
                    }
                    mGroupChatSessionCache.remove(chatId);
                    removeImsServiceSession(session);
                }
            }
        }.start();
    }

    public GroupChatSession getGroupChatSession(String chatId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with chatId '").append(chatId)
                    .append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatSessionCache.get(chatId);
        }
    }

    public void addGroupChatConferenceSubscriber(String callId, GroupChatSession session) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mGroupChatConferenceSubscriberCache.put(callId, session);
        }
    }

    public void removeGroupChatConferenceSubscriber(final String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove GroupChatConferenceSubscriber with callId '")
                    .append(callId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mGroupChatConferenceSubscriberCache.remove(callId);
                }
            }
        }.start();
    }

    public GroupChatSession getGroupChatSessionOfConferenceSubscriber(String callId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get GroupChatSession with ConferenceSunscriber '")
                    .append(callId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mGroupChatConferenceSubscriberCache.get(callId);
        }
    }

    public boolean isChatSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxChatSessions == 0 means that the allowed number of chat sessions in use is
             * disabled
             */
            int maxChatSessions = mRcsSettings.getMaxChatSessions();
            if (maxChatSessions == 0) {
                return true;
            }

            return mOneToOneChatSessionCache.size() + mGroupChatSessionCache.size() < maxChatSessions;
        }
    }

    public void assertAvailableChatSession(String errorMessage) throws CoreException {
        if (!isChatSessionAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.error(errorMessage);
            }
            /*
             * TODO : Proper exception handling will be added here as part of the CR037
             * implementation
             */
            throw new CoreException(errorMessage);
        }
    }

    public void addSession(FileSharingSession session) {
        String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileTransferSessionCache.put(fileTransferId, session);
            /*
             * Only FileSharingSessions of type ImsFileSharingSession has a dialog path. Hence add
             * only those type of sessions to the ImsServiceSession cache and add
             * HttpFileTransferSession to ImsServiceSessionWithoutDialogPath cache.
             */
            if (session instanceof ImsFileSharingSession) {
                addImsServiceSession(session);
            } else if (session instanceof HttpFileTransferSession) {
                addImsServiceSessionWithoutDialogPath(session);
            }
        }
    }

    public void removeSession(final FileSharingSession session) {
        final String fileTransferId = session.getFileTransferId();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mFileTransferSessionCache.remove(fileTransferId);
                    /*
                     * Only FileSharingSessions of type ImsFileSharingSession has a dialog path.
                     * Hence it is possible to remove only those type of sessions from the
                     * ImsServiceSession cache and remove HttpFileTransferSession from
                     * ImsServiceSessionWithoutDialogPath cache.
                     */
                    if (session instanceof ImsFileSharingSession) {
                        removeImsServiceSession(session);
                    } else if (session instanceof HttpFileTransferSession) {
                        removeImsServiceSessionWithoutDialogPath(session);
                    }
                }
            }
        }.start();
    }

    public FileSharingSession getFileSharingSession(String fileTransferId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileSharingSession with fileTransfer ID '")
                    .append(fileTransferId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileTransferSessionCache.get(fileTransferId);
        }
    }

    public void addSession(FileUploadSession session) {
        String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Add FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            mFileUploadSessionCache.put(uploadId, session);
        }
    }

    public void removeSession(final FileUploadSession session) {
        final String uploadId = session.getUploadID();
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Remove FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        /*
         * Performing remove session operation on a new thread so that ongoing threads trying to get
         * that session can finish up before it is actually removed
         */
        new Thread() {
            @Override
            public void run() {
                synchronized (getImsServiceSessionOperationLock()) {
                    mFileUploadSessionCache.remove(uploadId);
                }
            }
        }.start();
    }

    public FileUploadSession getFileUploadSession(String uploadId) {
        if (sLogger.isActivated()) {
            sLogger.debug(new StringBuilder("Get FileUploadSession with upload ID '")
                    .append(uploadId).append("'").toString());
        }
        synchronized (getImsServiceSessionOperationLock()) {
            return mFileUploadSessionCache.get(uploadId);
        }
    }

    public boolean isFileTransferSessionAvailable() {
        synchronized (getImsServiceSessionOperationLock()) {
            /*
             * maxFtSessions == 0 means that the checking of allowed number of file transfer
             * sessions in use is disabled
             */
            int maxFileTransferSessions = mRcsSettings.getMaxFileTransferSessions();
            if (maxFileTransferSessions == 0) {
                return true;
            }

            return mFileTransferSessionCache.size() + mFileUploadSessionCache.size() < maxFileTransferSessions;
        }
    }

    public void assertAvailableFileTransferSession(String errorMessage) throws CoreException {
        if (!isFileTransferSessionAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.error(errorMessage);
            }
            /*
             * TODO : Proper exception handling will be added here as part of the CR037
             * implementation
             */
            throw new CoreException(errorMessage);
        }
    }

    public void assertFileSizeNotExceedingMaxLimit(long size, String errorMessage)
            throws CoreException {
        /*
         * maxFtSize == 0 means that the checking of allowed number of file transfer size in use is
         * disabled
         */
        long maxFileTransferSize = mRcsSettings.getMaxFileTransferSize();
        if (maxFileTransferSize > 0 && size > maxFileTransferSize) {
            if (sLogger.isActivated()) {
                sLogger.error(errorMessage);
            }
            /*
             * TODO : Proper exception handling will be added here as part of the CR037
             * implementation
             */
            throw new CoreException(errorMessage);
        }
    }

    /**
     * Checks if max number of concurrent outgoing file transfer sessions reached
     * 
     * @return boolean
     */
    public boolean isMaxConcurrentOutgoingFileTransfersReached() {
        int nrOfConcurrentOutgoingFileTransferSessions = 0;
        synchronized (getImsServiceSessionOperationLock()) {
            for (FileSharingSession session : mFileTransferSessionCache.values()) {
                if (!session.isInitiatedByRemote()) {
                    nrOfConcurrentOutgoingFileTransferSessions++;
                }
            }
            /*
             * maxConcurrentOutgoingFilrTransferSessions == 0 means that the checking of allowed
             * concurrent number of outgoing file transfers in use is disabled
             */
            int maxConcurrentOutgoingFileTransferSessions = mRcsSettings
                    .getMaxConcurrentOutgoingFileTransferSessions();
            if (maxConcurrentOutgoingFileTransferSessions == 0) {
                return false;
            }
            if (nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions) {
                return true;
            }
            nrOfConcurrentOutgoingFileTransferSessions += mFileUploadSessionCache.size();
            return nrOfConcurrentOutgoingFileTransferSessions >= maxConcurrentOutgoingFileTransferSessions;
        }
    }

    /**
     * Initiate a file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param contact Remote contact identifier
     * @param content Content of file to sent
     * @param fileIcon Content of fileicon
     * @return File transfer session
     */
    public FileSharingSession initiateFileTransferSession(String fileTransferId, ContactId contact,
            MmContent content, MmContent fileIcon) {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate a file transfer session with contact " + contact + ", file "
                    + content.toString());
        }

        boolean isFToHttpSupportedByRemote = false;
        Capabilities remoteCapability = mContactsManager.getContactCapabilities(contact);
        if (remoteCapability != null) {
            isFToHttpSupportedByRemote = remoteCapability.isFileTransferHttpSupported();
        }

        Capabilities myCapability = mRcsSettings.getMyCapabilities();
        if (isFToHttpSupportedByRemote && myCapability.isFileTransferHttpSupported()) {
            if (FileTransferProtocol.HTTP.equals(mRcsSettings.getFtProtocol())) {
                return new OriginatingHttpFileSharingSession(fileTransferId, this, content,
                        contact, fileIcon, UUID.randomUUID().toString(), mCore, mMessagingLog,
                        mRcsSettings);
            }
        }

        if (remoteCapability != null && remoteCapability.isFileTransferThumbnailSupported()) {
            fileIcon = null;
        }
        return new OriginatingMsrpFileSharingSession(fileTransferId, this, content, contact,
                fileIcon, mRcsSettings);
    }

    /**
     * Initiate a group file transfer session
     * 
     * @param fileTransferId File transfer Id
     * @param content The file content to be sent
     * @param fileIcon Content of fileicon
     * @param groupChatId Chat contribution ID
     * @param groupChatSessionId GroupChatSession Id
     * @return File transfer session
     */
    public FileSharingSession initiateGroupFileTransferSession(String fileTransferId,
            MmContent content, MmContent fileIcon, String groupChatId, String groupChatSessionId) {
        if (sLogger.isActivated()) {
            sLogger.info("Send file " + content.toString() + " to " + groupChatId);
        }

        FileSharingSession session = new OriginatingHttpGroupFileSharingSession(fileTransferId,
                this, content, fileIcon, ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
                groupChatSessionId, groupChatId, UUID.randomUUID().toString(), mCore, mRcsSettings,
                mMessagingLog);

        return session;
    }

    /**
     * Receive a file transfer invitation
     * 
     * @param invite Initial invite
     */
    public void receiveFileTransferInvitation(SipRequest invite) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a file transfer session invitation");
        }

        try {
            // Test if the contact is blocked
            ContactId remote = ContactUtils.createContactId(SipUtils.getAssertedIdentity(invite));
            if (mContactsManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked: automatically reject the file transfer invitation");
                }

                handleFileTransferInvitationRejected(invite, FileTransfer.ReasonCode.REJECTED_SPAM);

                // Send a 603 Decline response
                sendErrorResponse(invite, Response.DECLINE);
                return;
            }

            // Test number of sessions
            if (!isFileTransferSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("The max number of file transfer sessions is achieved: reject the invitation");
                }

                handleFileTransferInvitationRejected(invite,
                        FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS);

                // Send a 603 Decline response
                sendErrorResponse(invite, Response.DECLINE);
                return;
            }

            FileSharingSession session = new TerminatingMsrpFileSharingSession(this, invite,
                    mRcsSettings);

            getImsModule()
                    .getCore()
                    .getListener()
                    .handleFileTransferInvitation(session, false, remote,
                            session.getRemoteDisplayName(),
                            FileTransferLog.NOT_APPLICABLE_EXPIRATION);

            session.startSession();

        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.warn("Cannot parse contact from FT invitation");
            }
        }
    }

    /**
     * Initiate a one-to-one chat session
     * 
     * @param contact Remote contact identifier
     * @param firstMsg First message
     * @return IM session
     * @throws CoreException
     */
    public OneToOneChatSession initiateOneToOneChatSession(ContactId contact, ChatMessage firstMsg)
            throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info(new StringBuilder("Initiate 1-1 chat session with ").append(contact)
                    .append(".").toString());
        }
        assertAvailableChatSession("Max chat sessions achieved");

        OriginatingOneToOneChatSession session = new OriginatingOneToOneChatSession(this, contact,
                firstMsg, mRcsSettings, mMessagingLog);
        return session;
    }

    /**
     * Receive a one-to-one chat session invitation
     * 
     * @param invite Initial invite
     */
    public void receiveOne2OneChatSession(SipRequest invite) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a 1-1 chat session invitation");
        }
        try {
            ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
            ChatMessage firstMsg = ChatUtils.getFirstMessage(invite);

            // Test if the contact is blocked
            if (mContactsManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked: automatically reject the chat invitation");
                }

                // Save the message in the spam folder
                if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                    mMessagingLog.addOneToOneSpamMessage(firstMsg);
                }

                // Send message delivery report if requested
                if (ChatUtils.isImdnDeliveredRequested(invite)) {
                    // Check notification disposition
                    String msgId = ChatUtils.getMessageId(invite);
                    if (msgId != null) {
                        String remoteInstanceId = null;
                        ContactHeader inviteContactHeader = (ContactHeader) invite
                                .getHeader(ContactHeader.NAME);
                        if (inviteContactHeader != null) {
                            remoteInstanceId = inviteContactHeader
                                    .getParameter(SipUtils.SIP_INSTANCE_PARAM);
                        }
                        // Send message delivery status via a SIP MESSAGE
                        getImdnManager().sendMessageDeliveryStatusImmediately(remote, msgId,
                                ImdnDocument.DELIVERY_STATUS_DELIVERED, remoteInstanceId);
                    }
                }

                // Send a 486 Busy response
                sendErrorResponse(invite, Response.BUSY_HERE);
                return;
            }

            /*
             * Save the message if it was not already persisted in the DB. We don't have to reject
             * the session if the message was a duplicate one as the session rejection/keeping will
             * be handled in TerminatingOneToOneChatSession.startSession() in an uniform way as
             * according to the defined race conditions in the specification document.
             */
            if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
                mMessagingLog.addIncomingOneToOneChatMessage(firstMsg,
                        ChatUtils.isImdnDisplayedRequested(invite));
            }

            // Test number of sessions
            if (!isChatSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("The max number of chat sessions is achieved: reject the invitation");
                }

                // Send a 486 Busy response
                sendErrorResponse(invite, Response.BUSY_HERE);
                return;
            }

            TerminatingOneToOneChatSession session = new TerminatingOneToOneChatSession(this,
                    invite, remote, mRcsSettings, mMessagingLog);

            getImsModule().getCore().getListener().handleOneOneChatSessionInvitation(session);

            session.startSession();

        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot parse remote contact");
            }
        }
    }

    /**
     * Initiate an ad-hoc group chat session
     * 
     * @param contacts List of contact identifiers
     * @param subject Subject
     * @return GroupChatSession
     * @throws CoreException
     */
    public GroupChatSession initiateAdhocGroupChatSession(Set<ContactId> contacts, String subject)
            throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Initiate an ad-hoc group chat session");
        }

        assertAvailableChatSession("Max number of chat sessions reached");

        Map<ContactId, ParticipantStatus> participants = ChatUtils.getParticipants(contacts,
                ParticipantStatus.INVITING);

        OriginatingAdhocGroupChatSession session = new OriginatingAdhocGroupChatSession(this,
                ImsModule.IMS_USER_PROFILE.getImConferenceUri(), subject, participants,
                mRcsSettings, mMessagingLog);

        return session;
    }

    /**
     * Receive ad-hoc group chat session invitation
     * 
     * @param invite Initial invite
     */
    public void receiveAdhocGroupChatSession(SipRequest invite) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive an ad-hoc group chat session invitation");
        }
        ContactId contact = null;
        String remoteUri = null;

        try {
            contact = ChatUtils.getReferredIdentityAsContactId(invite);
            // Test if the contact is blocked
            if (mContactsManager.isBlockedForContact(contact)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + contact
                            + " is blocked: automatically reject the chat invitation");
                }

                handleGroupChatInvitationRejected(invite, GroupChat.ReasonCode.REJECTED_SPAM);

                // Send a 486 Busy response
                sendErrorResponse(invite, Response.BUSY_HERE);
                return;
            }
        } catch (RcsContactFormatException e) {
            // GC invitation is out of the blue (i.e. Store & Forward)
            remoteUri = ChatUtils.getReferredIdentityAsContactUri(invite);
            if (sLogger.isActivated()) {
                sLogger.info("Receive a forward GC invitation from " + remoteUri);
            }
        }

        // Test number of sessions
        if (!isChatSessionAvailable()) {
            if (sLogger.isActivated()) {
                sLogger.debug("The max number of chat sessions is achieved: reject the invitation");
            }

            handleGroupChatInvitationRejected(invite, GroupChat.ReasonCode.REJECTED_MAX_CHATS);

            // Send a 486 Busy response
            sendErrorResponse(invite, Response.BUSY_HERE);
            return;
        }

        /*
         * Get the list of participants from the invite, give them the initial status INVITED as the
         * actual status was not included in the invite.
         */
        Map<ContactId, ParticipantStatus> inviteParticipants = ChatUtils.getParticipants(invite,
                ParticipantStatus.INVITED);

        TerminatingAdhocGroupChatSession session = new TerminatingAdhocGroupChatSession(this,
                invite, contact, inviteParticipants, remoteUri, mRcsSettings, mMessagingLog);

        /*--
         * 6.3.3.1 Leaving a Group Chat that is idle
         * In case the user expresses their desire to leave the Group Chat while it is inactive, the device will not offer the user
         * the possibility any more to enter new messages and restart the chat and automatically decline the first incoming INVITE
         * request for the chat with a SIP 603 DECLINE response. Subsequent INVITE requests should not be rejected as they may be
         * received when the user is added again to the Chat by one of the participants.
         */
        boolean reject = mMessagingLog.isGroupChatNextInviteRejected(session.getContributionID());
        if (reject) {
            if (sLogger.isActivated()) {
                sLogger.debug("Chat Id " + session.getContributionID()
                        + " is declined since previously terminated by user while disconnected");
            }
            // Send a 603 Decline response
            sendErrorResponse(invite, Response.DECLINE);
            mMessagingLog.acceptGroupChatNextInvitation(session.getContributionID());
            return;
        }

        getImsModule().getCore().getListener().handleAdhocGroupChatSessionInvitation(session);

        session.startSession();
    }

    /**
     * Rejoin a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     * @throws CoreException
     */
    public ChatSession rejoinGroupChatSession(String chatId) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Rejoin group chat session");
        }

        assertAvailableChatSession("Max chat sessions reached");

        // Get the group chat info from database
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: conversation not found");
            }
            throw new CoreException("Group chat conversation not found in database");
        }
        if (groupChat.getRejoinId() == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be rejoined: rejoin ID not found");
            }
            throw new CoreException("Rejoin ID not found in database");
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Rejoin group chat: " + groupChat.toString());
        }

        return new RejoinGroupChatSession(this, groupChat, mRcsSettings, mMessagingLog);
    }

    /**
     * Restart a group chat session
     * 
     * @param chatId Chat ID
     * @return IM session
     * @throws CoreException
     */
    public ChatSession restartGroupChatSession(String chatId) throws CoreException {
        if (sLogger.isActivated()) {
            sLogger.info("Restart group chat session");
        }

        assertAvailableChatSession("Max chat sessions reached");

        // Get the group chat info from database
        GroupChatInfo groupChat = mMessagingLog.getGroupChatInfo(chatId);
        if (groupChat == null) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: conversation not found");
            }
            throw new CoreException("Group chat conversation not found in database");
        }

        if (sLogger.isActivated()) {
            sLogger.debug("Restart group chat: " + groupChat.toString());
        }

        Map<ContactId, ParticipantStatus> storedParticipants = mMessagingLog
                .getParticipants(chatId);

        if (storedParticipants.isEmpty()) {
            if (sLogger.isActivated()) {
                sLogger.warn("Group chat " + chatId + " can't be restarted: participants not found");
            }
            throw new CoreException("No connected group chat participants found in database");
        }

        return new RestartGroupChatSession(this, ImsModule.IMS_USER_PROFILE.getImConferenceUri(),
                groupChat.getSubject(), chatId, storedParticipants, mRcsSettings, mMessagingLog);
    }

    /**
     * Receive a conference notification
     * 
     * @param notify Received notify
     */
    public void receiveConferenceNotification(SipRequest notify) {
        GroupChatSession session = getGroupChatSessionOfConferenceSubscriber(notify.getCallId());
        if (session != null) {
            session.getConferenceEventSubscriber().receiveNotification(notify);
        }
    }

    /**
     * Receive a message delivery status
     * 
     * @param message Received message
     */
    public void receiveMessageDeliveryStatus(SipRequest message) {
        // Send a 200 OK response
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Send 200 OK");
            }
            SipResponse response = SipMessageFactory.createResponse(message,
                    IdGenerator.getIdentifier(), 200);
            getImsModule().getSipManager().sendSipResponse(response);
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.error("Can't send 200 OK response", e);
            }
            return;
        }

        try {
            // Parse received message
            ImdnDocument imdn = ChatUtils.parseCpimDeliveryReport(message.getContent());
            if (imdn == null) {
                return;
            }

            ContactId contact = ContactUtils.createContactId(SipUtils.getAssertedIdentity(message));
            String msgId = imdn.getMsgId();
            // Note: FileTransferId is always generated to equal the
            // associated msgId of a FileTransfer invitation message.
            String fileTransferId = msgId;

            // Check if message delivery of a file transfer
            boolean isFileTransfer = mMessagingLog.isFileTransfer(fileTransferId);
            if (isFileTransfer) {
                // Notify the file delivery outside of the chat session
                receiveFileDeliveryStatus(contact, imdn);
            } else {
                // Get session associated to the contact
                OneToOneChatSession session = getOneToOneChatSession(contact);
                if (session != null) {
                    // Notify the message delivery from the chat session
                    session.handleMessageDeliveryStatus(contact, imdn);
                } else {
                    // Notify the message delivery outside of the chat session
                    getImsModule().getCore().getListener()
                            .handleMessageDeliveryStatus(contact, imdn);
                }
            }
        } catch (Exception e) {
            if (sLogger.isActivated()) {
                sLogger.warn("Cannot parse message delivery status");
            }
        }
    }

    /**
     * Receive 1-1 file delivery status
     * 
     * @param contact Contact identifier
     * @param imdn Imdn document
     */
    public void receiveFileDeliveryStatus(ContactId contact, ImdnDocument imdn) {
        // Notify the file delivery outside of the chat session
        getImsModule().getCore().getListener().handleFileDeliveryStatus(contact, imdn);
    }

    /**
     * Receive group file delivery status
     * 
     * @param chatId Chat Id
     * @param contact Contact identifier
     * @param imdn IM delivery notification document
     */
    public void receiveGroupFileDeliveryStatus(String chatId, ContactId contact, ImdnDocument imdn) {
        getImsModule().getCore().getListener().handleGroupFileDeliveryStatus(chatId, contact, imdn);
    }

    /**
     * Receive S&F push messages
     * 
     * @param invite Received invite
     */
    public void receiveStoredAndForwardPushMessages(SipRequest invite) {
        if (sLogger.isActivated()) {
            sLogger.debug("Receive S&F push messages invitation");
        }
        ContactId remote;
        try {
            remote = ChatUtils.getReferredIdentityAsContactId(invite);
        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot parse remote contact");
            }
            return;
        }
        ChatMessage firstMsg = ChatUtils.getFirstMessage(invite);

        // Test if the contact is blocked
        if (mContactsManager.isBlockedForContact(remote)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the S&F invitation");
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, 486);
            return;
        }

        /*
         * Save the message if it was not already persisted in the DB. We don't have to reject the
         * session if the message was a duplicate one as the session rejection/keeping will be
         * handled in TerminatingOneToOneChatSession.startSession() in an uniform way as according
         * to the defined race conditions in the specification document.
         */
        if (firstMsg != null && !mMessagingLog.isMessagePersisted(firstMsg.getMessageId())) {
            mMessagingLog.addIncomingOneToOneChatMessage(firstMsg,
                    ChatUtils.isImdnDisplayedRequested(invite));
        }

        getStoreAndForwardManager().receiveStoredMessages(invite, remote, mRcsSettings,
                mMessagingLog);
    }

    /**
     * Receive S&F push notifications
     * 
     * @param invite Received invite
     */
    public void receiveStoredAndForwardPushNotifications(SipRequest invite) {
        if (sLogger.isActivated()) {
            sLogger.debug("Receive S&F push notifications invitation");
        }
        ContactId remote;
        try {
            remote = ChatUtils.getReferredIdentityAsContactId(invite);
        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.error("Cannot parse remote contact");
            }
            return;
        }
        // Test if the contact is blocked
        if (mContactsManager.isBlockedForContact(remote)) {
            if (sLogger.isActivated()) {
                sLogger.debug("Contact " + remote
                        + " is blocked: automatically reject the S&F invitation");
            }

            // Send a 486 Busy response
            sendErrorResponse(invite, 486);
            return;
        }

        getStoreAndForwardManager().receiveStoredNotifications(invite, remote, mRcsSettings,
                mMessagingLog);
    }

    /**
     * Receive HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
    public void receiveOneToOneHttpFileTranferInvitation(SipRequest invite,
            FileTransferHttpInfoDocument ftinfo) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a single HTTP file transfer invitation");
        }

        try {
            ContactId remote = ChatUtils.getReferredIdentityAsContactId(invite);
            // Test if the contact is blocked
            if (mContactsManager.isBlockedForContact(remote)) {
                if (sLogger.isActivated()) {
                    sLogger.debug("Contact " + remote
                            + " is blocked, automatically reject the HTTP File transfer");
                }

                handleFileTransferInvitationRejected(invite, FileTransfer.ReasonCode.REJECTED_SPAM);
                // Send a 603 Decline response
                sendErrorResponse(invite, Response.DECLINE);
                return;
            }

            // Test number of sessions
            if (!isFileTransferSessionAvailable()) {
                if (sLogger.isActivated()) {
                    sLogger.debug("The max number of FT sessions is achieved, reject the HTTP File transfer");
                }

                handleFileTransferInvitationRejected(invite,
                        FileTransfer.ReasonCode.REJECTED_MAX_FILE_TRANSFERS);
                // Send a 603 Decline response
                sendErrorResponse(invite, 603);
                return;
            }

            // Reject if file is too big or size exceeds device storage capacity. This control
            // should be done
            // on UI. It is done after end user accepts invitation to enable prior handling by the
            // application.
            FileSharingError error = FileSharingSession.isFileCapacityAcceptable(
                    ftinfo.getFileSize(), mRcsSettings);
            if (error != null) {
                // Send a 603 Decline response
                sendErrorResponse(invite, 603);
                int errorCode = error.getErrorCode();
                switch (errorCode) {
                    case FileSharingError.MEDIA_SIZE_TOO_BIG:
                        handleFileTransferInvitationRejected(invite,
                                FileTransfer.ReasonCode.REJECTED_MAX_SIZE);
                    case FileSharingError.NOT_ENOUGH_STORAGE_SPACE:
                        handleFileTransferInvitationRejected(invite,
                                FileTransfer.ReasonCode.REJECTED_LOW_SPACE);
                    default:
                        if (sLogger.isActivated()) {
                            sLogger.error("Encountered unexpected error while receiving HTTP file transfer invitation"
                                    + errorCode);
                        }
                }
                return;
            }

            // Create and start a chat session
            TerminatingOneToOneChatSession oneToOneChatSession = new TerminatingOneToOneChatSession(
                    this, invite, remote, mRcsSettings, mMessagingLog);
            oneToOneChatSession.startSession();

            // Create and start a new HTTP file transfer session
            FileSharingSession fileSharingSession = new DownloadFromInviteFileSharingSession(this,
                    oneToOneChatSession, ftinfo, ChatUtils.getMessageId(invite),
                    oneToOneChatSession.getRemoteContact(),
                    oneToOneChatSession.getRemoteDisplayName(), mRcsSettings, mMessagingLog);

            getImsModule()
                    .getCore()
                    .getListener()
                    .handleOneToOneFileTransferInvitation(fileSharingSession, oneToOneChatSession,
                            ftinfo.getTransferValidity());

            fileSharingSession.startSession();

        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.error("receiveHttpFileTranferInvitation: cannot parse remote contact");
            }
        }
    }

    /**
     * Receive S&F HTTP file transfer invitation
     * 
     * @param invite Received invite
     * @param ftinfo File transfer info document
     */
    public void receiveStoredAndForwardOneToOneHttpFileTranferInvitation(SipRequest invite,
            FileTransferHttpInfoDocument ftinfo) {
        if (sLogger.isActivated()) {
            sLogger.info("Receive a single S&F HTTP file transfer invitation");
        }
        ContactId remote;
        try {
            remote = ChatUtils.getReferredIdentityAsContactId(invite);
        } catch (RcsContactFormatException e) {
            if (sLogger.isActivated()) {
                sLogger.error("receiveStoredAndForwardHttpFileTranferInvitation: cannot parse remote contact");
            }
            return;
        }

        // Create and start a chat session
        TerminatingStoreAndForwardOneToOneChatMessageSession one2oneChatSession = new TerminatingStoreAndForwardOneToOneChatMessageSession(
                this, invite, remote, mRcsSettings, mMessagingLog);
        one2oneChatSession.startSession();

        // Auto reject if file too big
        if (isFileSizeExceeded(ftinfo.getFileSize())) {
            if (sLogger.isActivated()) {
                sLogger.debug("File is too big, reject file transfer invitation");
            }

            // Send a 403 Decline response
            // TODO add warning header "xxx Size exceeded"
            one2oneChatSession.sendErrorResponse(invite, one2oneChatSession.getDialogPath()
                    .getLocalTag(), 403);

            // Close session
            one2oneChatSession
                    .handleError(new FileSharingError(FileSharingError.MEDIA_SIZE_TOO_BIG));
            return;
        }

        // Create and start a new HTTP file transfer session
        FileSharingSession filetransferSession = new DownloadFromInviteFileSharingSession(this,
                one2oneChatSession, ftinfo, ChatUtils.getMessageId(invite),
                one2oneChatSession.getRemoteContact(), one2oneChatSession.getRemoteDisplayName(),
                mRcsSettings, mMessagingLog);

        getImsModule()
                .getCore()
                .getListener()
                .handleOneToOneFileTransferInvitation(filetransferSession, one2oneChatSession,
                        ftinfo.getTransferValidity());

        filetransferSession.startSession();
    }

    /**
     * Check whether file size exceeds the limit
     * 
     * @param size of file
     * @return {@code true} if file size limit is exceeded, otherwise {@code false}
     */
    private boolean isFileSizeExceeded(long size) {
        // Auto reject if file too big
        long maxSize = mRcsSettings.getMaxFileTransferSize();
        if (maxSize > 0 && size > maxSize) {
            return true;
        }

        return false;
    }

    /**
     * Check if the capabilities are valid based on msgCapValidity paramter
     * 
     * @param capabilities
     * @return {@code true} if valid, otherwise {@code false}
     */
    public boolean isCapabilitiesValid(Capabilities capabilities) {
        long msgCapValidityPeriod = mRcsSettings.getMsgCapValidityPeriod();
        if (System.currentTimeMillis() > capabilities.getTimestampOfLastRefresh()
                + msgCapValidityPeriod) {
            return false;
        }
        return true;
    }
}
