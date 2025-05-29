package com.reneyao.realtime.demo;


import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

public class SignatureUtil {

        // 签名生成方法
        public static String generateSignature(String secretId, String type, String secretKey, String timestamp) throws Exception {
            // 将四个参数放入数组
            String[] paramList = {secretId, type, timestamp, secretKey};
            Arrays.sort(paramList); // 字典序排序

            // 拼接参数
            StringBuilder signatureStr = new StringBuilder();
            for (String s : paramList) {
                signatureStr.append(s);
            }

            // 计算 SHA-256 哈希
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(signatureStr.toString().getBytes(StandardCharsets.UTF_8));

            // 转为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        }

        // 示例调用
        public static void main(String[] args) throws Exception {
            String secretId = "abc123";
            String type = "getInfo";
            String secretKey = "mySecretKey";
            String timestamp = String.valueOf(System.currentTimeMillis());

            String signature = generateSignature(secretId, type, secretKey, timestamp);

            System.out.println("Timestamp: " + timestamp);
            System.out.println("Signature: " + signature);
        }
    }


