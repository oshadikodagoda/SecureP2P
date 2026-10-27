package securep2p.crypto;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

public class CryptoEngine {

    // --- RSA Methods ---
    public static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048); // 2048-bit key is standard and secure
        return generator.generateKeyPair();
    }

    public static String encryptRSA(String data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    // --- AES Methods ---
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256); // 256-bit AES
        return keyGen.generateKey();
    }

    // Convert AES Key to String so it can be sent over the network
    public static String aesKeyToString(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    // Convert String back to AES Key
    public static SecretKey stringToAesKey(String keyString) {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        return new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
    }

    public static String encryptAES(String data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decryptAES(String encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedBytes = cipher.doFinal(decodedBytes);
        return new String(decryptedBytes, "UTF-8");
    }

    // Helper method to send public keys over the network
    public static String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    public static PublicKey stringToPublicKey(String keyString) throws Exception {
        byte[] decodedKey = Base64.getDecoder().decode(keyString);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(decodedKey);
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
    
    public static byte[] encryptAESBytes(byte[] data, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(data);
    }

    public static byte[] decryptAESBytes(byte[] encryptedData, SecretKey secretKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedData);
    }
}