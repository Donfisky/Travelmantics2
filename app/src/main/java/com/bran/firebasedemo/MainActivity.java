package com.bran.firebasedemo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.data.model.Resource;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    private static final int STORAGE_REQUEST_CODE = 112;
    private Uri IMAGE_URL;
    private EditText cruiseName, cruisePrice, cruiseDescription;
    private Button selectImage;
    private ImageView cruiseImage;
    private DatabaseReference databaseReference, travelDealsRef;
    private TravelDeal deal;
    private ConstraintLayout mainLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FirebaseDatabase fbDatabase = FirebaseUtil.firebaseDatabase;
        databaseReference = FirebaseUtil.databaseReference;
        travelDealsRef = FirebaseUtil.returnNewDatabase("traveldeals");

        cruiseName = findViewById(R.id.cruise_name);
        cruisePrice = findViewById(R.id.cruise_price);
        cruiseDescription = findViewById(R.id.cruise_description);
        selectImage = findViewById(R.id.selectImage);
        cruiseImage = findViewById(R.id.imageView);
        mainLayout = findViewById(R.id.main_container);

        TravelDeal deal = (TravelDeal) getIntent().getSerializableExtra("deal");
        if (deal == null)
            deal = new TravelDeal();

        populateFields(deal);

        selectImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imageIntent();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.save:
                saveDeal();
                cleanInput();
                backToList();
                Snackbar dealSaved = Snackbar.make(mainLayout, "Deal Saved", Snackbar.LENGTH_LONG);
                dealSaved.show();
                return true;
            case R.id.logout:
                AuthUI.getInstance()
                        .signOut(this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            public void onComplete(@NonNull Task<Void> task) {
                                Log.i("logout-trout", "User Logged out successfully");
                                FirebaseUtil.attachListener(); // This will be called if the User id not Logged in
                            }
                        });
                FirebaseUtil.detachListener();

                return true;
            case R.id.delete:
                deleteDeal();
                backToList();
                Snackbar dealDeleted = Snackbar.make(mainLayout, "Deal Deleted Successfully", Snackbar.LENGTH_LONG);
                dealDeleted.show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == STORAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null && data.getData() != null) {

            Uri imageUri = data.getData();
            final StorageReference storageRef = FirebaseUtil.storageReference.child(imageUri.getLastPathSegment());

            storageRef.putFile(imageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) throw task.getException();

                    return storageRef.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull final Task<Uri> task) {
                    task.addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            IMAGE_URL = task.getResult(); // Set Current Image Url

                            String url = IMAGE_URL.toString();

                            if (deal.getImageUrl() != null)
                                deleteImage(deal.getImageUrl()); // Delete Previous Image

                            deal.setImageUrl(url);

                            displayImage(); // Display selected Image
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            final Snackbar sn = Snackbar.make(mainLayout, "Bad Internet Connection!", BaseTransientBottomBar.LENGTH_LONG);
                            sn.setAction("Okay", new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    sn.dismiss();
                                }
                            });
                            Log.i("tag", e.getMessage(), e);
                            sn.show();
                        }
                    });
                }
            });
        }
    }

    private void displayImage() {
        if (IMAGE_URL != null) {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;

            Picasso.with(this)
                    .load(IMAGE_URL)
                    .resize(width, width * 2/3)
                    .centerCrop()
                    .into(cruiseImage);
        }
    }

    private void deleteImage(String imageUrl) {
        if (imageUrl != null && !TextUtils.isEmpty(imageUrl)) {
            StorageReference picRef = FirebaseUtil.firebaseStorage.getReferenceFromUrl(imageUrl);

            picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.i("success", "File deleted");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    final Snackbar sn = Snackbar.make(mainLayout, "Bad Internet Connection!", BaseTransientBottomBar.LENGTH_LONG);
                    sn.setAction("Okay", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sn.dismiss();
                        }
                    });
                    sn.show();
                }
            });
        }
    }

    private void cleanInput() {
        cruiseName.setText("");
        cruisePrice.setText("");
        cruiseDescription.setText("");
        cruiseName.requestFocus();
    }

    private void saveDeal() {
        deal.setTitle(cruiseName.getText().toString());
        deal.setPrice(cruisePrice.getText().toString());
        deal.setDescription(cruiseDescription.getText().toString());

        if (deal.getId() == null) {
            String key = travelDealsRef.push().getKey();
            travelDealsRef.child(key).setValue(deal);
        }
        else
            travelDealsRef.child(deal.getId()).setValue(deal);
    }

    private void deleteDeal(){
        if (deal == null){
            Snackbar snackbar = Snackbar.make(mainLayout, "This deal does not exist!", Snackbar.LENGTH_LONG);
            snackbar.show();
            return;
        }

        travelDealsRef.child(deal.getId()).removeValue();
        if (deal.getImageUrl() != null)
            deleteImage(deal.getImageUrl()); // Delete Previous Image
    }

    private void backToList(){
        Intent backIntent = new Intent(this, ListActivity.class);
        startActivity(backIntent);
    }

    private void imageIntent() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        startActivityForResult(Intent.createChooser(intent, "Please Select Image"), STORAGE_REQUEST_CODE);
    }

    private void populateFields(TravelDeal deal){
        this.deal = deal;
        cruiseName.setText(deal.getTitle());
        cruisePrice.setText(deal.getPrice());
        cruiseDescription.setText(deal.getDescription());

        if (deal.getImageUrl() != null) {
            IMAGE_URL = Uri.parse(deal.getImageUrl());
            displayImage();
        }
    }
}
