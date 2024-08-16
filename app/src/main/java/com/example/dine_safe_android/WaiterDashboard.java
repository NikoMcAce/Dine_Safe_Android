package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class WaiterDashboard extends AppCompatActivity {
    public String restaurantName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_waiter_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String username = sharedPreferences.getString("username", "Username");
        String role = sharedPreferences.getString("role", "Role");

        // Set the restaurant name, role, and name
        TextView restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        TextView roleTextView = findViewById(R.id.roleTextView);
        TextView nameTextView = findViewById(R.id.nameTextView);
        restaurantNameTextView.setText(restaurantName);
        roleTextView.setText(role);
        nameTextView.setText(username); // Changed from username to name

        // Fetch the table count and populate tables
        DatabaseReference tablesRef = FirebaseDatabase.getInstance().getReference("Restaurants").child(restaurantName).child("table_count");
        tablesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Integer tableCount = dataSnapshot.getValue(Integer.class);

                if (tableCount != null && tableCount > 0) {
                    LinearLayout tablesContainer = findViewById(R.id.tablesContainer);
                    tablesContainer.removeAllViews(); // Clear any existing views

                    for (int i = 1; i <= tableCount; i++) {
                        final int tableindex =i;
                        View tableButtonView = getLayoutInflater().inflate(R.layout.table_button, tablesContainer, false);
                        TextView tableNumberTextView = tableButtonView.findViewById(R.id.tableNumberTextView);
                        tableNumberTextView.setText(tableNumberTextView.getText().toString()+String.valueOf(i)); // Set table number

                        // Set onClickListener for each table button
                        tableButtonView.setOnClickListener(v -> {
                            Toast.makeText(WaiterDashboard.this, "Table " + tableindex + " clicked", Toast.LENGTH_SHORT).show();
                            // Add code to handle table click here
                        });

                        // Add table button to the container
                        tablesContainer.addView(tableButtonView);
                    }
                } else {
                    Toast.makeText(WaiterDashboard.this, "No tables available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(WaiterDashboard.this, "Failed to retrieve table count.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}