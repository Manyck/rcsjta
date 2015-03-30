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

package com.gsma.rcs.provisioning.local;

import static com.gsma.rcs.provisioning.local.Provisioning.saveCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.saveEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setCheckBoxParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setEditTextParam;
import static com.gsma.rcs.provisioning.local.Provisioning.setSpinnerParameter;

import com.gsma.rcs.R;
import com.gsma.rcs.core.ims.service.extension.CertificateProvisioning;
import com.gsma.rcs.provider.LocalContentResolver;
import com.gsma.rcs.provider.security.SecurityLog;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.provider.settings.RcsSettingsData;
import com.gsma.rcs.provider.settings.RcsSettingsData.AuthenticationProcedure;
import com.gsma.rcs.provider.settings.RcsSettingsData.ConfigurationMode;
import com.gsma.rcs.provider.settings.RcsSettingsData.GsmaRelease;
import com.gsma.rcs.provisioning.ProvisioningParser;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.CommonServiceConfiguration.MessagingMode;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * End user profile parameters provisioning
 * 
 * @author jexa7410
 */
public class ProfileProvisioning extends Activity {
    /**
     * IMS authentication for mobile access
     */
    private static final String[] MOBILE_IMS_AUTHENT = {
            AuthenticationProcedure.GIBA.name(), AuthenticationProcedure.DIGEST.name()
    };

    /**
     * IMS authentication for Wi-Fi access
     */
    private static final String[] WIFI_IMS_AUTHENT = {
        AuthenticationProcedure.DIGEST.name()
    };

    private static Logger logger = Logger.getLogger(ProfileProvisioning.class.getSimpleName());

    private static final String PROVISIONING_EXTENSION = ".xml";
    private String mInputedUserPhoneNumber;
    private String mSelectedProvisioningFile;

    private boolean mInFront;

    private RcsSettings mRcsSettings;

    /**
     * Folder path for provisioning file
     */
    private static final String PROVISIONING_FOLDER_PATH = Environment
            .getExternalStorageDirectory().getPath();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set layout
        setContentView(R.layout.rcs_provisioning_profile);

        // Set buttons callback
        Button btn = (Button) findViewById(R.id.save_btn);
        btn.setOnClickListener(saveBtnListener);
        btn = (Button) findViewById(R.id.gen_btn);
        btn.setOnClickListener(genBtnListener);

        mRcsSettings = RcsSettings.createInstance(new LocalContentResolver(this));

        updateProfileProvisioningUI(savedInstanceState);
        mInFront = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mInFront == false) {
            mInFront = true;
            // Update UI (from DB)
            updateProfileProvisioningUI(null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mInFront = false;
    }

    /**
     * Update Profile Provisioning UI
     * 
     * @param bundle bundle to save parameters
     */
    private void updateProfileProvisioningUI(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        // Display parameters
        Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ProfileProvisioning.this,
                android.R.layout.simple_spinner_item, MOBILE_IMS_AUTHENT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        setSpinnerParameter(spinner, RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                MOBILE_IMS_AUTHENT, helper);

        spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
        adapter = new ArrayAdapter<String>(ProfileProvisioning.this,
                android.R.layout.simple_spinner_item, WIFI_IMS_AUTHENT);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(0);

        setEditTextParam(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME, helper);
        setEditTextParam(R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, helper);
        setEditTextParam(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, helper);
        setEditTextParam(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, helper);
        setEditTextParam(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, helper);
        setEditTextParam(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, helper);
        setEditTextParam(R.id.ImsOutboundProxyAddrForMobile, RcsSettingsData.IMS_PROXY_ADDR_MOBILE,
                helper);
        setEditTextParam(R.id.ImsOutboundProxyPortForMobile, RcsSettingsData.IMS_PROXY_PORT_MOBILE,
                helper);
        setEditTextParam(R.id.ImsOutboundProxyAddrForWifi, RcsSettingsData.IMS_PROXY_ADDR_WIFI,
                helper);
        setEditTextParam(R.id.ImsOutboundProxyPortForWifi, RcsSettingsData.IMS_PROXY_PORT_WIFI,
                helper);
        setEditTextParam(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, helper);
        setEditTextParam(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, helper);
        setEditTextParam(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, helper);
        setEditTextParam(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, helper);
        setEditTextParam(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, helper);
        setEditTextParam(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, helper);
        setEditTextParam(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, helper);
        setEditTextParam(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI, helper);
        setEditTextParam(R.id.RcsApn, RcsSettingsData.RCS_APN, helper);

        setCheckBoxParam(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, helper);
        setCheckBoxParam(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, helper);
        setCheckBoxParam(R.id.file_transfer, RcsSettingsData.CAPABILITY_FILE_TRANSFER, helper);
        setCheckBoxParam(R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                helper);
        setCheckBoxParam(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, helper);
        setCheckBoxParam(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, helper);
        setCheckBoxParam(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, helper);
        setCheckBoxParam(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, helper);
        setCheckBoxParam(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, helper);
        setCheckBoxParam(R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                helper);
        setCheckBoxParam(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, helper);
        setCheckBoxParam(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH, helper);
        setCheckBoxParam(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, helper);
        setCheckBoxParam(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF, helper);
        setCheckBoxParam(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, helper);
        setCheckBoxParam(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, helper);
        TextView txt = (TextView) findViewById(R.id.release);
        txt.setText(mRcsSettings.getGsmaRelease().name());
    }

    /**
     * Save button listener
     */
    private OnClickListener saveBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Save parameters
            saveInstanceState(null);
            Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service),
                    Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        saveInstanceState(bundle);
    }

    /**
     * Save parameters either in bundle or in RCS settings
     */
    private void saveInstanceState(Bundle bundle) {
        ProvisioningHelper helper = new ProvisioningHelper(this, mRcsSettings, bundle);

        Spinner spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForMobile);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_MOBILE,
                    spinner.getSelectedItemPosition());
        } else {
            AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String) spinner
                    .getSelectedItem());
            mRcsSettings.setImsAuthenticationProcedureForMobile(procedure);
        }

        spinner = (Spinner) findViewById(R.id.ImsAuthenticationProcedureForWifi);
        if (bundle != null) {
            bundle.putInt(RcsSettingsData.IMS_AUTHENT_PROCEDURE_WIFI,
                    spinner.getSelectedItemPosition());
        } else {
            AuthenticationProcedure procedure = AuthenticationProcedure.valueOf((String) spinner
                    .getSelectedItem());
            mRcsSettings.setImsAuhtenticationProcedureForWifi(procedure);
        }

        saveEditTextParam(R.id.ImsUsername, RcsSettingsData.USERPROFILE_IMS_USERNAME, helper);
        saveEditTextParam(R.id.ImsDisplayName, RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME, helper);
        saveEditTextParam(R.id.ImsHomeDomain, RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN, helper);
        saveEditTextParam(R.id.ImsPrivateId, RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, helper);
        saveEditTextParam(R.id.ImsPassword, RcsSettingsData.USERPROFILE_IMS_PASSWORD, helper);
        saveEditTextParam(R.id.ImsRealm, RcsSettingsData.USERPROFILE_IMS_REALM, helper);
        saveEditTextParam(R.id.ImsOutboundProxyAddrForMobile,
                RcsSettingsData.IMS_PROXY_ADDR_MOBILE, helper);
        saveEditTextParam(R.id.ImsOutboundProxyPortForMobile,
                RcsSettingsData.IMS_PROXY_PORT_MOBILE, helper);
        saveEditTextParam(R.id.ImsOutboundProxyAddrForWifi, RcsSettingsData.IMS_PROXY_ADDR_WIFI,
                helper);
        saveEditTextParam(R.id.ImsOutboundProxyPortForWifi, RcsSettingsData.IMS_PROXY_PORT_WIFI,
                helper);
        saveEditTextParam(R.id.XdmServerAddr, RcsSettingsData.XDM_SERVER, helper);
        saveEditTextParam(R.id.XdmServerLogin, RcsSettingsData.XDM_LOGIN, helper);
        saveEditTextParam(R.id.XdmServerPassword, RcsSettingsData.XDM_PASSWORD, helper);
        saveEditTextParam(R.id.FtHttpServerAddr, RcsSettingsData.FT_HTTP_SERVER, helper);
        saveEditTextParam(R.id.FtHttpServerLogin, RcsSettingsData.FT_HTTP_LOGIN, helper);
        saveEditTextParam(R.id.FtHttpServerPassword, RcsSettingsData.FT_HTTP_PASSWORD, helper);
        saveEditTextParam(R.id.ImConferenceUri, RcsSettingsData.IM_CONF_URI, helper);
        saveEditTextParam(R.id.EndUserConfReqUri, RcsSettingsData.ENDUSER_CONFIRMATION_URI, helper);
        saveEditTextParam(R.id.RcsApn, RcsSettingsData.RCS_APN, helper);

        // Save capabilities
        saveCheckBoxParam(R.id.image_sharing, RcsSettingsData.CAPABILITY_IMAGE_SHARING, helper);
        saveCheckBoxParam(R.id.video_sharing, RcsSettingsData.CAPABILITY_VIDEO_SHARING, helper);
        saveCheckBoxParam(R.id.file_transfer, RcsSettingsData.CAPABILITY_FILE_TRANSFER, helper);
        saveCheckBoxParam(R.id.file_transfer_http, RcsSettingsData.CAPABILITY_FILE_TRANSFER_HTTP,
                helper);
        saveCheckBoxParam(R.id.im, RcsSettingsData.CAPABILITY_IM_SESSION, helper);
        saveCheckBoxParam(R.id.im_group, RcsSettingsData.CAPABILITY_IM_GROUP_SESSION, helper);
        saveCheckBoxParam(R.id.ipvoicecall, RcsSettingsData.CAPABILITY_IP_VOICE_CALL, helper);
        saveCheckBoxParam(R.id.ipvideocall, RcsSettingsData.CAPABILITY_IP_VIDEO_CALL, helper);
        saveCheckBoxParam(R.id.cs_video, RcsSettingsData.CAPABILITY_CS_VIDEO, helper);
        saveCheckBoxParam(R.id.presence_discovery, RcsSettingsData.CAPABILITY_PRESENCE_DISCOVERY,
                helper);
        saveCheckBoxParam(R.id.social_presence, RcsSettingsData.CAPABILITY_SOCIAL_PRESENCE, helper);
        saveCheckBoxParam(R.id.geolocation_push, RcsSettingsData.CAPABILITY_GEOLOCATION_PUSH,
                helper);
        saveCheckBoxParam(R.id.file_transfer_thumbnail,
                RcsSettingsData.CAPABILITY_FILE_TRANSFER_THUMBNAIL, helper);
        saveCheckBoxParam(R.id.file_transfer_sf, RcsSettingsData.CAPABILITY_FILE_TRANSFER_SF,
                helper);
        saveCheckBoxParam(R.id.group_chat_sf, RcsSettingsData.CAPABILITY_GROUP_CHAT_SF, helper);
        saveCheckBoxParam(R.id.sip_automata, RcsSettingsData.CAPABILITY_SIP_AUTOMATA, helper);
    }

    /**
     * Generate profile button listener
     */
    private OnClickListener genBtnListener = new OnClickListener() {
        public void onClick(View v) {
            // Load the user profile
            loadProfile();
        }
    };

    /**
     * Load the user profile
     */
    private void loadProfile() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View view = factory.inflate(R.layout.rcs_provisioning_generate_profile, null);
        final EditText textEdit = (EditText) view.findViewById(R.id.msisdn);
        textEdit.setText(mRcsSettings.getUserProfileImsUserName());

        String[] xmlFiles = getProvisioningFiles();
        final Spinner spinner = (Spinner) view.findViewById(R.id.XmlProvisioningFile);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, xmlFiles);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.label_generate_profile).setView(view)
                .setNegativeButton(R.string.label_cancel, null)
                .setPositiveButton(R.string.label_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mInputedUserPhoneNumber = textEdit.getText().toString();
                        mSelectedProvisioningFile = (String) spinner.getSelectedItem();
                        if (mSelectedProvisioningFile != null
                                && !mSelectedProvisioningFile
                                        .equals(getString(R.string.label_no_xml_file))) {
                            String filePath = PROVISIONING_FOLDER_PATH + File.separator
                                    + mSelectedProvisioningFile;
                            if (logger.isActivated()) {
                                logger.debug("Selection of provisioning file: "
                                        + mSelectedProvisioningFile);
                            }
                            String mXMLFileContent = getFileContent(filePath);
                            if (mXMLFileContent != null) {
                                if (logger.isActivated()) {
                                    logger.debug("Selection of provisioning file: " + filePath);
                                }
                                ProvisionTask mProvisionTask = new ProvisionTask();
                                mProvisionTask.execute(mXMLFileContent, mInputedUserPhoneNumber);
                                return;
                            }
                        }
                        Toast.makeText(ProfileProvisioning.this,
                                getString(R.string.label_load_failed), Toast.LENGTH_LONG).show();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    /**
     * Read a text file and convert it into a string
     * 
     * @param filePath the file path
     * @return the result string
     */
    private String getFileContent(String filePath) {
        if (filePath == null)
            return null;
        // Get the text file
        File file = new File(filePath);

        // Read text from file
        StringBuilder text = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            return text.toString();

        } catch (Exception e) {
            if (logger.isActivated()) {
                logger.error(
                        "Error reading file content: " + e.getClass().getName() + " "
                                + e.getMessage(), e);
            }
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    /**
     * Asynchronous Tasks that loads the provisioning file.
     */
    private class ProvisionTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String UserPhoneNumber = params[1];
            String mXMLFileContent = params[0];
            return createProvisioning(mXMLFileContent, UserPhoneNumber);
        }

        /**
         * Parse the provisioning data then save it into RCS settings provider
         * 
         * @param mXMLFileContent the XML file containing provisioning data
         * @param userPhoneNumber the user phone number
         * @return true if loading the provisioning is successful
         */
        private boolean createProvisioning(String mXMLFileContent, String userPhoneNumber) {
            ContentResolver contentResolver = getContentResolver();
            LocalContentResolver localContentResolver = new LocalContentResolver(contentResolver);

            SecurityLog.createInstance(localContentResolver);
            SecurityLog securityLog = SecurityLog.getInstance();
            ProvisioningParser parser = new ProvisioningParser(mXMLFileContent, mRcsSettings,
                    new CertificateProvisioning(securityLog));
            
            // Save GSMA release set into the provider
            GsmaRelease release = mRcsSettings.getGsmaRelease();
            // Save client Messaging Mode set into the provider
            MessagingMode messagingMode = mRcsSettings.getMessagingMode();

            // Before parsing the provisioning, the GSMA release is set to Albatros
            mRcsSettings.setGsmaRelease(GsmaRelease.ALBATROS);
            // Before parsing the provisioning, the client Messaging mode is set to NONE
            mRcsSettings.setMessagingMode(MessagingMode.NONE);

            if (parser.parse(release, true)) {
                // Customize provisioning data with user phone number
                mRcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_USERNAME,
                        userPhoneNumber);
                mRcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_DISPLAY_NAME,
                        userPhoneNumber);
                String homeDomain = mRcsSettings
                        .readParameter(RcsSettingsData.USERPROFILE_IMS_HOME_DOMAIN);
                String sipUri = userPhoneNumber + "@" + homeDomain;
                mRcsSettings.writeParameter(RcsSettingsData.USERPROFILE_IMS_PRIVATE_ID, sipUri);
                mRcsSettings.writeParameter(RcsSettingsData.FT_HTTP_LOGIN, sipUri);
                return true;
            } else {
                if (logger.isActivated()) {
                    logger.error("Can't parse provisioning document");
                }
                // Restore GSMA release saved before parsing of the provisioning
                mRcsSettings.setGsmaRelease(release);
                // Restore the client messaging mode saved before parsing of the provisioning
                mRcsSettings.setMessagingMode(messagingMode);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            updateProfileProvisioningUI(null);
            // set configuration mode to manual
            mRcsSettings.setConfigurationMode(ConfigurationMode.MANUAL);
            if (result)
                Toast.makeText(ProfileProvisioning.this, getString(R.string.label_reboot_service),
                        Toast.LENGTH_LONG).show();
            else
                Toast.makeText(ProfileProvisioning.this, getString(R.string.label_parse_failed),
                        Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Load a list of provisioning files from the SDCARD
     * 
     * @return List of XML provisioning files
     */
    private String[] getProvisioningFiles() {
        String[] files = null;
        File folder = new File(PROVISIONING_FOLDER_PATH);
        try {
            folder.mkdirs();
            if (folder.exists()) {
                // filter
                FilenameFilter filter = new FilenameFilter() {
                    public boolean accept(File dir, String filename) {
                        return filename.endsWith(PROVISIONING_EXTENSION);
                    }
                };
                files = folder.list(filter);
            }
        } catch (SecurityException e) {
            // intentionally blank
        }
        if ((files == null) || (files.length == 0)) {
            // No provisioning file
            return new String[] {
                getString(R.string.label_no_xml_file)
            };
        } else {
            return files;
        }
    }
}
