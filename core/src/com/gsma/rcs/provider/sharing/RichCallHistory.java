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

package com.gsma.rcs.provider.sharing;

import java.util.Calendar;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;

import com.gsma.rcs.core.content.MmContent;
import com.gsma.rcs.core.content.VideoContent;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.ContactUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contacts.ContactId;
import com.gsma.services.rcs.sharing.image.ImageSharing;
import com.gsma.services.rcs.sharing.image.ImageSharingLog;
import com.gsma.services.rcs.sharing.video.VideoSharingLog;

/**
 * Rich call history
 * 
 * @author Jean-Marc AUFFRET
 */
public class RichCallHistory {
    /**
     * Current instance
     */
    private static RichCallHistory instance;

    private final LocalContentResolver mLocalContentResolver;

    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(RichCallHistory.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    /**
     * Get image transfer info from its unique Id
     * 
     * @param columnName
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getImageTransferData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for image transfer data with sharingId : "
                            + sharingId);

        } catch (RuntimeException e) {
            if (logger.isActivated()) {
                logger.error("Exception occured while retrieving image share info of sharingId = '"
                        + sharingId + "' ! ", e);
            }
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    /**
     * Get video sharing info from its unique Id
     * 
     * @param columnName
     * @param sharingId
     * @return Cursor the caller of this method has to close the cursor if a cursor is returned
     */
    private Cursor getVideoSharingData(String columnName, String sharingId) {
        String[] projection = new String[] {
            columnName
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(
                    Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (cursor.moveToFirst()) {
                return cursor;
            }
            throw new SQLException(
                    "No row returned while querying for video sharing data with sharingId : "
                            + sharingId);

        } catch (RuntimeException e) {
            if (logger.isActivated()) {
                logger.error("Exception occured while retrieving video share info of sharingId = '"
                        + sharingId + "' ! ", e);
            }
            if (cursor != null) {
                cursor.close();
            }
            throw e;
        }
    }

    private String getDataAsString(Cursor cursor) {
        try {
            return cursor.getString(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private int getDataAsInt(Cursor cursor) {
        try {
            return cursor.getInt(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private long getDataAsLong(Cursor cursor) {
        try {
            return cursor.getLong(FIRST_COLUMN_IDX);

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Create instance
     * 
     * @param localContentResolver Local content resolver
     */
    public static synchronized void createInstance(LocalContentResolver localContentResolver) {
        if (instance == null) {
            instance = new RichCallHistory(localContentResolver);
        }
    }

    /**
     * Returns instance
     * 
     * @return Instance
     */
    public static RichCallHistory getInstance() {
        return instance;
    }

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     */
    private RichCallHistory(LocalContentResolver localContentResolver) {
        super();
        mLocalContentResolver = localContentResolver;
    }

    /*--------------------- Video sharing methods ----------------------*/

    /**
     * Add a new video sharing in the call history
     * 
     * @param sharingId Session ID
     * @param contact Remote contact ID
     * @param direction Call event direction
     * @param content Shared content
     * @param state Call state
     * @param reasonCode Reason Code
     */
    public Uri addVideoSharing(String sharingId, ContactId contact, int direction,
            VideoContent content, int state, int reasonCode) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Add new video sharing for contact ").append(contact)
                    .append(": sharingId=").append(sharingId).append(", state=").append(state)
                    .append(", reasonCode=").append(reasonCode).toString());
        }

        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_SHARING_ID, sharingId);
        values.put(VideoSharingData.KEY_CONTACT, contact.toString());
        values.put(VideoSharingData.KEY_DIRECTION, direction);
        values.put(VideoSharingData.KEY_STATE, state);
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode);
        values.put(VideoSharingData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        values.put(VideoSharingData.KEY_DURATION, 0);
        values.put(VideoSharingData.KEY_VIDEO_ENCODING, content.getEncoding());
        values.put(VideoSharingData.KEY_WIDTH, content.getWidth());
        values.put(VideoSharingData.KEY_HEIGHT, content.getHeight());
        return mLocalContentResolver.insert(VideoSharingLog.CONTENT_URI, values);
    }

    /**
     * Set the video sharing state and reason code
     * 
     * @param sessionId Session ID of the entry
     * @param state New state
     * @param reasonCode Reason Code
     */
    public void setVideoSharingStateAndReasonCode(String sharingId, int state, int reasonCode) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("Update video sharing state of sharing ")
                    .append(sharingId).append(" state=").append(state).append(", reasonCode=")
                    .append(reasonCode).toString());
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_STATE, state);
        values.put(VideoSharingData.KEY_REASON_CODE, reasonCode);
        mLocalContentResolver.update(Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /**
     * Update the video sharing duration at the end of the call
     * 
     * @param sessionId Session ID of the entry
     * @param duration Duration
     */
    public void setVideoSharingDuration(String sharingId, long duration) {
        if (logger.isActivated()) {
            logger.debug("Update duration of sharing " + sharingId + " to " + duration);
        }
        ContentValues values = new ContentValues();
        values.put(VideoSharingData.KEY_DURATION, duration);
        mLocalContentResolver.update(Uri.withAppendedPath(VideoSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /*--------------------- Image sharing methods ----------------------*/

    /**
     * Add a new image sharing in the call history
     * 
     * @param sharingId Session ID
     * @param contact Remote contact ID
     * @param direction Call event direction
     * @param content Shared content
     * @param status Call status
     * @param reasonCode Reason Code
     */
    public Uri addImageSharing(String sharingId, ContactId contact, int direction,
            MmContent content, int status, int reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Add new image sharing for contact " + contact + ": sharing =" + sharingId
                    + ", status=" + status);
        }

        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_SHARING_ID, sharingId);
        values.put(ImageSharingData.KEY_CONTACT, contact.toString());
        values.put(ImageSharingData.KEY_DIRECTION, direction);
        values.put(ImageSharingData.KEY_FILE, content.getUri().toString());
        values.put(ImageSharingData.KEY_FILENAME, content.getName());
        values.put(ImageSharingData.KEY_MIME_TYPE, content.getEncoding());
        values.put(ImageSharingData.KEY_TRANSFERRED, 0);
        values.put(ImageSharingData.KEY_FILESIZE, content.getSize());
        values.put(ImageSharingData.KEY_STATE, status);
        values.put(ImageSharingData.KEY_REASON_CODE, reasonCode);
        values.put(ImageSharingData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        return mLocalContentResolver.insert(ImageSharingLog.CONTENT_URI, values);
    }

    /**
     * Set the image sharing state and reason code
     * 
     * @param sessionId Session ID of the entry
     * @param state New state
     * @param reasonCode Reason Code
     */
    public void setImageSharingStateAndReasonCode(String sharingId, int state, int reasonCode) {
        if (logger.isActivated()) {
            logger.debug("Update status of image sharing " + sharingId + " to " + state);
        }
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_STATE, state);
        values.put(ImageSharingData.KEY_REASON_CODE, reasonCode);
        if (state == ImageSharing.State.TRANSFERRED) {
            // Update the size of bytes if fully transferred
            long total = getImageSharingTotalSize(sharingId);
            if (total != 0) {
                values.put(ImageSharingData.KEY_TRANSFERRED, total);
            }
        }
        mLocalContentResolver.update(Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /**
     * Read the total size of transferred image
     *
     * @param sessionId the session identifier
     * @return the total size (or 0 if failed)
     */
    public long getImageSharingTotalSize(String sharingId) {
        Cursor c = null;
        try {
            String[] projection = new String[] {
                ImageSharingData.KEY_FILESIZE
            };
            c = mLocalContentResolver.query(
                    Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId), projection, null,
                    null, null);
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        } catch (Exception e) {
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return 0L;
    }

    /**
     * Update the image sharing progress
     * 
     * @param sharingId Session ID of the entry
     * @param currentSize Current size
     */
    public void setImageSharingProgress(String sharingId, long currentSize) {
        ContentValues values = new ContentValues();
        values.put(ImageSharingData.KEY_TRANSFERRED, currentSize);
        mLocalContentResolver.update(Uri.withAppendedPath(ImageSharingLog.CONTENT_URI, sharingId),
                values, null, null);
    }

    /**
     * Delete all entries in Rich Call history
     */
    public void deleteAllEntries() {
        mLocalContentResolver.delete(ImageSharingLog.CONTENT_URI, null, null);
        mLocalContentResolver.delete(VideoSharingLog.CONTENT_URI, null, null);
    }

    /**
     * Get remote contact from unique Id
     * 
     * @param sharingId
     * @return ContactId
     */
    public ContactId getImageSharingRemoteContact(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer contact for sharingId '" + sharingId + "'");
        }
        Cursor cursor = getImageTransferData(ImageSharingData.KEY_CONTACT, sharingId);
        return ContactUtils.createContactId(getDataAsString(cursor));
    }

    /**
     * Get Image Uri from unique Id
     * 
     * @param sharingId
     * @return Uri of image
     */
    public Uri getImage(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image uri for sharingId '" + sharingId + "'");
        }
        return Uri
                .parse(getDataAsString(getImageTransferData(ImageSharingData.KEY_FILE, sharingId)));
    }

    /**
     * Get Image sharing name from unique Id
     * 
     * @param sharingId
     * @return name
     */
    public String getImageSharingName(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image sharing name for sharingId '" + sharingId + "'");
        }
        return getDataAsString(getImageTransferData(ImageSharingData.KEY_FILENAME, sharingId));
    }

    /**
     * Get Image sharing size from unique Id
     * 
     * @param sharingId
     * @return Size
     */
    public long getImageSharingSize(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image sharing size for sharingId '" + sharingId + "'");
        }
        return getDataAsLong(getImageTransferData(ImageSharingData.KEY_FILESIZE, sharingId));
    }

    /**
     * Get Image sharing mime type from unique Id
     * 
     * @param sharingId
     * @return Mimetype
     */
    public String getImageSharingMimeType(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image type for sharingId '" + sharingId + "'");
        }
        return getDataAsString(getImageTransferData(ImageSharingData.KEY_MIME_TYPE, sharingId));
    }

    /**
     * Get Image sharing state from unique Id
     * 
     * @param sharingId
     * @return State
     */
    public int getImageSharingState(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer state for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getImageTransferData(ImageSharingData.KEY_STATE, sharingId));
    }

    /**
     * Get Image sharing reason code from unique Id
     * 
     * @param sharingId
     * @return Reason code
     */
    public int getImageSharingReasonCode(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer state readon code for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getImageTransferData(ImageSharingData.KEY_REASON_CODE, sharingId));
    }

    /**
     * Get Image sharing direction from unique Id
     * 
     * @param sharingId
     * @return Direction
     */
    public int getImageSharingDirection(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get image transfer direction for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getImageTransferData(ImageSharingData.KEY_DIRECTION, sharingId));
    }

    /**
     * Get Video sharing remote contact from unique Id
     * 
     * @param sharingId
     * @return ContactId
     */
    public ContactId getVideoSharingRemoteContact(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share contact for sharingId '" + sharingId + "'");
        }
        return ContactUtils.createContactId(getDataAsString(getVideoSharingData(
                VideoSharingData.KEY_CONTACT, sharingId)));
    }

    /**
     * Get Video sharing state from unique Id
     * 
     * @param sharingId
     * @return State
     */
    public int getVideoSharingState(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share state for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getVideoSharingData(VideoSharingData.KEY_STATE, sharingId));
    }

    /**
     * Get Video sharing reason code from unique Id
     * 
     * @param sharingId
     * @return Reason code
     */
    public int getVideoSharingReasonCode(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share state reason code for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getVideoSharingData(VideoSharingData.KEY_REASON_CODE, sharingId));
    }

    /**
     * Get Video sharing direction from unique Id
     * 
     * @param sharingId
     * @return Direction
     */
    public int getVideoSharingDirection(String sharingId) {
        if (logger.isActivated()) {
            logger.debug("Get video share direction for sharingId '" + sharingId + "'");
        }
        return getDataAsInt(getVideoSharingData(VideoSharingData.KEY_DIRECTION, sharingId));
    }
}
