package com.github.novskey.novabot.data;

import lombok.*;

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

}
