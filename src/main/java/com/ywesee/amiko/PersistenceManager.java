package com.ywesee.amiko;

import android.content.Context;
import android.content.Intent;


public class PersistenceManager extends Object {
    static PersistenceManager shared = null;

    static PersistenceManager getShared() {
        if (shared == null) {
            shared = new PersistenceManager();
        }
        return shared;
    }

    private PersistenceManager() {

    }

    public void loginToGoogle(Context context) {
        Intent intent = new Intent(context, GoogleOAuthActivity.class);
        context.startActivity(intent);
    }
}
