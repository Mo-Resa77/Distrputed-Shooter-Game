// " fourth class " 
package shooter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/*Handles encryption of game actions:
Uses AES encryption
Encrypts and decrypts action messages
Ensures secure communication between nodes */
public class Encryptor {

    private static final String key = "1234567890123456"; // 16-byte key 

    /*The key is:
private: Only accessible within this class
static: Shared by all instances
final: Cannot be changed*/
    // تتنادا بالكلاس ستتك
    public static String encrypt(String plainText) throws Exception {
        // 1. Get AES cipher instance
        Cipher cipher = Cipher.getInstance("AES");

        // 2. Create secret key from our key string
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");

        // 3. Initialize cipher for encryption
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        // 4. Encrypt and encode
        return Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
    }

    // same ....
    public static String decrypt(String encryptedText) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        // 4. Decode and decrypt and others are same like encryption
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText)));
    }
}
