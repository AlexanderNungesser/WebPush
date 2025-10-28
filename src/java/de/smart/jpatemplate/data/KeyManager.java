/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileWriter;
import java.security.KeyPair;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * A class responsible for managing keys.
 */
public class KeyManager {
    private static final String KEY_PATH = "./vapid_keypair.pem";
    
    /**
     * Creates or reads a keypair.
     * @return the keypair
     */
    public static KeyPair getKeyPair() {
        initProvider();
        KeyPair keyPair = readKeyPair(KEY_PATH);
        if (keyPair == null) {
            try{      
                keyPair = generateKeyPair();
            }catch(Exception e){}
        }
        return keyPair;
    }
    
    /**
     * Converts the public PEM yey of a keypair to a ECDH Key used by the browser.
     * @param keyPair A keypair in PEM format
     * @return the public key in ECDH format
     */
    public static String convertPublicKey(KeyPair keyPair) {
       return toBase64Url(getUncompressedPublicKey((ECPublicKey) keyPair.getPublic()));
    }
    
    
    private static void writeKeyToFile(String filename, String title, byte[] bytes) throws IOException {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(bytes);
        
        try (FileWriter writer = new FileWriter(filename, StandardCharsets.US_ASCII)) {
            writer.write("-----BEGIN " + title + "-----\n");
            writer.write(base64 + "\n");
            writer.write("-----END " + title + "-----\n");
        }
    }
    
    private static KeyPair readKeyPair(String path){
        PEMKeyPair pemKeyPair;
        
        try (InputStream inputStream = KeyManager.class.getResourceAsStream(path)) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            PEMParser pemParser;
            pemParser = new PEMParser(inputStreamReader);
            pemKeyPair = (PEMKeyPair) pemParser.readObject();
            return new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
        }catch(Exception e){
            return null;
        }
    }
    
    private static void initProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    private static KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException{
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();

        writeKeyToFile("vapid_keypair.pem", "EC PRIVATE KEY", keyPair.getPrivate().getEncoded());
        writeKeyToFile("vapid_keypair.pem", "PUBLIC KEY", keyPair.getPublic().getEncoded());

        return keyPair;
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
