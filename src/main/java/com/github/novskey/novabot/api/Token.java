package com.github.novskey.novabot.api;

import java.util.Date;

public class Token {

    private String token;
    private String userId;
    private Date validDate;

    public Token(String token, String userId, Date validDate) {
        this.token = token;
        this.userId = userId;
        this.validDate = validDate;
    }

    public String getToken() {
        return token;
    }

    public String getUserId() {
        return userId;
    }

    public boolean isValid() {
        return new Date().getTime() <= validDate.getTime();
    }
}
