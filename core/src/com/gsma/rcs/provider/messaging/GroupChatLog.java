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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.SparseArray;

import com.gsma.rcs.core.ims.service.im.chat.GroupChatInfo;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.GroupChat;
import com.gsma.services.rcs.chat.ParticipantInfo;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * This class interfaces the chat table
 */
public class GroupChatLog implements IGroupChatLog {

    private final static int DEPARTED_BY_USER = 1;

    private final static String ORDER_BY_TIMESTAMP_DESC = ChatData.KEY_TIMESTAMP.concat(" DESC");

    private final Context mCtx;

    private final LocalContentResolver mLocalContentResolver;

    private final static String SELECT_CHAT_ID = new StringBuilder(ChatData.KEY_CHAT_ID).append(
            "=?").toString();

    private final static String SELECT_CHAT_ID_STATUS_REJECTED = new StringBuilder(
            ChatData.KEY_CHAT_ID).append("=? AND ").append(ChatData.KEY_STATE).append("=")
            .append(GroupChat.State.ABORTED).append(" AND ").append(ChatData.KEY_REASON_CODE)
            .append("=").append(GroupChat.ReasonCode.ABORTED_BY_USER).append(" AND ")
            .append(ChatData.KEY_USER_ABORTION).append("=")
            .append(UserAbortion.SERVER_NOT_NOTIFIED.toInt()).toString();

    private static final String SELECT_ACTIVE_GROUP_CHATS = new StringBuilder(ChatData.KEY_STATE)
            .append("=").append(GroupChat.State.STARTED).toString();

    /**
     * The logger
     */
    private static final Logger logger = Logger.getLogger(GroupChatLog.class.getSimpleName());

    private static final int FIRST_COLUMN_IDX = 0;

    public static enum UserAbortion {

        SERVER_NOTIFIED(0), SERVER_NOT_NOTIFIED(1);

        private final int mValue;

        private static SparseArray<UserAbortion> mValueToEnum = new SparseArray<UserAbortion>();
        static {
            for (UserAbortion entry : UserAbortion.values()) {
                mValueToEnum.put(entry.toInt(), entry);
            }
        }

        private UserAbortion(int value) {
            mValue = value;
        }

        public final int toInt() {
            return mValue;
        }

        public final static UserAbortion valueOf(int value) {
            UserAbortion entry = mValueToEnum.get(value);
            if (entry != null) {
                return entry;
            }
            throw new IllegalArgumentException("No enum const class "
                    + UserAbortion.class.getName() + "." + value);
        }
    }

    /**
     * Constructor
     * 
     * @param localContentResolver Local content resolver
     */
    /* package private */GroupChatLog(Context ctx, LocalContentResolver localContentResolver) {
        mCtx = ctx;
        mLocalContentResolver = localContentResolver;
    }

    /**
     * Convert a set of ParticipantInfo into a string
     * 
     * @param participants the participant information
     * @return the string with comma separated values of key pairs formatted as follows: "key=value"
     */
    private static String writeParticipantInfo(Set<ParticipantInfo> participants) {
        if (participants == null || participants.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int size = participants.size();
        for (ParticipantInfo participant : participants) {
            // set key
            sb.append(participant.getContact());
            sb.append('=');
            // set value
            sb.append(participant.getStatus());
            if (--size != 0) {
                // Not last item : add separator
                sb.append(',');
            }
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#addGroupChat(java.lang.String,
     * java.lang.String, java.util.Set, int, int)
     */
    public void addGroupChat(String chatId, ContactId contact, String subject,
            Set<ParticipantInfo> participants, int state, int reasonCode, int direction) {
        if (logger.isActivated()) {
            logger.debug(new StringBuilder("addGroupChat; chatID=").append(chatId)
                    .append(",subject=").append(subject).append(",state").append(state)
                    .append("reasonCode=").append(reasonCode).append(",direction=")
                    .append(direction).toString());
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_CHAT_ID, chatId);
        if (contact != null) {
            values.put(ChatData.KEY_CONTACT, contact.toString());
        }
        values.put(ChatData.KEY_STATE, state);
        values.put(ChatData.KEY_REASON_CODE, reasonCode);
        values.put(ChatData.KEY_SUBJECT, subject);
        values.put(ChatData.KEY_PARTICIPANTS, writeParticipantInfo(participants));
        values.put(ChatData.KEY_DIRECTION, direction);
        values.put(ChatData.KEY_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        mLocalContentResolver.insert(ChatData.CONTENT_URI, values);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatLog#acceptGroupChatNextInvitation(java.lang
     * .String)
     */
    @Override
    public void acceptGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("acceptGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOTIFIED.toInt());
        String[] selectionArgs = {
            chatId
        };
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, SELECT_CHAT_ID_STATUS_REJECTED,
                selectionArgs);
        if (logger.isActivated()) {
            logger.debug("acceptGroupChatNextInvitation (chatID=" + chatId + ")");
        }
    }

    @Override
    public void setGroupChatStateAndReasonCode(String chatId, int state, int reasonCode) {
        if (logger.isActivated()) {
            logger.debug("updateGroupChatStatus (chatId=" + chatId + ") (state=" + state
                    + ") (reasonCode=" + reasonCode + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_STATE, state);
        values.put(ChatData.KEY_REASON_CODE, reasonCode);
        String selectionArgs[] = new String[] {
            chatId
        };
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, SELECT_CHAT_ID, selectionArgs);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatParticipant(java.lang.
     * String, java.util.Set)
     */
    @Override
    public void updateGroupChatParticipant(String chatId, Set<ParticipantInfo> participants) {
        String encodedParticipants = writeParticipantInfo(participants);
        if (logger.isActivated()) {
            logger.debug("updateGroupChatParticipant (chatId=" + chatId + ") (participants="
                    + encodedParticipants + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_PARTICIPANTS, encodedParticipants);
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatLog#updateGroupChatRejoinIdOnSessionStart
     * (java.lang.String, java.lang.String)
     */
    @Override
    public void setGroupChatRejoinId(String chatId, String rejoinId) {
        if (logger.isActivated()) {
            logger.debug("Update group chat rejoin ID to " + rejoinId);
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_REJOIN_ID, rejoinId);
        values.put(ChatData.KEY_STATE, GroupChat.State.STARTED);
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatInfo(java.lang.String)
     */
    @Override
    public GroupChatInfo getGroupChatInfo(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for " + chatId);
        }
        GroupChatInfo result = null;
        Cursor cursor = null;

        // @formatter:off
		String[] projection = new String[] { ChatData.KEY_CHAT_ID, ChatData.KEY_REJOIN_ID, ChatData.KEY_PARTICIPANTS,
				ChatData.KEY_SUBJECT };
		// @formatter:on
        String[] selArgs = new String[] {
            chatId
        };
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                // Decode list of participants
                Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(mCtx,
                        cursor.getString(2));
                result = new GroupChatInfo(cursor.getString(0), cursor.getString(1), chatId,
                        participants, cursor.getString(3));
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatConnectedParticipants(java
     * .lang.String)
     */
    @Override
    public Set<ParticipantInfo> getGroupChatConnectedParticipants(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get connected participants for " + chatId);
        }
        Set<ParticipantInfo> result = new HashSet<ParticipantInfo>();
        String[] projection = new String[] {
            ChatData.KEY_PARTICIPANTS
        };
        String[] selArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                // Decode list of participants
                Set<ParticipantInfo> participants = ChatLog.GroupChat.getParticipantInfo(mCtx,
                        cursor.getString(0));
                if (participants != null) {
                    for (ParticipantInfo participantInfo : participants) {
                        // Only consider participants who have not declined or left GC
                        switch (participantInfo.getStatus()) {
                            case ParticipantInfo.Status.DEPARTED:
                            case ParticipantInfo.Status.DECLINED:
                                break;
                            default:
                                result.add(participantInfo);
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error("Exception", e);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * @see
     * com.orangelabs.rcs.provider.messaging.IGroupChatLog#isGroupChatNextInviteRejected(java.lang
     * .String)
     */
    @Override
    public boolean isGroupChatNextInviteRejected(String chatId) {
        String[] projection = {
            ChatData.KEY_CHAT_ID
        };
        String[] selectionArgs = {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection,
                    SELECT_CHAT_ID_STATUS_REJECTED, selectionArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.getCount() != 0) {
                return true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatData( java.lang.String,
     * java.lang.String)
     */
    private Cursor getGroupChatData(String columnName, String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat info for " + chatId);
        }
        String[] projection = new String[] {
            columnName
        };
        String[] selArgs = new String[] {
            chatId
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection, SELECT_CHAT_ID,
                    selArgs, ORDER_BY_TIMESTAMP_DESC);
            if (cursor.moveToFirst()) {
                return cursor;
            }

            throw new SQLException(
                    "No row returned while querying for group chat data with chatId : " + chatId);

        } catch (RuntimeException e) {
            if (logger.isActivated()) {
                logger.error("Error in retrieving group chat info with chatId : " + chatId);
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

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getParticipants(java .lang.String)
     */
    public Set<ParticipantInfo> getParticipants(String participants) {
        return ChatLog.GroupChat.getParticipantInfo(mCtx, participants);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatDirection
     * (java.lang.String)
     */
    public int getGroupChatDirection(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat direction for " + chatId);
        }
        return getDataAsInt(getGroupChatData(ChatData.KEY_DIRECTION, chatId));
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatState (java.lang.String)
     */
    public int getGroupChatState(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat state for " + chatId);
        }
        return getDataAsInt(getGroupChatData(ChatData.KEY_STATE, chatId));
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatReasonCode
     * (java.lang.String)
     */
    public int getGroupChatReasonCode(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat reason code for " + chatId);
        }
        return getDataAsInt(getGroupChatData(ChatData.KEY_REASON_CODE, chatId));
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getSubject(java.lang .String)
     */
    public String getSubject(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat subject for " + chatId);
        }
        return getDataAsString(getGroupChatData(ChatData.KEY_SUBJECT, chatId));
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#getGroupChatParticipants
     * (java.lang.String)
     */
    public Set<ParticipantInfo> getGroupChatParticipants(String chatId) {
        if (logger.isActivated()) {
            logger.debug("Get group chat participants for " + chatId);
        }
        return getParticipants(getDataAsString(getGroupChatData(ChatData.KEY_PARTICIPANTS, chatId)));
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#setRejectNextGroupChatNextInvitation
     * (java.lang.String)
     */
    public void setRejectNextGroupChatNextInvitation(String chatId) {
        if (logger.isActivated()) {
            logger.debug("setRejectNextGroupChatNextInvitation (chatId=" + chatId + ")");
        }
        ContentValues values = new ContentValues();
        values.put(ChatData.KEY_USER_ABORTION, UserAbortion.SERVER_NOT_NOTIFIED.toInt());
        mLocalContentResolver.update(ChatData.CONTENT_URI, values, ChatData.KEY_CHAT_ID + " = '"
                + chatId + "'", null);
    }

    /*
     * (non-Javadoc)
     * @see com.orangelabs.rcs.provider.messaging.IGroupChatLog#
     * retrieveChatIdsOfActiveGroupChatsForAutoRejoin
     */
    public List<String> getChatIdsOfActiveGroupChatsForAutoRejoin() {
        String[] projection = new String[] {
            ChatData.KEY_CHAT_ID
        };
        Cursor cursor = null;
        try {
            cursor = mLocalContentResolver.query(ChatData.CONTENT_URI, projection,
                    SELECT_ACTIVE_GROUP_CHATS, null, null);
            List<String> activeGroupChats = new ArrayList<String>();
            while (cursor.moveToNext()) {
                String chatId = cursor.getString(FIRST_COLUMN_IDX);
                activeGroupChats.add(chatId);
            }
            return activeGroupChats;

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
