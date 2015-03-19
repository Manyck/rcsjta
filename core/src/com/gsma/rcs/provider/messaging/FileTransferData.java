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

import android.net.Uri;

import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.filetransfer.FileTransferLog;

/**
 * File transfer data constants
 * 
 * @author Jean-Marc AUFFRET
 */
public class FileTransferData {

    /**
     * Database URI
     */
    public static final Uri CONTENT_URI = Uri
            .parse("content://com.gsma.rcs.filetransfer/filetransfer");

    /**
     * History log member id
     */
    public static final int HISTORYLOG_MEMBER_ID = 2;

    /**
     * Unique history log id
     */
    /* package private */static final String KEY_BASECOLUMN_ID = FileTransferLog.BASECOLUMN_ID;

    /**
     * Unique file transfer identifier
     */
    public static final String KEY_FT_ID = FileTransferLog.FT_ID;

    /**
     * Id of chat
     */
    public static final String KEY_CHAT_ID = FileTransferLog.CHAT_ID;

    /**
     * Date of the transfer
     */
    /* package private */static final String KEY_TIMESTAMP = FileTransferLog.TIMESTAMP;

    /**
     * Time when file is sent. If 0 means not sent.
     */
    /* package private */static final String KEY_TIMESTAMP_SENT = FileTransferLog.TIMESTAMP_SENT;

    /**
     * Time when file is delivered. If 0 means not delivered.
     */
    /* package private */static final String KEY_TIMESTAMP_DELIVERED = FileTransferLog.TIMESTAMP_DELIVERED;

    /**
     * Time when file is displayed.
     */
    /* package private */static final String KEY_TIMESTAMP_DISPLAYED = FileTransferLog.TIMESTAMP_DISPLAYED;

    /**
     * ContactId formatted number of remote contact or null if the filetransfer is an outgoing group
     * file transfer.
     */
    public static final String KEY_CONTACT = FileTransferLog.CONTACT;

    /**
     * @see FileTransfer.State for possible states.
     */
    /* package private */static final String KEY_STATE = FileTransferLog.STATE;

    /**
     * Reason code associated with the file transfer state.
     * 
     * @see FileTransfer.ReasonCode for possible reason codes.
     */
    /* package private */static final String KEY_REASON_CODE = FileTransferLog.REASON_CODE;

    /**
     * @see ReadStatus
     */
    /* package private */static final String KEY_READ_STATUS = FileTransferLog.READ_STATUS;

    /**
     * Multipurpose Internet Mail Extensions (MIME) type of message
     */
    /* package private */static final String KEY_MIME_TYPE = FileTransferLog.MIME_TYPE;

    /**
     * URI of the file
     */
    /* package private */static final String KEY_FILE = FileTransferLog.FILE;

    /**
     * Filename
     */
    /* package private */static final String KEY_FILENAME = FileTransferLog.FILENAME;

    /**
     * Size transferred in bytes
     */
    /* package private */static final String KEY_TRANSFERRED = FileTransferLog.TRANSFERRED;

    /**
     * File size in bytes
     */
    /* package private */static final String KEY_FILESIZE = FileTransferLog.FILESIZE;

    /**
     * Incoming transfer or outgoing transfer
     * 
     * @see Direction
     */
    public static final String KEY_DIRECTION = FileTransferLog.DIRECTION;

    /**
     * Column name KEY_FILEICON : the URI of the file icon
     */
    /* package private */static final String KEY_FILEICON = FileTransferLog.FILEICON;

    /**
     * URI of the file icon
     */
    /* package private */static final String KEY_FILEICON_MIME_TYPE = FileTransferLog.FILEICON_MIME_TYPE;

    /**
     * The upload transaction ID (hidden field from client applications)
     */
    public static final String KEY_UPLOAD_TID = "upload_tid";

    /**
     * The download server address (hidden field from client applications)
     */
    public static final String KEY_DOWNLOAD_URI = "download_uri";

    /**
     * The time for when file on the content server is no longer valid to download.
     */
    /* package private */static final String KEY_FILE_EXPIRATION = FileTransferLog.FILE_EXPIRATION;

    /**
     * The time for when file icon on the content server is no longer valid to download.
     */
    /* package private */static final String KEY_FILEICON_EXPIRATION = FileTransferLog.FILEICON_EXPIRATION;

    /**
     * @see FileTransferLog#NOT_APPLICABLE_EXPIRATION
     */
    public static final long NOT_APPLICABLE_EXPIRATION = FileTransferLog.NOT_APPLICABLE_EXPIRATION;

    /**
     * @see FileTransferLog#UNKNOWN_EXPIRATION
     */
    public static final long UNKNOWN_EXPIRATION = FileTransferLog.UNKNOWN_EXPIRATION;

    /**
     * The remote SIP instance ID to fill the accept contact header of the SIP delivery
     * notification.<br>
     * Only application for incoming HTTP file transfers.
     */
    /* package private */static final String KEY_REMOTE_SIP_ID = "remote_sip_id";

}
