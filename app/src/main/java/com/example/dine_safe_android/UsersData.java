package com.example.dine_safe_android;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import java.io.File;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class UsersData extends AppCompatActivity {
    private EditText etName, etPhoneNumber, etRole, etNewPassword;
    private Button btnDeleteAccount, btnDownloadData;
    private  FireDetectionManager fireDetectionManager;

    private String restaurantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users_data);
        fireDetectionManager = new FireDetectionManager(this);
        fireDetectionManager.checkForFireAndHandle(true);
        etName = findViewById(R.id.etName1);
        etPhoneNumber = findViewById(R.id.etPhoneNumber1);
        etRole = findViewById(R.id.etRole1);
        etNewPassword = findViewById(R.id.etNewPassword1);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount1);
        btnDownloadData = findViewById(R.id.btnDownloadData1);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        checkAndRequestPermissions();


        // Load user data
        if (username != null) {
            DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String name = dataSnapshot.child("name").getValue(String.class);
                    String phone = dataSnapshot.child("phone").getValue(String.class);
                    String role = dataSnapshot.child("role").getValue(String.class);

                    etName.setText(name);
                    etPhoneNumber.setText(phone);
                    etRole.setText(role);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(UsersData.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(UsersData.this)
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete your account?")
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (username != null) {
                                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
                                userRef.removeValue().addOnSuccessListener(aVoid -> {
                                    Toast.makeText(UsersData.this, "Account deleted successfully.", Toast.LENGTH_SHORT).show();
                                    SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.clear();
                                    editor.apply();
                                    Intent intent = new Intent(UsersData.this, LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                });
                            }
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnDownloadData.setOnClickListener(v -> {
            downloadUserData();
        });

        etNewPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (!etNewPassword.getText().toString().isEmpty()) {
                DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
                userRef.child("password").setValue(etNewPassword.getText().toString());
                Toast.makeText(UsersData.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });
    }

    private void downloadUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String username = sharedPreferences.getString("username", null);
        String restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        if (username == null || restaurantName == null) {
            Toast.makeText(this, "Invalid username or restaurant name.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a JSON object to store all the data
        JSONObject userData = new JSONObject();

        // Fetch user data
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(username);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot userSnapshot) {
                try {
                    // Add user data to the JSON object
                    for (DataSnapshot child : userSnapshot.getChildren()) {
                        userData.put(child.getKey(), child.getValue());
                    }

                    // Now fetch orders data
                    fetchOrdersData(username, restaurantName, userData);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(UsersData.this, "Error while creating JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(UsersData.this, "Failed to load user data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchOrdersData(String username, String restaurantName, JSONObject userData) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference("Restaurants")
                .child(restaurantName)
                .child("orders");

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot ordersSnapshot) {
                try {
                    JSONArray relevantOrders = new JSONArray();

                    for (DataSnapshot orderSnapshot : ordersSnapshot.getChildren()) {
                        String billedBy = orderSnapshot.child("billed_by").getValue(String.class);
                        String servedBy = orderSnapshot.child("served_by").getValue(String.class);
                        DataSnapshot foodOrderedSnapshot = orderSnapshot.child("food_ordered");

                        boolean isRelevantOrder = false;

                        if (billedBy != null && billedBy.equals(username) ||
                                servedBy != null && servedBy.equals(username)) {
                            isRelevantOrder = true;
                        } else {
                            for (DataSnapshot foodSnapshot : foodOrderedSnapshot.getChildren()) {
                                String preparedBy = foodSnapshot.child("prepared_by").getValue(String.class);
                                if (preparedBy != null && preparedBy.equals(username)) {
                                    isRelevantOrder = true;
                                    break;
                                }
                            }
                        }

                        if (isRelevantOrder) {
                            JSONObject orderJson = new JSONObject();
                            for (DataSnapshot child : orderSnapshot.getChildren()) {
                                orderJson.put(child.getKey(), child.getValue());
                            }
                            relevantOrders.put(orderJson);
                        }
                    }

                    // Add orders to userData JSON
                    userData.put("relevant_orders", relevantOrders);

                    // Now save the JSON data to a file or use it as needed
                    saveUserDataToFile(userData);

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(UsersData.this, "Error while creating JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(UsersData.this, "Failed to load orders data.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserDataToFile(JSONObject userData) {
        try {
            // Define the file name
            String fileName = "user_data_" + System.currentTimeMillis() + ".json";

            // Get the Downloads directory
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs(); // Create the directory if it doesn't exist
            }

            // Create the file in the Downloads directory
            File file = new File(downloadsDir, fileName);

            // Write JSON data to file
            FileWriter writer = new FileWriter(file);
            writer.write(userData.toString(4)); // Pretty print with an indent of 4 spaces
            writer.flush();
            writer.close();

            Toast.makeText(this, "Data exported successfully to Downloads: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error saving file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
    }
}