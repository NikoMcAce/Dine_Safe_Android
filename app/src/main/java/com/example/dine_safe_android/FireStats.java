package com.example.dine_safe_android;

import static android.content.ContentValues.TAG;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

public class FireStats extends AppCompatActivity {

    private LinearLayout sensorContainer;
    private Button btnFireAlarmStatus;
    private DatabaseReference databaseReference;
    private String restaurantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_stats);

        sensorContainer = findViewById(R.id.sensorContainer);
        btnFireAlarmStatus = findViewById(R.id.btnFireAlarmStatus);
        TextView tvRestaurantName=findViewById(R.id.tvRestaurantName);
        TextView tvUsername = findViewById(R.id.tvUsername);


        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String adminUsername = sharedPreferences.getString("username", "Admin Username");

        tvRestaurantName.setText(restaurantName);
        tvUsername.setText(adminUsername);

        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Sensor");

        // Fetch data from Firebase
        fetchDataFromFirebase();

        // Button Click Handlers
        findViewById(R.id.btnDeactivateAlarm).setOnClickListener(view -> deactivateAlarm());
        findViewById(R.id.btnResetSensors).setOnClickListener(view -> {
            Toast.makeText(FireStats.this, "Reset Sensors button clicked", Toast.LENGTH_SHORT).show();

            // Reference to the sensors node
            DatabaseReference sensorsRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Sensor");

            sensorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    for (DataSnapshot sensorSnapshot : snapshot.getChildren()) {
                        String sensorKey = sensorSnapshot.getKey();

                        // Check if the sensor key contains the word "Node"
                        if (sensorKey != null && sensorKey.contains("Node")) {
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("current_temprature", 0);
                            updates.put("int_flame_detected", 0);

                            // Update only the specified fields for each sensor node that contains "Node"
                            sensorSnapshot.getRef().updateChildren(updates);
                        }
                    }
                    Toast.makeText(FireStats.this, "All relevant sensor nodes reset to default values.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Toast.makeText(FireStats.this, "Failed to reset sensors: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        monitorFireAlarmStatus();
    }

    private void fetchDataFromFirebase() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                sensorContainer.removeAllViews();
                boolean isFireDetected = false;
                String nodeName = null;  // Initialize as null

                for (DataSnapshot nodeSnapshot : dataSnapshot.getChildren()) {
                    String currentNodeName = nodeSnapshot.child("Name").getValue(String.class);
                    Double currentTemp = nodeSnapshot.child("current_temprature").getValue(Double.class);
                    Double maxTemp = nodeSnapshot.child("max_temp").getValue(Double.class);
                    Integer flameDetected = nodeSnapshot.child("int_flame_detected").getValue(Integer.class);

                    // Check if fire is detected
                    if (flameDetected != null && flameDetected == 1 && maxTemp <= currentTemp && maxTemp != null && currentTemp != null) {
                        isFireDetected = true;
                        nodeName = currentNodeName;  // Set nodeName only when fire is detected
                    }

                    // Ensure that sensor data is not null before creating a sensor card
                    if (currentNodeName != null && currentTemp != null && maxTemp != null && flameDetected != null) {
                        addSensorCard(currentNodeName, currentTemp, maxTemp, flameDetected);
                    }
                }

                // Update Fire Alarm Button Color and Firebase only if fire is detected
                if (isFireDetected && nodeName != null) {
                    //btnFireAlarmStatus.setBackgroundColor(Color.RED);

                    // Get the current time
                    String fireStartedTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                    // Create a map for the updates
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("fire_detected", true);
                    updates.put("fire_started_time", fireStartedTime);
                    updates.put("place_of_fire", nodeName);

                    // Update the Firebase database
                    databaseReference.updateChildren(updates).addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Fire detected and data updated in Firebase for: ");
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to update fire detection data: " + e.getMessage());
                    });
                } else {
                   // btnFireAlarmStatus.setBackgroundColor(Color.GREEN);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors.
                Toast.makeText(FireStats.this, "Failed to load data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSensorCard(String sensorName, Double currentTemp, Double maxTemp, Integer flameDetected) {
        View sensorCard = LayoutInflater.from(this).inflate(R.layout.sensor_card, sensorContainer, false);

        TextView tvSensorName = sensorCard.findViewById(R.id.tvSensorName);
        TextView tvCurrentTemp = sensorCard.findViewById(R.id.tvCurrentTemp);
        TextView tvMaxTemp = sensorCard.findViewById(R.id.tvMaxTemp);
        TextView tvFlameDetected = sensorCard.findViewById(R.id.tvFlameDetected);

        tvSensorName.setText(sensorName);
        tvCurrentTemp.setText("Current Temperature: " + currentTemp + "°C");
        tvMaxTemp.setText("Max Temperature: " + maxTemp + "°C");
        tvFlameDetected.setText("Flame Detected: " + (flameDetected == 1 ? "Yes" : "No"));

        // Set the background color based on the conditions
        if (flameDetected == 1 && currentTemp > maxTemp) {
            // If flame is detected and current temperature is greater than max temperature, set card color to red
            sensorCard.setBackgroundColor(Color.RED);
        } else if (flameDetected == 1) {
            // If only flame is detected, set card color to yellow
            sensorCard.setBackgroundColor(Color.YELLOW);
        } else {
            // No specific condition met, leave the card as it is (default background)
            sensorCard.setBackgroundColor(Color.WHITE);  // or any default color
        }

        sensorContainer.addView(sensorCard);
    }

    private void deactivateAlarm() {
        // Reference to the sensors node
        DatabaseReference sensorsRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Sensor");

        sensorsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String placeOfFire = snapshot.child("place_of_fire").getValue(String.class);
                    String fireStartedTime = snapshot.child("fire_started_time").getValue(String.class);

                    if (snapshot.child("fire_detected").getValue(Boolean.class) != null && snapshot.child("fire_detected").getValue(Boolean.class)) {
                        // Update Firebase
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("fire_detected", false);
                        updates.put("place_of_fire", "");
                        updates.put("fire_started_time", "");

                        sensorsRef.updateChildren(updates).addOnSuccessListener(aVoid -> {
                            Toast.makeText(FireStats.this, "Alarm deactivated and fire details cleared.", Toast.LENGTH_SHORT).show();
                            logFireEvent(placeOfFire, fireStartedTime);
                        }).addOnFailureListener(e -> {
                            Toast.makeText(FireStats.this, "Error deactivating alarm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    } else {
                        Toast.makeText(FireStats.this, "No active fire detected.", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FireStats.this, "Failed to read data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logFireEvent(String placeOfFire, String fireStartedTime) {
        if (placeOfFire == null || placeOfFire.isEmpty() || fireStartedTime == null || fireStartedTime.isEmpty()) {
            Toast.makeText(FireStats.this, "No fire event to log.", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference fireLogsRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Fire_Logs");

        fireLogsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                int maxFireNo = 0;

                for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                    int fireNo = Integer.parseInt(childSnapshot.getKey());
                    if (fireNo > maxFireNo) {
                        maxFireNo = fireNo;
                        Log.d(TAG, "NikoMcAce: "+maxFireNo);
                    }
                }

                int newFireNo = maxFireNo + 1;

                // Get the current date and time
                String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String timeDeactivated = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

                // Log the fire event
                DatabaseReference newFireLogRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/Fire_Logs/" + newFireNo);
                Map<String, Object> fireLogData = new HashMap<>();
                fireLogData.put("date", date);
                fireLogData.put("time_deactivated", timeDeactivated);
                fireLogData.put("place_of_fire", placeOfFire);
                fireLogData.put("fire_started_time", fireStartedTime);

                newFireLogRef.updateChildren(fireLogData).addOnSuccessListener(aVoid -> {
                    Toast.makeText(FireStats.this, "Fire event logged successfully.", Toast.LENGTH_SHORT).show();
                }).addOnFailureListener(e -> {
                    Toast.makeText(FireStats.this, "Error logging fire event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(FireStats.this, "Failed to read fire logs: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void monitorFireAlarmStatus() {
        DatabaseReference fireStatusRef = databaseReference.child("fire_detected");

        fireStatusRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Boolean isFireDetected = dataSnapshot.getValue(Boolean.class);

                if (isFireDetected != null && isFireDetected) {
                    // Fire detected, set button color to RED
                    btnFireAlarmStatus.setBackgroundColor(Color.RED);
                    btnFireAlarmStatus.setText("Fire Alarm");
                } else {
                    // No fire detected, set button color to GREEN
                    btnFireAlarmStatus.setBackgroundColor(Color.GREEN);
                    btnFireAlarmStatus.setText("Fire Alarm");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
                Toast.makeText(FireStats.this, "Failed to monitor fire status.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}