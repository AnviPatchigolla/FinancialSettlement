import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class EncryptionUtil{
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 128;
    private final SecretKey key;
    private final SecureRandom secureRandom = new SecureRandom();

    public EncryptionUtil(){
        try{
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);
            key = keyGenerator.generateKey();
        }catch(GeneralSecurityException e){
            throw new IllegalStateException("Encryption is unavailable.", e);
        }
    }

    public byte[] encrypt(String value){
        try{
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            return encryptedData;
        }catch(GeneralSecurityException e){
            throw new IllegalStateException("Unable to encrypt transaction data.", e);
        }
    }

    public String decrypt(byte[] encryptedData){
        if(encryptedData == null || encryptedData.length <= IV_LENGTH){
            throw new IllegalArgumentException("Encrypted transaction data is invalid.");
        }

        try{
            byte[] iv = Arrays.copyOfRange(encryptedData, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(encryptedData, IV_LENGTH, encryptedData.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        }catch(GeneralSecurityException e){
            throw new IllegalArgumentException("Encrypted transaction data is invalid.", e);
        }
    }
}
