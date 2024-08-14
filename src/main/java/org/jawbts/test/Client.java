package org.jawbts.test;

import org.jawbts.voskSocket.EncryptUtils;

import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.io.*;
import java.net.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.sound.sampled.*;

public class Client {
    public static void main(String[] args) throws IOException {
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 60000, 16, 2, 4, 44100, false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        for (Mixer.Info info1 : AudioSystem.getMixerInfo()) {
            System.out.println(Arrays.toString(AudioSystem.getMixer(info1).getTargetLineInfo()));
        }
        // doClient();
    }

    public static void doClient() throws IOException {
        String serverAddress = "localhost";
        int portNumber = 25550;

        try (Socket socket = new Socket(serverAddress, portNumber);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to the server.");

            // 发送消息到服务器
            out.println("CBD:/voskModels/ensmall+60000+16+2+4+44100");
            out.println("CMO");

            // 接收服务器的响应
            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Received from server: " + response);
                if (response.startsWith("!_C")) {
                    System.out.println(decrypt("fuck2", response.substring(3)));
                }
            }
        } catch (UnknownHostException e) {
            System.err.println("Server not found: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("I/O Error: " + e.getMessage());
        }
    }

    public static String decrypt(String password, String s) {
        PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory factory;
        Key key;

        byte[] salt = Base64.getUrlDecoder().decode(s.substring(0, 12));

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

        Cipher cipher;
        try {
            cipher = Cipher.getInstance("PBEWITHMD5andDES");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        PBEParameterSpec pbeParameterSpec = new PBEParameterSpec(salt,100);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, pbeParameterSpec);
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        }

        byte[] result = Base64.getUrlDecoder().decode(s.substring(12));
        try {
            result = cipher.doFinal(result);
        } catch (IllegalBlockSizeException e) {
            // 这不应该发生
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            System.out.println("Wrong password");
            return "";
        }

        return new String(result);
    }
}