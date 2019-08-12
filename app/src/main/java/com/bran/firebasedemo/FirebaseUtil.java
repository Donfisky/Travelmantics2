package com.bran.firebasedemo;

import android.app.Activity;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

public class FirebaseUtil {
    private static final int RC_SIGN_IN = 123;
    public static FirebaseDatabase firebaseDatabase;
    public static DatabaseReference databaseReference;
    public static ArrayList<TravelDeal> deals;
    public static FirebaseAuth auth;
    public static FirebaseAuth.AuthStateListener authStateListener;
    public static FirebaseStorage firebaseStorage;
    public static StorageReference storageReference;
    private static FirebaseUtil firebaseUtil;
    private static Activity compatActivity;

    private FirebaseUtil() {
    }

    public static void openFirebasePath(Activity activity, String path) {
        if (firebaseUtil == null) {
            firebaseUtil = new FirebaseUtil();
            firebaseDatabase = FirebaseDatabase.getInstance();
            compatActivity = activity; // Sets the Parent Activity

            auth = FirebaseAuth.getInstance();
            authStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    if (firebaseAuth.getCurrentUser() == null)
                        FirebaseUtil.signIn(); // If there's no Authenticated User, call the "signIn() method"
                    Toast.makeText(compatActivity, "Welcome back!", Toast.LENGTH_SHORT).show();
                }
            };

            connectStorage();
        }

        deals = new ArrayList<>();
        databaseReference = firebaseDatabase.getReference().child(path);
    }

    public static DatabaseReference returnNewDatabase(String path) {
        if (firebaseUtil == null) {
            firebaseUtil = new FirebaseUtil();
            firebaseDatabase = FirebaseDatabase.getInstance();
            deals = new ArrayList<>();
        }

        databaseReference = firebaseDatabase.getReference().child(path);
        return databaseReference;
    }

    private static void signIn() {
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        compatActivity.startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(!BuildConfig.DEBUG, true)
                        .build(),
                RC_SIGN_IN);
    }

    private static void connectStorage() {
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference().child("traveldeals");
    }

    public static void attachListener() {
        auth.addAuthStateListener(authStateListener);
    }

    public static void detachListener() {
        auth.removeAuthStateListener(authStateListener);
    }
}
