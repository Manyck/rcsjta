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

package com.gsma.rcs.core.ims.service.extension;

import com.gsma.rcs.core.Core;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.capability.CapabilityService;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service extension manager which adds supported extension after having verified some authorization
 * rules.
 * 
 * @author Jean-Marc AUFFRET
 * @author YPLO6403
 */
public class ServiceExtensionManager {

    /**
     * Singleton of ServiceExtensionManager
     */
    private static volatile ServiceExtensionManager mInstance;

    private static final String EXTENSION_SEPARATOR = ";";

    private final static Logger sLogger = Logger.getLogger(ServiceExtensionManager.class
            .getSimpleName());

    private RcsSettings mRcsSettings;

    /**
     * Empty constructor : prevent caller from creating multiple instances
     * 
     * @param rcsSettings
     */
    private ServiceExtensionManager(RcsSettings rcsSettings) {
        mRcsSettings = rcsSettings;
    }

    /**
     * Get an instance of ServiceExtensionManager.
     * 
     * @param rcsSettings
     * @return the singleton instance.
     */
    public static ServiceExtensionManager getInstance(RcsSettings rcsSettings) {
        if (mInstance == null) {
            synchronized (ServiceExtensionManager.class) {
                if (mInstance == null) {
                    mInstance = new ServiceExtensionManager(rcsSettings);
                }
            }
        }
        return mInstance;
    }

    /**
     * Save supported extensions in database
     * 
     * @param supportedExts List of supported extensions
     */
    private void saveSupportedExtensions(Set<String> supportedExts) {
        // Update supported extensions in database
        mRcsSettings.setSupportedRcsExtensions(supportedExts);
    }

    /**
     * Check if the extensions are valid. Each valid extension is saved in the cache.
     * 
     * @param context Context
     * @param supportedExts Set of supported extensions
     * @param newExts Set of new extensions to be checked
     */
    private void checkExtensions(Context context, Set<String> supportedExts, Set<String> newExts) {
        // Check each new extension
        for (String extension : newExts) {
            if (isExtensionAuthorized(context, extension)) {
                if (supportedExts.contains(extension)) {
                    if (sLogger.isActivated()) {
                        sLogger.debug("Extension " + extension + " is already in the list");
                    }
                } else {
                    // Add the extension in the supported list if authorized and not yet in the list
                    supportedExts.add(extension);
                    if (sLogger.isActivated()) {
                        sLogger.debug("Extension " + extension + " is added to the list");
                    }
                }
            }
        }
    }

    /**
     * Update supported extensions at boot or upon install/remove of client application
     * 
     * @param context Context
     */
    public void updateSupportedExtensions(Context context) {
        boolean logActivated = sLogger.isActivated();
        if (context == null) {
            return;
        }
        if (logActivated) {
            sLogger.debug("Update supported extensions");
        }
        try {
            Set<String> newSupportedExts = new HashSet<String>();
            Set<String> oldSupportedExts = mRcsSettings.getSupportedRcsExtensions();
            // Intent query on current installed activities
            PackageManager pm = context.getPackageManager();
            List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo appInfo : apps) {
                Bundle appMeta = appInfo.metaData;
                if (appMeta != null) {
                    String exts = appMeta.getString(CapabilityService.INTENT_EXTENSIONS);
                    if (!TextUtils.isEmpty(exts)) {
                        if (logActivated) {
                            sLogger.debug("Update supported extensions ".concat(exts));
                        }
                        checkExtensions(context, newSupportedExts, getExtensions(exts));
                    }
                }
            }
            if (oldSupportedExts.equals(newSupportedExts)) {
                return;
            }
            // Update supported extensions in database
            saveSupportedExtensions(newSupportedExts);
            Core core = Core.getInstance();
            if (core == null || !core.isStarted()) {
                /* Stack is not started, don't process this event */
                return;
            }
            core.getImsModule().getSipManager().getNetworkInterface().getRegistrationManager()
                    .restart();
        } catch (RuntimeException e) {
            /*
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error("Failed to update supported extensions!", e);
        }
    }

    /**
     * Is extension authorized
     * 
     * @param context Context
     * @param ext Extension ID
     * @return Boolean
     */
    public boolean isExtensionAuthorized(Context context, String ext) {
        if (!mRcsSettings.isExtensionsAllowed()) {
            if (sLogger.isActivated()) {
                sLogger.debug("Extensions are not allowed");
            }
            return false;
        }
        if (sLogger.isActivated()) {
            sLogger.debug("No control on extensions");
        }
        return true;
    }

    /**
     * Remove supported extensions
     * 
     * @param context Context
     */
    public void removeSupportedExtensions(Context context) {
        updateSupportedExtensions(context);
    }

    /**
     * Add supported extensions
     * 
     * @param context Context
     */
    public void addNewSupportedExtensions(Context context) {
        updateSupportedExtensions(context);
    }

    /**
     * Extract set of extensions from String
     * 
     * @param extensions String where extensions are concatenated with a ";" separator
     * @return the set of extensions
     */
    public static Set<String> getExtensions(String extensions) {
        Set<String> result = new HashSet<String>();
        if (TextUtils.isEmpty(extensions)) {
            return result;

        }
        String[] extensionList = extensions.split(ServiceExtensionManager.EXTENSION_SEPARATOR);
        for (String extension : extensionList) {
            if (!TextUtils.isEmpty(extension) && extension.trim().length() > 0) {
                result.add(extension);
            }
        }
        return result;
    }

    /**
     * Concatenate set of extensions into a string
     * 
     * @param extensions set of extensions
     * @return String where extensions are concatenated with a ";" separator
     */
    public static String getExtensions(Set<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            return "";

        }
        StringBuilder result = new StringBuilder();
        int size = extensions.size();
        for (String extension : extensions) {
            if (extension.trim().length() == 0) {
                --size;
                continue;

            }
            result.append(extension);
            if (--size != 0) {
                // Not last item : add separator
                result.append(EXTENSION_SEPARATOR);
            }
        }
        return result.toString();
    }

}
