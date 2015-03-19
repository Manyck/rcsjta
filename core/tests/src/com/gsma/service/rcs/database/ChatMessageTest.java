/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.gsma.service.rcs.database;

import com.gsma.rcs.core.ims.ImsModule;
import com.gsma.rcs.core.ims.service.im.chat.ChatMessage;
import com.gsma.rcs.core.ims.service.im.chat.ChatUtils;
import com.gsma.rcs.core.ims.userprofile.UserProfile;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.messaging.MessagingLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.Geoloc;
import com.gsma.services.rcs.RcsContactFormatException;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.chat.ChatLog.Message;
import com.gsma.services.rcs.chat.ChatLog.Message.MimeType;
import com.gsma.services.rcs.contact.ContactId;
import com.gsma.services.rcs.contact.ContactUtil;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;

import java.util.Date;

public class ChatMessageTest extends AndroidTestCase {
    private ContactId mContact;
    private Context mContext;
    private ContentResolver mContentResolver;
    private RcsSettings mRcsSettings;
    private MessagingLog mMessagingLog;
    private LocalContentResolver mLocalContentResolver;

    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();
        mContentResolver = mContext.getContentResolver();
        mLocalContentResolver = new LocalContentResolver(mContentResolver);
        mRcsSettings = RcsSettings.createInstance(mLocalContentResolver);
        mMessagingLog = MessagingLog.createInstance(mContext, mLocalContentResolver, mRcsSettings);
        ContactUtil contactUtils = ContactUtil.getInstance(mContext);
        try {
            mContact = contactUtils.formatContact("+339000000");
        } catch (RcsContactFormatException e) {
            fail("Cannot create contactID");
        }
        ImsModule.IMS_USER_PROFILE = new UserProfile(mContact, "homeDomain", "privateID",
                "password", "realm", "xdmServerAddr", "xdmServerLogin", "xdmServerPassword",
                "imConferenceUri", mRcsSettings);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testTextMessage() {
        String msgId = Long.toString(System.currentTimeMillis());
        String txt = "Hello";
        Date now = new Date();
        ChatMessage msg = new ChatMessage(msgId, mContact, txt, MimeType.TEXT_MESSAGE, now,
                "display");

        // Add entry
        mMessagingLog.addOutgoingOneToOneChatMessage(msg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED);

        // Read entry
        Uri uri = Uri.withAppendedPath(Message.CONTENT_URI, msgId);
        Cursor cursor = mContentResolver.query(uri, new String[] {
                Message.DIRECTION, Message.CONTACT, Message.CONTENT, Message.MIME_TYPE,
                Message.MESSAGE_ID, Message.TIMESTAMP
        }, "(" + Message.MESSAGE_ID + "='" + msgId + "')", null, Message.TIMESTAMP + " ASC");
        assertEquals(cursor.getCount(), 1);
        while (cursor.moveToNext()) {
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndex(Message.DIRECTION)));
            String contact = cursor.getString(cursor.getColumnIndex(Message.CONTACT));
            String content = cursor.getString(cursor.getColumnIndex(Message.CONTENT));
            assertNotNull(content);
            String readTxt = new String(content);
            String mimeType = cursor.getString(cursor.getColumnIndex(Message.MIME_TYPE));
            String id = cursor.getString(cursor.getColumnIndex(Message.MESSAGE_ID));
            long readDate = cursor.getLong(cursor.getColumnIndex(Message.TIMESTAMP));
            assertEquals(now.getTime(), readDate);
            assertEquals(direction, Direction.OUTGOING);
            assertEquals(contact, mContact.toString());
            assertEquals(readTxt, txt);
            assertEquals(mimeType, Message.MimeType.TEXT_MESSAGE);
            assertEquals(id, msgId);
        }
        mLocalContentResolver.delete(Uri.withAppendedPath(Message.CONTENT_URI, msgId), null, null);
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }

    public void testGeolocMessage() {
        Geoloc geoloc = new Geoloc("test", 10.0, 11.0, 2000, 2);
        ChatMessage chatMsg = ChatUtils.createGeolocMessage(mContact, geoloc);
        String msgId = chatMsg.getMessageId();
        // Add entry
        mMessagingLog.addOutgoingOneToOneChatMessage(chatMsg, Message.Content.Status.SENT,
                Message.Content.ReasonCode.UNSPECIFIED);

        // Read entry
        Uri uri = Uri.withAppendedPath(Message.CONTENT_URI, msgId);
        Cursor cursor = mContentResolver.query(uri, new String[] {
                Message.DIRECTION, Message.CONTACT, Message.CONTENT, Message.MIME_TYPE,
                Message.MESSAGE_ID
        }, "(" + Message.MESSAGE_ID + "='" + msgId + "')", null, Message.TIMESTAMP + " ASC");
        assertEquals(cursor.getCount(), 1);
        while (cursor.moveToNext()) {
            Direction direction = Direction.valueOf(cursor.getInt(cursor
                    .getColumnIndex(Message.DIRECTION)));
            String contact = cursor.getString(cursor.getColumnIndex(Message.CONTACT));
            String content = cursor.getString(cursor.getColumnIndex(Message.CONTENT));
            assertNotNull(content);
            Geoloc readGeoloc = new Geoloc(content);
            assertNotNull(readGeoloc);

            String contentType = cursor.getString(cursor.getColumnIndex(Message.MIME_TYPE));
            String id = cursor.getString(cursor.getColumnIndex(Message.MESSAGE_ID));

            assertEquals(direction, Direction.OUTGOING);
            assertEquals(contact, mContact.toString());
            assertEquals(readGeoloc.getLabel(), geoloc.getLabel());
            assertEquals(readGeoloc.getLatitude(), geoloc.getLatitude());
            assertEquals(readGeoloc.getLongitude(), geoloc.getLongitude());
            assertEquals(readGeoloc.getExpiration(), geoloc.getExpiration());
            assertEquals(readGeoloc.getAccuracy(), geoloc.getAccuracy());
            assertEquals(contentType, Message.MimeType.GEOLOC_MESSAGE);
            assertEquals(id, msgId);
        }
        mLocalContentResolver.delete(Uri.withAppendedPath(Message.CONTENT_URI, msgId), null, null);
        assertFalse(mMessagingLog.isMessagePersisted(msgId));
    }
}
