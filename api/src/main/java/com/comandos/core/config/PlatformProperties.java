package com.comandos.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "platform")
public class PlatformProperties {
    private String allowedOrigin = "http://localhost:3000";
    private final Security security = new Security();

    public String getAllowedOrigin() {
        return allowedOrigin;
    }

    public void setAllowedOrigin(String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    public Security getSecurity() {
        return security;
    }

    public static class Security {
        private boolean requireLogin;
        private boolean enforcePermissions;

        public boolean isRequireLogin() {
            return requireLogin;
        }

        public void setRequireLogin(boolean requireLogin) {
            this.requireLogin = requireLogin;
        }

        public boolean isEnforcePermissions() {
            return enforcePermissions;
        }

        public void setEnforcePermissions(boolean enforcePermissions) {
            this.enforcePermissions = enforcePermissions;
        }
    }
}
