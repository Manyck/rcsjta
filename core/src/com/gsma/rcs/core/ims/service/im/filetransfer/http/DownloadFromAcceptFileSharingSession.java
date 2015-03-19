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

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Terminating file transfer HTTP session starting from user acceptance (after core was restarted).
 */
public class DownloadFromAcceptFileSharingSession extends TerminatingHttpFileSharingSession {

    private final static Logger LOGGER = Logger
            .getLogger(DownloadFromAcceptFileSharingSession.class.getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param content the content to be transferred
     * @param resume the Data Object to access FT HTTP table in DB
     * @param rcsSettings
     * @param messagingLog
     */
    public DownloadFromAcceptFileSharingSession(ImsService parent, MmContent content,
            FtHttpResumeDownload resume, RcsSettings rcsSettings, MessagingLog messagingLog) {

        // @formatter:off
        super(parent,
                content,
                resume.getFileExpiration(),
                resume.getFileicon() != null ? FileTransferUtils.createMmContent(resume.getFileicon()) : null,
                resume.getFileicon() != null ? resume.getIconExpiration() : FileTransferData.NOT_APPLICABLE_EXPIRATION,
                resume.getContact(),
                null,
                resume.getChatId(),
                resume.getFileTransferId(),
                resume.isGroupTransfer(),
                resume.getServerAddress(),
                rcsSettings,
                messagingLog,
                resume.getRemoteSipInstance());
        // @formatter:on
        setSessionAccepted();
    }

    /**
     * Background processing
     */
    public void run() {
        boolean logActivated = LOGGER.isActivated();
        if (logActivated) {
            LOGGER.info("Accept HTTP file transfer session");
        }
        try {
            httpTransferStarted();
        } catch (Exception e) {
            if (logActivated) {
                LOGGER.error("Transfer has failed", e);
            }
            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
            return;

        }
        super.run();
    }

}
