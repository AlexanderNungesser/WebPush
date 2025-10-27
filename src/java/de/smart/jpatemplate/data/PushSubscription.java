/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import java.security.Security; 
/**
 *
 * @author maxst
 */
public class PushSubscription {
    private String auth;
	private String key;
	private String endpoint;
        private String test;

        public PushSubscription() {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        }
        
	public void setAuth(String auth) {
		this.auth = auth;
	}

	public String getAuth() {
		return auth;
	}
        
        public String getTest() { return test;}

	/**
	 * Returns the base64 encoded auth string as a byte[]
	 */
	public byte[] getAuthAsBytes() {
		return Base64.getDecoder().decode(getAuth());
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getKey() {
		return key;
	}

	/**
	 * Returns the base64 encoded public key string as a byte[]
	 */
	public byte[] getKeyAsBytes() {
		return Base64.getDecoder().decode(getKey());
	}

	/**
	 * Returns the base64 encoded public key as a PublicKey object
	 */
	public PublicKey getUserPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
            KeyFactory kf = KeyFactory.getInstance("ECDH", BouncyCastleProvider.PROVIDER_NAME);
            ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec("secp256r1");
            ECPoint point = ecSpec.getCurve().decodePoint(getKeyAsBytes());
            ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);

            return kf.generatePublic(pubSpec);
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getEndpoint() {
		return endpoint;
	}
}

