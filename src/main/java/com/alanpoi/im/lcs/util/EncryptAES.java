package com.alanpoi.im.lcs.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class EncryptAES {
	private static final Logger log = LoggerFactory.getLogger(EncryptAES.class);
	
	private static final String KEY_ALGORITHM = "AES";
	private static final String MODE = "AES/CBC/PKCS5Padding";
	private static final byte[] iv = new byte[16];

	public static byte[] encrypt128CBCPKCS5Padding(byte[] in, String pwd){
		byte[] out = null;
		try {
			SecretKeySpec key = new SecretKeySpec(pwd.getBytes(), KEY_ALGORITHM);

			Cipher cipher = Cipher.getInstance(MODE);

			//StringUtil.getRandomString(16).getBytes();
			cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
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
		} catch (InvalidAlgorithmParameterException e) {
			log.error("", e);
		} catch (Exception e){
			log.error("", e);
		}
		return out;
	}
	
	public static byte[] decrypt128CBCPKCS5Padding(byte[] in, String pwd){

		byte[] out = null;
		try {
			SecretKeySpec key = new SecretKeySpec(pwd.getBytes(), KEY_ALGORITHM);  
			Cipher cipher = Cipher.getInstance(MODE);

			cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
			out = cipher.doFinal(in);
			//log.info("aces decrypt out len:{}", out.length);
			//log.info("pad:{}", (int)(out[out.length - 1] & 0xff));
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
		} catch (Exception e) {
			log.error("", e);
		}
		return out;
	}
	
}
