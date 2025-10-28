/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.smart.jpatemplate.data;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.File;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
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
    
    
    private static void writeKeysToFile(String path, KeyPair keypair) throws IOException {
        String publicKey = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keypair.getPublic().getEncoded());        
        String privateKey = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(keypair.getPrivate().getEncoded());
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile(); 
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        try (FileWriter writer = new FileWriter(path, StandardCharsets.US_ASCII)) {
            writer.write("-----BEGIN PRIVATE KEY-----\n");
            writer.write(privateKey + "\n");
            writer.write("-----END PRIVATE KEY-----\n");            
            writer.write("-----BEGIN PUBLIC KEY-----\n");
            writer.write(publicKey + "\n");
            writer.write("-----END PUBLIC KEY-----");
        }
    }
    
    private static KeyPair readKeyPair(String path) {
        try (FileReader fileReader = new FileReader(path);
             PEMParser pemParser = new PEMParser(fileReader)) {

            Object object;
            PrivateKey privateKey = null;
            PublicKey publicKey = null;
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);

            while ((object = pemParser.readObject()) != null) {
                if (object instanceof PEMKeyPair pEMKeyPair) {
                    return converter.getKeyPair(pEMKeyPair);
                } else if (object instanceof PrivateKeyInfo privateKeyInfo) {
                    privateKey = converter.getPrivateKey(privateKeyInfo);
                } else if (object instanceof SubjectPublicKeyInfo subjectPublicKeyInfo) {
                    publicKey = converter.getPublicKey(subjectPublicKeyInfo);
                }
            }

            if (privateKey != null && publicKey != null) {
                System.out.println("Read an existing Keypair!");
                return new KeyPair(publicKey, privateKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    private static void initProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
    
    private static KeyPair generateKeyPair() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException{
        System.out.println("Generating a new key Pair!");
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDH");
        keyGen.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair keyPair = keyGen.generateKeyPair();
        
        writeKeysToFile(KEY_PATH, keyPair);

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
