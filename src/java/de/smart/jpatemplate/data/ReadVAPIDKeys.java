/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.spec.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.util.Base64;
import java.security.interfaces.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
/**
 *
 * @author steidlemax
 */
public class ReadVAPIDKeys {
    public static KeyPair getKeyPair() {
        initProvider();
        KeyPair keyPair = readKeyPair();
        
        if (keyPair == null) {
            keyPair = generateKeyPair();
        }
        
        return keyPair;
    }
    
    public static String convertVAPIDKey(KeyPair keyPair) {
       return toBase64Url(getUncompressedPublicKey((ECPublicKey) keyPair.getPublic()));
    }
    
    private static void writePEM(String filename, String title, byte[] bytes) throws IOException {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(bytes);
        try (FileWriter writer = new FileWriter(filename, StandardCharsets.US_ASCII)) {
            writer.write("-----BEGIN " + title + "-----\n");
            writer.write(base64 + "\n");
            writer.write("-----END " + title + "-----\n");
        }
    }
    
    private static KeyPair readKeyPair() {
        try {
            InputStream inputStream = ReadVAPIDKeys.class.getResourceAsStream("./vapid_keypair.pem");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            PEMParser pemParser;
            pemParser = new PEMParser(inputStreamReader);
            PEMKeyPair pemKeyPair = (PEMKeyPair) pemParser.readObject();

            return new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
        } catch (Exception exp) {
            return null;
        }
    }
    
    private static void initProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
            keyGen.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair keyPair = keyGen.generateKeyPair();
            
            writePEM("vapid_keypair.pem", "EC PRIVATE KEY", keyPair.getPrivate().getEncoded());
            writePEM("vapid_keypair.pem", "PUBLIC KEY", keyPair.getPublic().getEncoded());
            
            return keyPair;
        } catch (Exception exp) {
            return null;
        }
    }
    
    private static byte[] getUncompressedPublicKey(ECPublicKey publicKey) {
        byte[] x = publicKey.getW().getAffineX().toByteArray();
        byte[] y = publicKey.getW().getAffineY().toByteArray();

        // Auf 32 Bytes normalisieren
        x = normalizeTo32(x);
        y = normalizeTo32(y);

        byte[] uncompressed = new byte[65];
        uncompressed[0] = 0x04;
        System.arraycopy(x, 0, uncompressed, 1, 32);
        System.arraycopy(y, 0, uncompressed, 33, 32);
        return uncompressed;
    }

    private static byte[] normalizeTo32(byte[] arr) {
        if (arr.length == 32) return arr;
        byte[] out = new byte[32];
        if (arr.length > 32)
            System.arraycopy(arr, arr.length - 32, out, 0, 32);
        else
            System.arraycopy(arr, 0, out, 32 - arr.length, arr.length);
        return out;
    }

    private static String toBase64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
