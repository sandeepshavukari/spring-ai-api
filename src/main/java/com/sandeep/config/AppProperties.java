package com.sandeep.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app")
@Data
@Validated
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private RateLimit rateLimit = new RateLimit();
    private OAuth2 oauth2 = new OAuth2();
    private Upload upload = new Upload();

    @Data
    public static class Jwt {
        private String secret;
        private long expiration;
        private long refreshExpiration;
    }

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:4200";
    }

    @Data
    public static class RateLimit {
        private int userRequestsPerDay = 50;
        private int premiumRequestsPerDay = 1000;
    }

    @Data
    public static class OAuth2 {
        private String redirectUri = "http://localhost:4200/oauth2/callback";
    }

    @Data
    public static class Upload {
        private String dir = "uploads";
    }
}
