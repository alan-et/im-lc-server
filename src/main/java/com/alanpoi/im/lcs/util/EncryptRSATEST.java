package com.alanpoi.im.lcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

//RSA加密解密
public class EncryptRSATEST {
	private static final Logger log = LoggerFactory.getLogger(EncryptRSATEST.class);
	
	public static final String KEY_ALGORITHM = "RSA"; 
	
	public static RSAPrivateKey privateKey;
	public static RSAPublicKey publicKey;
	
	//加载私钥
	public static void loadPrivateKey(String path) throws Exception{
		String keyStr = loadKeyFile(path);
		//log.info("keystr:{}", keyStr);
		//BASE64Decoder b64Decoder = new BASE64Decoder();
		byte[] buf = Base64.getDecoder().decode(keyStr);
		//byte[] buf = Base64.decodeBase64(keyStr);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(buf);
		KeyFactory keyFactory= KeyFactory.getInstance(KEY_ALGORITHM);  
		privateKey = (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
	}
	//加载公钥
	public static void loadPublicKey(String path) throws Exception{
		String keyStr = loadKeyFile(path);
		//log.info("keystr:{}", keyStr);
		//byte[] buf = Base64.decodeBase64(keyStr);
		byte[] buf = Base64.getDecoder().decode(keyStr);
		X509EncodedKeySpec keySpec= new X509EncodedKeySpec(buf); 
		KeyFactory keyFactory= KeyFactory.getInstance(KEY_ALGORITHM);  
		publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
	}
	private static String loadKeyFile(String path) throws Exception{
		FileReader freader = new FileReader(path);
		BufferedReader br = new BufferedReader(freader);
		StringBuffer sb = new StringBuffer();
		String line = null;
		while((line = br.readLine()) != null){
			if(line.charAt(0) == '-'){
				continue;
			}
			sb.append(line);
		}
		return sb.toString();
	}
	//公钥加密
	public static byte[] encryptByPublicKey(byte in[]){
		byte[] out = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			Cipher cipher= Cipher.getInstance(keyFactory.getAlgorithm());  
	        cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
	        out = cipher.doFinal(in);  
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
		} catch (NoSuchPaddingException e) {
			log.error("", e);
		} catch (InvalidKeyException e) {
			log.error("", e);
		} catch (IllegalBlockSizeException e) {
			log.error("", e);
		} catch (BadPaddingException e) {
			log.error("", e);
		}  
		
        return out;  
	}
	//私钥解密
	public static byte[] decryptByPrivateKey(byte[] in){
		byte[] out = null;
		try {
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			Cipher cipher= Cipher.getInstance(keyFactory.getAlgorithm());  
	        cipher.init(Cipher.DECRYPT_MODE, privateKey);  
	        out = cipher.doFinal(in);  
		} catch (NoSuchAlgorithmException e) {
			log.error("", e);
		} catch (NoSuchPaddingException e) {
			log.error("", e);
		} catch (InvalidKeyException e) {
			log.error("", e);
		} catch (IllegalBlockSizeException e) {
			log.error("", e);
		} catch (BadPaddingException e) {
			log.error("", e);
		}  
		
        return out;  
	}

	public static void main(String[] args) throws Exception {
		String path="/Users/liangbo001/work/im-root/im-lc-server/config/dev-pub";

		loadPublicKey(path);
		byte[] bytes=encryptByPublicKey(new byte[1]);
		loadPrivateKey("/Users/liangbo001/work/im-root/im-lc-server/config/dev-pri.pkcs8");
        byte[] bytes1=decryptByPrivateKey(bytes);
	}

	/**
	 * 将字节数组转为 Base64 并按 64 字符换行（符合 PEM 规范）
	 */
	private static String formatBase64(byte[] data) {
		String base64 = Base64.getEncoder().encodeToString(data);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < base64.length(); i++) {
			sb.append(base64.charAt(i));
			if ((i + 1) % 64 == 0) {
				sb.append("\n");
			}
		}
		return sb.toString();
	}
}
