package com.autorecon.common.auth;

public interface AuthTokenResolver {
    AuthInfo resolve(String token);

    class AuthInfo {
        private Long userId;
        private Long enterpriseId;
        private String username;
        private String enterpriseName;
        private String roles;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getEnterpriseId() {
            return enterpriseId;
        }

        public void setEnterpriseId(Long enterpriseId) {
            this.enterpriseId = enterpriseId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEnterpriseName() {
            return enterpriseName;
        }

        public void setEnterpriseName(String enterpriseName) {
            this.enterpriseName = enterpriseName;
        }

        public String getRoles() {
            return roles;
        }

        public void setRoles(String roles) {
            this.roles = roles;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final AuthInfo info = new AuthInfo();

            public Builder userId(Long userId) {
                info.setUserId(userId);
                return this;
            }

            public Builder enterpriseId(Long enterpriseId) {
                info.setEnterpriseId(enterpriseId);
                return this;
            }

            public Builder username(String username) {
                info.setUsername(username);
                return this;
            }

            public Builder enterpriseName(String enterpriseName) {
                info.setEnterpriseName(enterpriseName);
                return this;
            }

            public Builder roles(String roles) {
                info.setRoles(roles);
                return this;
            }

            public AuthInfo build() {
                return info;
            }
        }
    }
}
