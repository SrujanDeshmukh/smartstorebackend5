package com.raghunath.smartstore.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "refresh_tokens")
public class RefreshToken {

    @Id
    private String id;
    private String email;
    private String token;

    public RefreshToken() {
        // No-arg constructor needed for MongoDB
    }

    public RefreshToken(String email, String token) {
        this.email = email;
        this.token = token;
    }

    //  Getters
    public String getEmail() {
        return email;
    }

    public String getToken() {
        return token;
    }

    //  Setters
    public void setEmail(String email) {
        this.email = email;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
