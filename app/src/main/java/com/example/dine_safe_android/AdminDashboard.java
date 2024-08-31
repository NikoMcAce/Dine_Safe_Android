package com.example.dine_safe_android;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AdminDashboard extends AppCompatActivity {
    public String restaurantName="";
    private FireDetectionManager fireDetectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retrieve restaurant name and admin username from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
         restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        String adminUsername = sharedPreferences.getString("username", "Admin Username");

        // Set restaurant name and admin username to TextViews
        TextView restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        TextView adminUsernameTextView = findViewById(R.id.adminUsernameTextView);
        restaurantNameTextView.setText(restaurantName);
        adminUsernameTextView.setText(adminUsername);

        Button logoutButton = findViewById(R.id.logoutButtonAdmin);
        Button tableCountButton = findViewById(R.id.tableCountButton);
        Button viewMenuButton = findViewById(R.id.viewMenuButton);
        Button addEmployeeButton = findViewById(R.id.addEmployeeButton);
        Button fireStatsButton = findViewById(R.id.fireStatsButton);
        Button salesButton = findViewById(R.id.salesButton);

        fireDetectionManager = new FireDetectionManager(this);
        fireDetectionManager.checkForFireAndHandle(false);

        // Set onClick listeners for buttons
        tableCountButton.setOnClickListener(v -> {
            showTableCountDialog();
        });

        viewMenuButton.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashboard.this, ViewMenu.class);
            startActivity(i);
        });

        addEmployeeButton.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashboard.this, AddEmployee.class);
            startActivity(i);
        });

        fireStatsButton.setOnClickListener(v -> {
            Intent i = new Intent (AdminDashboard.this,FireStats.class);
            startActivity(i);
        });

        salesButton.setOnClickListener(v -> {
            Intent i = new Intent(AdminDashboard.this, Sales.class);
            startActivity(i);
        });
        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear SharedPreferences
                SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();

                // Redirect to LoginActivity
                Intent intent = new Intent(AdminDashboard.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });


    }
    private void showTableCountDialog() {
        // Inflate the dialog layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_table_count, null);

        // Find the EditText in the dialog layout
        EditText tableCountEditText = dialogView.findViewById(R.id.tableCountEditText);

        // Fetch current table count from Firebase
        DatabaseReference databaseRef = FirebaseDatabase.getInstance().getReference("Restaurants/" + restaurantName + "/table_count");
        databaseRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String currentTableCount = task.getResult().getValue(String.class);
                if (currentTableCount != null) {
                    tableCountEditText.setText(currentTableCount);
                } else {
                    tableCountEditText.setText("0"); // Default value if not set
                }
            } else {
                Toast.makeText(AdminDashboard.this, "Failed to fetch data.", Toast.LENGTH_SHORT).show();
            }
        });

        // Create and show the dialog
        new AlertDialog.Builder(this)
                .setTitle("Update Table Count")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    // Get the new table count from EditText
                    String tableCountStr = tableCountEditText.getText().toString();
                    try {
                        String newTableCount = tableCountStr;
                        Log.d(TAG, "NikoMcAce table count : "+Integer.parseInt(newTableCount));
                        if (Integer.parseInt(newTableCount) > 0) {
                            // Save the new table count to Firebase
                            databaseRef.setValue(newTableCount).addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(AdminDashboard.this, "Table count updated.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(AdminDashboard.this, "Failed to update table count.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(AdminDashboard.this, "Invalid table count.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(AdminDashboard.this, "Invalid input.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }
}