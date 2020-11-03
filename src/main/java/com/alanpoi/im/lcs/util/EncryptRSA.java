package com.alanpoi.im.lcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

//RSA加密解密
public class EncryptRSA {
	private static final Logger log = LoggerFactory.getLogger(EncryptRSA.class);
	
	public static final String KEY_ALGORITHM = "RSA"; 
	
	public RSAPrivateKey privateKey;
	public RSAPublicKey publicKey;
	
	//加载私钥
	public void loadPrivateKey(String path) throws Exception{
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
	public void loadPublicKey(String path) throws Exception{
		String keyStr = loadKeyFile(path);
		//log.info("keystr:{}", keyStr);
		//byte[] buf = Base64.decodeBase64(keyStr);
		byte[] buf = Base64.getDecoder().decode(keyStr);
		X509EncodedKeySpec keySpec= new X509EncodedKeySpec(buf); 
		KeyFactory keyFactory= KeyFactory.getInstance(KEY_ALGORITHM);  
		publicKey = (RSAPublicKey) keyFactory.generatePublic(keySpec);
	}
	private String loadKeyFile(String path) throws Exception{
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
	public byte[] encryptByPublicKey(byte in[]){
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
	public byte[] decryptByPrivateKey(byte[] in){
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
}
