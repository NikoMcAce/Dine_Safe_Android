package com.example.dine_safe_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class WaiterDashboard extends AppCompatActivity {
    public String restaurantName = "";
    private TextView tvRestaurantName, tvUserName;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_waiter_dashboard);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String username = sharedPreferences.getString("username", "Username");
        String role = sharedPreferences.getString("role", "Role");

        Button btnTables = findViewById(R.id.btnTables);
        Button btnOrderStatus = findViewById(R.id.btnOrderStatus);
        Button btnYourData = findViewById(R.id.btnYourData);
        Button btnLogout = findViewById(R.id.btnLogout);
        Button btnOrderHistory, btnComplaints;

        // Inside onCreate() method
        btnOrderHistory = findViewById(R.id.btnOrderHistory);
        btnComplaints = findViewById(R.id.btnComplaints);

        btnTables.setOnClickListener(v -> startActivity(new Intent(WaiterDashboard.this, Waiter_Tables.class)));
        btnOrderStatus.setOnClickListener(v -> startActivity(new Intent(WaiterDashboard.this, OrderStatus.class)));
        btnYourData.setOnClickListener(v -> startActivity(new Intent(WaiterDashboard.this, UsersData.class)));

        btnLogout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(WaiterDashboard.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });


        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvUserName = findViewById(R.id.tvUserName);

        tvRestaurantName.setText(restaurantName); // Set restaurant name from SharedPreferences
        fetchAndDisplayUserName(username, sharedPreferences); // Fetch and display user's name

        // Navigate to OrderHistoryActivity
        btnOrderHistory.setOnClickListener(v -> {
            Intent intent = new Intent(WaiterDashboard.this, OrderHistoryActivity.class);
            startActivity(intent);
        });

// Navigate to ComplaintsActivity
        btnComplaints.setOnClickListener(v -> {
            Intent intent = new Intent(WaiterDashboard.this, ComplaintsActivity.class);
            startActivity(intent);
        });
    }
    private void fetchAndDisplayUserName(String username, SharedPreferences sharedPreferences) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.child("name").getValue(String.class);
                if (name != null) {
                    tvUserName.setText(name);
                    // Update SharedPreferences with the user's name for future use
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("name", name);
                    editor.apply();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(WaiterDashboard.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}