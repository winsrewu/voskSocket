package org.jawbts.voskSocket;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class EncryptUtils {
    private static final EncryptUtils INSTANCE = new EncryptUtils();

    public static EncryptUtils getInstance() {
        return INSTANCE;
    }

    private Key key;
    private final SecureRandom random = new SecureRandom();

    public void init(String password) {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory factory;
        try {
            factory = SecretKeyFactory.getInstance("PBEWITHMD5andDES");
        } catch (NoSuchAlgorithmException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        try {
            key = factory.generateSecret(pbeKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String encrypt(String s) {
        byte[] salt = random.generateSeed(8);

        PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt,100);
        Cipher cipher;
        try {
            cipher = Cipher.getInstance("PBEWITHMD5andDES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, pbeParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        byte[] result;
        try {
            result = cipher.doFinal(s.getBytes());
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        return new String(Base64.getUrlEncoder().encode(salt)) + new String(Base64.getUrlEncoder().encode(result));
    }
}
