package com.bran.firebasedemo;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

public class FirebaseSetup extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        /*Enable disk Persistence*/
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
