/*******************************************************************************
w * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications AB.
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
 * NOTE: This file has been modified by Sony Mobile Communications AB.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.im.filetransfer.http;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileSharingError;
import com.gsma.rcs.core.ims.service.im.filetransfer.FileTransferUtils;
import com.gsma.rcs.provider.fthttp.FtHttpResumeDownload;
import com.gsma.rcs.provider.messaging.FileTransferData;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;

/**
 * Terminating file transfer HTTP session starting from system resuming (because core was
 * restarted).
 */
public class DownloadFromResumeFileSharingSession extends TerminatingHttpFileSharingSession {

    private final static Logger LOGGER = Logger
            .getLogger(DownloadFromResumeFileSharingSession.class.getSimpleName());

    private final FtHttpResumeDownload mResume;

    /**
     * Constructor create instance of session object to resume download
     * 
     * @param parent IMS service
     * @param content the content (url, mime-type and size)
     * @param resume the data object in DB
     * @param rcsSettings
     * @param messagingLog
     */
    public DownloadFromResumeFileSharingSession(ImsService parent, MmContent content,
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
        mResume = resume;
    }

    /**
     * Background processing
     */
    public void run() {
        boolean logActivated = LOGGER.isActivated();
        if (logActivated) {
            LOGGER.info("Resume a HTTP file transfer session as terminating");
        }
        try {
            httpTransferStarted();

            // Resume download file from the HTTP server
            if (mDownloadManager.mStreamForFile != null && mDownloadManager.resumeDownload()) {
                if (logActivated) {
                    LOGGER.debug("Resume download success for ".concat(mResume.toString()));
                }
                // Set file URL
                getContent().setUri(mDownloadManager.getDownloadedFileUri());

                // File transfered
                handleFileTransfered();
                // Send delivery report "displayed"
                sendDeliveryReport(ImdnDocument.DELIVERY_STATUS_DISPLAYED);
            } else {
                // Don't call handleError in case of Pause or Cancel
                if (mDownloadManager.isCancelled() || mDownloadManager.isPaused()) {
                    return;
                }

                // Upload error
                if (logActivated) {
                    LOGGER.info("Resume Download file has failed");
                }
                handleError(new FileSharingError(FileSharingError.MEDIA_DOWNLOAD_FAILED));
            }
        } catch (Exception e) {
            if (logActivated) {
                LOGGER.error("Transfer has failed", e);
            }
            // Unexpected error
            handleError(new FileSharingError(FileSharingError.UNEXPECTED_EXCEPTION, e.getMessage()));
        }
    }

}
