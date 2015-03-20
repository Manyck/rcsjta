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
import com.gsma.services.rcs.contacts.ContactId;

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
     * @param fileIcon Fileicon content
     * @param state File transfer state
     * @param reasonCode Reason code
     */
    public void addFileTransfer(String fileTransferId, ContactId contact, int direction,
            MmContent content, MmContent fileIcon, int state, int reasonCode);

    /**
     * Add an outgoing File Transfer supported by Group Chat
     * 
     * @param fileTransferId the identity of the file transfer
     * @param chatId the identity of the group chat
     * @param content the File content
     * @param Fileicon the fileIcon content
     * @param state File transfer state
     * @param reasonCode Reason code
     */
    public void addOutgoingGroupFileTransfer(String fileTransferId, String chatId,
            MmContent content, MmContent fileIcon, int state, int reasonCode);

    /**
     * Add incoming group file transfer
     * 
     * @param fileTransferId File transfer ID
     * @param chatId Chat ID
     * @param contact Contact ID
     * @param content File content
     * @param fileIcon Fileicon contentID
     * @param state File transfer state
     * @param reasonCode Reason code
     */
    public void addIncomingGroupFileTransfer(String fileTransferId, String chatId,
            ContactId contact, MmContent content, MmContent fileIcon, int state, int reasonCode);

    /**
     * Set file transfer state and reason code
     * 
     * @param fileTransferId File transfer ID
     * @param state File transfer state
     * @param reasonCode File transfer state reason code
     */
    public void setFileTransferStateAndReasonCode(String fileTransferId, int state, int reasonCode);

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
     */
    public void setFileTransferred(String fileTransferId, MmContent content);

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
     */
    public List<FtHttpResume> retrieveFileTransfersPausedBySystem();

    /**
     * Retrieve resumable file upload
     *
     * @param tId Unique Id used while uploading
     */
    public FtHttpResumeUpload retrieveFtHttpResumeUpload(String tId);

    /**
     * Get file transfer chatId from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return chatId
     */
    public String getFileTransferChatId(String fileTransferId);

    /**
     * Get file transfer remote contact from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return contact
     */
    public ContactId getFileTransferRemoteContact(String fileTransferId);

    /**
     * Get file from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return file
     */
    public Uri getFile(String fileTransferId);

    /**
     * Get file name from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return name
     */
    public String getFileName(String fileTransferId);

    /**
     * Get file transfer direction from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return Direction
     */
    public String getFileMimeType(String fileTransferId);

    /**
     * Get file icon from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return fileicon
     */
    public Uri getFileIcon(String fileTransferId);

    /**
     * Get file transfer direction from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return Direction
     */
    public int getFileTransferDirection(String fileTransferId);

    /**
     * Get file transfer state from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return State
     */
    public int getFileTransferState(String fileTransferId);

    /**
     * Get file transfer reason code from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return reason code of the state
     */
    public int getFileTransferStateReasonCode(String fileTransferId);

    /**
     * Get file size from its unique ID
     * 
     * @param fileTransferId Unique ID of file transfer
     * @return size of the file
     */
    public long getFileSize(String fileTransferId);

    /**
     * Is group file transfer
     * 
     * @param fileTransferId
     * @return true if it is group file transfer
     */
    public boolean isGroupFileTransfer(String fileTransferId);
}
