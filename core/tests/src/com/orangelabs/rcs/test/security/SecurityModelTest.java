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
package com.orangelabs.rcs.test.security;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

import android.test.AndroidTestCase;

import com.orangelabs.rcs.provider.security.SecurityInfos;
import com.orangelabs.rcs.provider.settings.RcsSettings;
import com.orangelabs.rcs.provisioning.ProvisioningParser;

/**
 * Test the security model
 * 
 * @author JEXA7410
 */
public class SecurityModelTest extends AndroidTestCase {
	protected void setUp() throws Exception {
		super.setUp();
		
		RcsSettings.createInstance(getContext());
		SecurityInfos.createInstance(getContext());
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private String loadConfigFile(String file) {
	    try {
		    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    	InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(file);
		    int i;
	        i = inputStream.read();
	        while (i != -1) {
	            outputStream.write(i);
	            i = inputStream.read();
	        }
	        inputStream.close();
		    return outputStream.toString();
	    } catch(Exception e) {
	    	return null;
	    }
	}
	
	public void testAuthorized() {
		String content = loadConfigFile("assets/template-ota_config-allowed.xml");
		ProvisioningParser parser = new ProvisioningParser(content);
		int gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
		boolean result = parser.parse(gsmaRelease, true);
		assertTrue(result);
		assertTrue(RcsSettings.getInstance().isExtensionsAllowed());
		
		
		List<String> list = SecurityInfos.getInstance().getIARICert("urn:urn-7:3gpp-application.ims.iari.rcs.mnc099.mcc099.demo1");
		assertEquals(list.size(), 2);
		assertEquals(list.get(0), "MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t_1A");
		assertEquals(list.get(1), "MIIDEzCCAfugAwIBAgIERnLjKTANBgkqhkiG9w0BAQsFADAYMRYwFAYDVQQDEw1t_1B");
	}

	public void testNotAllowed() {
		String content = loadConfigFile("assets/template-ota_config-not-allowed.xml");
		ProvisioningParser parser = new ProvisioningParser(content);
		int gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
		boolean result = parser.parse(gsmaRelease, true);
		assertTrue(result);
		assertFalse(RcsSettings.getInstance().isExtensionsAllowed());
	}

	public void testIariAllowed() {
		String content = loadConfigFile("assets/template-ota_config-allowed.xml");
		ProvisioningParser parser = new ProvisioningParser(content);
		int gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
		boolean result = parser.parse(gsmaRelease, true);
		assertTrue(result);
		assertTrue(RcsSettings.getInstance().isExtensionsAllowed());
	}

	public void testMnoApp() {
		String content = loadConfigFile("assets/template-ota_config-allowed-mno.xml");
		ProvisioningParser parser = new ProvisioningParser(content);
		int gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
		boolean result = parser.parse(gsmaRelease, true);
		assertTrue(result);
		assertEquals(RcsSettings.getInstance().getExtensionspolicy(), 0);
	}

	public void test3ppApp() {
		String content = loadConfigFile("assets/template-ota_config-allowed-3pp.xml");
		ProvisioningParser parser = new ProvisioningParser(content);
		int gsmaRelease = RcsSettings.getInstance().getGsmaRelease();
		boolean result = parser.parse(gsmaRelease, true);
		assertTrue(result);
		assertEquals(RcsSettings.getInstance().getExtensionspolicy(), 1);
	}
}