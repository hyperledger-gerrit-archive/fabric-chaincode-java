/*
Copyright IBM Corp., DTCC All Rights Reserved.

SPDX-License-Identifier: Apache-2.0
*/

package org.hyperledger.fabric.contract;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.protobuf.InvalidProtocolBufferException;

import org.hyperledger.fabric.protos.msp.Identities.SerializedIdentity;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.json.JSONObject;

/**
 * ClientIdentity represents information about the identity that submitted a
 * transaction. Chaincodes can use this class to obtain information about the submitting
 * identity including a unique ID, the MSP (Membership Service Provider) ID, and attributes.
 * Such information is useful in enforcing access control by the chaincode.
 *
 */
public class ClientIdentity {
    protected String mspId;
    protected X509Certificate cert;
    protected Map<String, String> attrs;
    protected String id;

    public ClientIdentity(ChaincodeStub stub) throws InvalidProtocolBufferException, CertificateException, UnsupportedEncodingException {
        final byte[] signingId = stub.getCreator();

        SerializedIdentity si = SerializedIdentity.parseFrom(signingId);
        this.mspId = si.getMspid();

        final byte[] idBytes = si.getIdBytes().toByteArray();

        final X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(idBytes));
        this.cert = cert;

        final byte[] extVal = cert.getExtensionValue("1.2.3.4.5.6.7.8.1");

        this.attrs = new HashMap<String, String>();

        if (extVal != null) {
            final String extStr = new String(extVal, "UTF-8");
            final Pattern pattern = Pattern.compile("\\{(.*)");
            final Matcher matcher = pattern.matcher(extStr);

            if (matcher.find()) {
                final String attrJSONString = "{" + matcher.group(1);
                final JSONObject attrJSON = new JSONObject(attrJSONString);

                final JSONObject attrs = attrJSON.getJSONObject("attrs");

                Iterator<String> keys = attrs.keys();

                Map<String, String> attrMap = new HashMap<String, String>();

                while(keys.hasNext()) {
                    String key = keys.next();

                    attrMap.put(key, attrs.getString(key));
                }

                this.attrs = attrMap;
            }
        }

        this.id = "x509::" + cert.getSubjectDN().getName() + "::" + cert.getIssuerDN().getName();
    }

    /**
     * getId returns the ID associated with the invoking identity. This ID
     * is guaranteed to be unique within the MSP.
     * @return {String} A string in the format: "x509::{subject DN}::{issuer DN}"
     */
    public String getId() {
        return this.id;
    }

    /**
     * getMSPID returns the MSP ID of the invoking identity.
     * @return {String}
     */
    public String getMSPID() {
        return this.mspId;
    }

    /**
     * getAttributeValue returns the value of the client's attribute named `attrName`.
     * If the invoking identity possesses the attribute, returns the value of the attribute.
     * If the invoking identity does not possess the attribute, returns null.
     * @param attrName Name of the attribute to retrieve the value from the
     *     identity's credentials (such as x.509 certificate for PKI-based MSPs).
     * @return {String | null} Value of the attribute or null if the invoking identity
     *     does not possess the attribute.
     */
    public String getAttributeValue(String attrName) {
        if (this.attrs.containsKey(attrName)) {
            return this.attrs.get(attrName);
        } else {
            return null;
        }
    }

    /**
     * assertAttributeValue verifies that the invoking identity has the attribute named `attrName`
     * with a value of `attrValue`.
     * @param attrName Name of the attribute to retrieve the value from the
     *     identity's credentials (such as x.509 certificate for PKI-based MSPs)
     * @param attrValue Expected value of the attribute
     * @return {boolean} True if the invoking identity possesses the attribute and the attribute
     *     value matches the expected value. Otherwise, returns false.
     */
    public boolean assertAttributeValue(String attrName, String attrValue) {
        if (!this.attrs.containsKey(attrName)) {
            return false;
        } else {
            return attrValue.equals(this.attrs.get(attrName));
        }
    }

    /**
     * getX509Certificate returns the X509 certificate associated with the invoking identity,
     * or null if it was not identified by an X509 certificate, for instance if the MSP is
     * implemented with an alternative to PKI such as [Identity Mixer](https://jira.hyperledger.org/browse/FAB-5673).
     * @return {X509Certificate | null}
     */
    public X509Certificate getX509Certificate() {
        return this.cert;
    }
}