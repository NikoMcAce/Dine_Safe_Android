package com.example.dine_safe_android;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FireDetectionManager {

    private static final String TAG = "FireDetectionManager";
    private DatabaseReference databaseReference;
    private String restaurantName;
    private Context context;
    private SharedPreferences sharedPreferences;  // Class-level variable for SharedPreferences

    public FireDetectionManager(Context context) {
        this.context = context;
        // Get restaurant name from SharedPreferences
        sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE); // Initialize here
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Sensor");
    }

    public void checkForFireAndHandle(Boolean show_evacuation) {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isFireDetected = false;
                String nodeName = null;

                for (DataSnapshot nodeSnapshot : dataSnapshot.getChildren()) {
                    String currentNodeName = nodeSnapshot.child("Name").getValue(String.class);
                    Double currentTemp = nodeSnapshot.child("current_temprature").getValue(Double.class);
                    Double maxTemp = nodeSnapshot.child("max_temp").getValue(Double.class);
                    Integer flameDetected = nodeSnapshot.child("int_flame_detected").getValue(Integer.class);

                    // Check if fire is detected
                    if (flameDetected != null && flameDetected == 1 && maxTemp <= currentTemp && maxTemp != null && currentTemp != null) {
                        isFireDetected = true;
                        nodeName = currentNodeName;  // Set nodeName only when fire is detected
                        break; // Exit loop once a fire is detected
                    }
                }

                boolean evacuationStarted = sharedPreferences.getBoolean("evacuation_started", false);
                if (show_evacuation && context instanceof Activity && isFireDetected && !evacuationStarted) {
                    // Mark that the evacuation activity has been started
                    sharedPreferences.edit().putBoolean("evacuation_started", true).apply();

                    Intent i = new Intent(context, Evacuations.class);
                    context.startActivity(i);
                    ((Activity) context).finish();
                    Log.d(TAG, "Evacuation activity started.");
                }

                // Update Firebase only if fire is detected
                if (isFireDetected && nodeName != null) {
                    // Get the current time
                    String fireStartedTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                    // Create a map for the updates
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fire_detected", true);
                    updates.put("fire_started_time", fireStartedTime);
                    updates.put("place_of_fire", nodeName);

                    // Update the Firebase database
                    databaseReference.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Fire detected and data updated in Firebase");
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update fire detection data: " + e.getMessage());
                    });
                } else if (!isFireDetected && evacuationStarted) {
                    // Reset the evacuation flag if the fire is no longer detected
                    sharedPreferences.edit().putBoolean("evacuation_started", false).apply();
                    Log.d(TAG, "No fire detected. Evacuation flag reset.");
                } else {
                    Log.d(TAG, "No fire detected.");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to read sensor data: " + databaseError.getMessage());
            }
        });
    }
}