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

package com.gsma.rcs.core.ims.service.im.chat;

import java.util.Date;

import com.gsma.rcs.core.ims.service.im.chat.geoloc.GeolocInfoDocument;
import com.gsma.services.rcs.contacts.ContactId;

/**
 * Geoloc message
 * 
 * @author Jean-Marc AUFFRET
 */
public class GeolocMessage extends InstantMessage {
    /**
     * MIME type
     */
    public static final String MIME_TYPE = GeolocInfoDocument.MIME_TYPE;

    /**
     * Geoloc info
     */
    private GeolocPush geoloc = null;

    /**
     * Constructor for outgoing message
     * 
     * @param messageId Message Id
     * @param remote Remote user identifier
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
     * @param displayName the display name of the remote contact
     */
    public GeolocMessage(String messageId, ContactId remote, GeolocPush geoloc,
            boolean imdnDisplayedRequested, String displayName) {
        this(messageId, remote, geoloc, imdnDisplayedRequested, null, displayName);
    }

    /**
     * Constructor for incoming message
     * 
     * @param messageId Message Id
     * @param remote Remote user identifier
     * @param geoloc Geoloc info
     * @param imdnDisplayedRequested Flag indicating that an IMDN "displayed" is requested
     * @param serverReceiptAt Receipt date of the message on the server
     * @param displayName the display name of the remote contact
     */
    public GeolocMessage(String messageId, ContactId remote, GeolocPush geoloc,
            boolean imdnDisplayedRequested, Date serverReceiptAt, String displayName) {
        super(messageId, remote, geoloc.getLabel(), imdnDisplayedRequested, serverReceiptAt,
                displayName);

        this.geoloc = geoloc;
    }

    /**
     * Get geoloc info
     * 
     * @return Geoloc info
     */
    public GeolocPush getGeoloc() {
        return geoloc;
    }

    @Override
    public String toString() {
        return geoloc.toString();
    }

}
