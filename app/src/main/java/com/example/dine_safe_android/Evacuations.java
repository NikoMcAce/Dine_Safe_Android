package com.example.dine_safe_android;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Evacuations extends AppCompatActivity {
    private String restaurantName,role;
    private TextView tvRestaurantName, tvPlaceOfFire;
    private ImageView imageView;

    private boolean navigation_flag=true;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_evacuations);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvPlaceOfFire = findViewById(R.id.tvPlaceOfFire);
        imageView = findViewById(R.id.imageView);

        // Get restaurant name from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        role = sharedPreferences.getString("role", "Role");


        // Set restaurant name
        tvRestaurantName.setText(restaurantName);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Sensor");

        // Monitor fire status
        monitorFireStatus();
    }

    private void monitorFireStatus() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean fireDetected = dataSnapshot.child("fire_detected").getValue(Boolean.class);
                String placeOfFire = dataSnapshot.child("place_of_fire").getValue(String.class);

                if (fireDetected != null && fireDetected) {
                    tvPlaceOfFire.setText("Place of Fire: " + placeOfFire);
                    showEvacuationImage(placeOfFire);
                } else {
                    imageView.setImageResource(0);
                    if (navigation_flag) {
                        navigation_flag=false;
                        navigateToDashboard(role);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Evacuations.this, "Failed to load fire status: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEvacuationImage(String placeOfFire) {
        if (placeOfFire != null && !placeOfFire.isEmpty()) {
            // Convert placeOfFire to lowercase and replace spaces with underscores
            String formattedPlaceOfFire = placeOfFire.toLowerCase().replace(" ", "_");

            // Find the image resource ID based on the formatted place of fire
            int imageResId = getResources().getIdentifier(formattedPlaceOfFire, "drawable", getPackageName());
            if (imageResId != 0) {
                // Set the image resource to the ImageView
                imageView.setImageResource(imageResId);
            } else {
                Toast.makeText(this, "Image not found for place: " + placeOfFire, Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void navigateToDashboard(String role) {
        Intent intent;
        switch (role) {
            case "admin":
                intent = new Intent(Evacuations.this, AdminDashboard.class);
                break;
            case "chef":
                intent = new Intent(Evacuations.this, ChefDashboard.class);
                break;
            case "waiter":
                intent = new Intent(Evacuations.this, WaiterDashboard.class);
                break;
            default:
                return;  // No valid role found, do nothing
        }
        startActivity(intent);
        finish();  // End the LoginActivity so the user can't go back to it
    }

}