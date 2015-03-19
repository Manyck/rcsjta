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

package com.gsma.rcs.provider.messaging;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.provider.fthttp.FtHttpResume;
import com.gsma.rcs.provider.fthttp.FtHttpResumeUpload;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.filetransfer.FileTransfer.ReasonCode;
import com.gsma.services.rcs.filetransfer.FileTransfer.State;

import android.database.Cursor;
import android.net.Uri;

import java.util.List;

/**
 * Interface for the ft table
 * 
 * @author LEMORDANT Philippe
 */
public interface IFileTransferLog {

    /**
     * Add outgoing file transfer
     * 
     * @param fileTransferId File Transfer ID
     * @param contact Contact ID
     * @param direction Direction
     * @param content File content
     * @param fileIcon File icon content
     * @param state File transfer state
     * @param reasonCode Reason code
     * @param fileExpiration the time when file on the content server is no longer valid to
     *            download.
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download.
     */
    public void addFileTransfer(String fileTransferId, ContactId contact, Direction direction,
            MmContent content, MmContent fileIcon, State state, ReasonCode reasonCode,
            long fileExpiration, long fileIconExpiration);

    /**
     * Add an outgoing File Transfer supported by Group Chat
     * 
     * @param fileTransferId the identity of the file transfer
     * @param chatId the identity of the group chat
     * @param content the File content
     * @param fileIcon the fileIcon content
     * @param state File transfer state
     * @param reasonCode Reason code
     */
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent fileIcon, State state, ReasonCode reasonCode);

    /**
     * Add incoming group file transfer
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param content File content
     * @param fileIcon File icon contentID
     * @param state File transfer state
     * @param reasonCode Reason code
     * @param fileExpiration the time when file on the content server is no longer valid to
     *            download.
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download.
     */
    public void addIncomingGroupFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileIcon, State state,
            ReasonCode reasonCode, long fileExpiration, long fileIconExpiration);

    /**
     * Set file transfer state and reason code
     * 
     * @param fileTransferId File transfer ID
     * @param state File transfer state
     * @param reasonCode File transfer state reason code
     */
    public void setFileTransferStateAndReasonCode(String fileTransferId, State state,
            ReasonCode reasonCode);

    /**
     * Update file transfer read status
     * 
     * @param fileTransferId File transfer ID
     */
    public void markFileTransferAsRead(String fileTransferId);

    /**
     * Update file transfer download progress
     * 
     * @param fileTransferId File transfer ID
     * @param currentSize Current size
     */
    public void setFileTransferProgress(String fileTransferId, long currentSize);

    /**
     * Set file transfer URI
     * 
     * @param fileTransferId File transfer ID
     * @param content the MmContent of received file
     * @param fileExpiration the time when file on the content server is no longer valid to download
     * @param fileIconExpiration the time when file icon on the content server is no longer valid to
     *            download
     */
    public void setFileTransferred(String fileTransferId, MmContent content, long fileExpiration,
            long fileIconExpiration);

    /**
     * Tells if the MessageID corresponds to that of a file transfer
     * 
     * @param fileTransferId File Transfer Id
     * @return boolean If there is File Transfer corresponding to msgId
     */
    public boolean isFileTransfer(String fileTransferId);

    /**
     * Set file upload TID
     * 
     * @param fileTransferId File transfer ID
     * @param tId TID
     */
    public void setFileUploadTId(String fileTransferId, String tId);

    /**
     * Set file download server uri
     * 
     * @param fileTransferId File transfer ID
     * @param downloadAddress Download Address
     */
    public void setFileDownloadAddress(String fileTransferId, Uri downloadAddress);

    /**
     * Retrieve file transfers paused by SYSTEM on connection loss
     * 
     * @return list of FtHttpResume
     */
    public List<FtHttpResume> retrieveFileTransfersPausedBySystem();

    /**
     * Retrieve resumable file upload
     * 
     * @param tId Unique Id used while uploading
     * @return instance of FtHttpResumeUpload
     */
    public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId);

    /**
     * Get file transfer state from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return State
     */
    public State getFileTransferState(String fileTransferId);

    /**
     * Get file transfer reason code from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return reason code of the state
     */
    public ReasonCode getFileTransferStateReasonCode(String fileTransferId);

    /**
     * Get cacheable file transfer data from its unique ID
     * 
     * @param fileTransferId
     * @return Cursor
     */
    public Cursor getCacheableFileTransferData(String fileTransferId);

    /**
     * Is group file transfer
     * 
     * @param fileTransferId
     * @return true if it is group file transfer
     */
    public boolean isGroupFileTransfer(String fileTransferId);

    /**
     * Get file transfer time stamp from file transfer Id
     * 
     * @param fileTransferId
     * @return time stamp
     */
    public long getFileTransferTimestamp(String fileTransferId);

    /**
     * Get file transfer sent time stamp from file transfer Id
     * 
     * @param fileTransferId
     * @return sent time stamp
     */
    public long getFileTransferSentTimestamp(String fileTransferId);

    /**
     * Get file transfer resume info from its corresponding filetransferId
     * 
     * @param fileTransferId
     * @return FtHttpResume
     */
    public FtHttpResume getFileTransferResumeInfo(String fileTransferId);

    /**
     * Get all one-to-one and group file transfers that are in queued state in ascending order of
     * timestamp
     * 
     * @return Cursor
     */
    public Cursor getQueuedFileTransfers();

    /**
     * Dequeue file transfer
     * 
     * @param fileTransferId
     * @param timestamp
     * @param timestampSent
     */
    public void dequeueFileTransfer(String fileTransferId, long timestamp, long timestampSent);

    /**
     * Get queued group file transfers
     * 
     * @param chatId
     * @return Cursor
     */
    public Cursor getQueuedGroupFileTransfers(String chatId);

    /**
     * Get queued one-one file transfers
     * 
     * @param contact
     * @return Cursor
     */
    public Cursor getQueuedOneToOneFileTransfers(ContactId contact);

    /**
     * Get file transfer Upload Tid
     * 
     * @param fileTransferId
     * @return Tid
     */
    public String getFileTransferUploadTid(String fileTransferId);

    /**
     * Update file transfer state for interrupted file transfers
     * 
     * @return TODO
     */
    public Cursor getInterruptedFileTransfers();

    /**
     * Sets remote SIP Instance identifier for download HTTP file transfer
     * 
     * @param fileTransferId
     * @param remoteInstanceId
     */
    public void setRemoteSipId(String fileTransferId, String remoteInstanceId);
}
