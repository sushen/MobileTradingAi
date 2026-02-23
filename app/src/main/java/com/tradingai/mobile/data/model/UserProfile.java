package com.tradingai.mobile.data.model;

public class UserProfile {
    public String userId;
    public String email;
    public String walletAddress;
    public String affiliateCode;
    public String referredBy;
    public String role;

    public UserProfile() {
    }

    public UserProfile(String userId, String email, String walletAddress, String affiliateCode, String referredBy, String role) {
        this.userId = userId;
        this.email = email;
        this.walletAddress = walletAddress;
        this.affiliateCode = affiliateCode;
        this.referredBy = referredBy;
        this.role = role;
    }
}
