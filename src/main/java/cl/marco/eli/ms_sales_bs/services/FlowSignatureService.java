package cl.marco.eli.ms_sales_bs.services;

import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

@Service
public class FlowSignatureService {

    public String generateSignature(Map<String, String> params, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder concatenatedParams = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            concatenatedParams.append(entry.getKey()).append(entry.getValue());
        }
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hashBytes = sha256_HMAC.doFinal(concatenatedParams.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
