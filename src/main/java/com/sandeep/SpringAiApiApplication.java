package com.sandeep;

import com.sandeep.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class SpringAiApiApplication {

	public static void main(String[] args) {
		// Use Windows' built-in certificate store so OkHttp (openai-java-client) can
		// reach api.x.ai without a PKIX / "unable to find valid certification path" error.
		// Windows-ROOT is kept up-to-date by Windows Update, so it already trusts
		// the root CA that signs api.x.ai's certificate.
		if (System.getProperty("os.name", "").toLowerCase().contains("win")) {
			System.setProperty("javax.net.ssl.trustStoreType", "Windows-ROOT");
			System.setProperty("javax.net.ssl.trustStore", "NONE");
		}
		SpringApplication.run(SpringAiApiApplication.class, args);
	}
}
