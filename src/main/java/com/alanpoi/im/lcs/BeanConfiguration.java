package com.alanpoi.im.lcs;


import com.alanpoi.im.lcs.util.EncryptRSA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
	private static final Logger log = LoggerFactory.getLogger(BeanConfiguration.class);
	
	@Value("${server.secp.key:}")
	private String secpKey;
	
	@Bean
	public EncryptRSA getEncryptRSA() throws Exception{
		EncryptRSA rsa = new EncryptRSA();
		rsa.loadPrivateKey(secpKey);
		
		return rsa;
	}


	
}
