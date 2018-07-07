package com.github.novskey.novabot.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Created by Paris on 17/01/2018.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class User {

    @NonNull
    String userID;
    boolean paused = false;
    String botToken;
    boolean verified = false;

}
