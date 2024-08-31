package com.example.dine_safe_android;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

public class Waiter_Tables extends AppCompatActivity {
    public String restaurantName = "";
    String username="";

    private FireDetectionManager fireDetectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_waiter_tables);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        username = sharedPreferences.getString("username", "Username");
        String role = sharedPreferences.getString("role", "Role");

        // Set the restaurant name, role, and name
        TextView restaurantNameTextView = findViewById(R.id.restaurantNameTextView);
        TextView nameTextView = findViewById(R.id.nameTextView);
        restaurantNameTextView.setText(restaurantName);

        nameTextView.setText(username); // Changed from username to name

        // Fetch the table count and populate tables
        DatabaseReference tablesRef = FirebaseDatabase.getInstance().getReference("Restaurants").child(restaurantName).child("table_count");
        tablesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Object tableCountObj = dataSnapshot.getValue();
                Integer tableCount = null;

                if (tableCountObj instanceof Long) {
                    tableCount = ((Long) tableCountObj).intValue();
                } else if (tableCountObj instanceof String) {
                    try {
                        tableCount = Integer.parseInt((String) tableCountObj);
                    } catch (NumberFormatException e) {
                        Toast.makeText(Waiter_Tables.this, "Invalid table count format.", Toast.LENGTH_SHORT).show();
                    }
                }

                if (tableCount != null && tableCount > 0) {
                    LinearLayout tablesContainer = findViewById(R.id.tablesContainer);
                    tablesContainer.removeAllViews(); // Clear any existing views

                    for (int i = 1; i <= tableCount; i++) {
                        final int tableIndex = i;
                        View tableButtonView = getLayoutInflater().inflate(R.layout.table_button, tablesContainer, false);
                        TextView tableNumberTextView = tableButtonView.findViewById(R.id.tableNumberTextView);
                        TextView tableStatusTextView = tableButtonView.findViewById(R.id.tableStatus);
                        tableNumberTextView.setText("Table number : " + tableIndex); // Set table number

                        // Check table status
                        checkTableStatus(tableIndex, tableStatusTextView);

                        // Set onClickListener for each table button
                        tableButtonView.setOnClickListener(v -> {
                            String status = tableStatusTextView.getText().toString();
                            // Split the string into an array of words
                            String[] words = status.split("\\s+");
                            status = words[words.length - 1];

                            Toast.makeText(Waiter_Tables.this, "Table " + tableIndex  +" "+ status, Toast.LENGTH_SHORT).show();

                            if (status.contains("Occupied")) {
                                showMenu(tableIndex);
                            } else {
                                assignTable(tableIndex);
                            }
                        });

                        // Add table button to the container
                        tablesContainer.addView(tableButtonView);
                    }
                } else {
                    Toast.makeText(Waiter_Tables.this, "No tables available.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Waiter_Tables.this, "Failed to retrieve table count.", Toast.LENGTH_SHORT).show();
            }
        });

        fireDetectionManager = new FireDetectionManager(this);
        fireDetectionManager.checkForFireAndHandle(true);
    }

    private void checkTableStatus(int tableIndex, TextView tableStatusTextView) {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance()
                .getReference("Restaurants")
                .child(restaurantName)
                .child("orders");

        ordersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean isOccupied = false;

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    Integer tableNo = orderSnapshot.child("table_no").getValue(Integer.class);
                    String status = orderSnapshot.child("status").getValue(String.class);

                    if (tableNo != null && status != null && tableNo == tableIndex && status.equalsIgnoreCase("occupied")) {
                        isOccupied = true;
                        break;
                    }
                }

                if (isOccupied) {
                    tableStatusTextView.setText("Status: Occupied");
                } else {
                    tableStatusTextView.setText("Status: Empty");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(Waiter_Tables.this, "Failed to retrieve table status.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Function to be called when the table is occupied
    private void showMenu(int tableIndex) {

        Toast.makeText(Waiter_Tables.this, "Showing menu for Table " + tableIndex, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Waiter_Tables.this, MakeOrder.class);
        intent.putExtra("TABLE_INDEX", tableIndex);
        startActivity(intent);
    }

    // Function to be called when the table is empty
    private void assignTable(int tableIndex) {
        // Create a dialog for assigning the table
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_assign_table); // Assuming you have a custom layout named dialog_assign_table
        dialog.setTitle("Assign Table " + tableIndex);

        // Get the elements from the dialog
        TextView numberOfCustomersTextView = dialog.findViewById(R.id.numberOfCustomers);
        Button plusButton = dialog.findViewById(R.id.plusButton);
        Button minusButton = dialog.findViewById(R.id.minusButton);
        Button assignButton = dialog.findViewById(R.id.assignButton);

        // Initialize the number of customers
        int[] numberOfCustomers = {1}; // Starting with 1 customer
        numberOfCustomersTextView.setText(String.valueOf(numberOfCustomers[0]));

        // Set up the plus button to increase the number of customers
        plusButton.setOnClickListener(v -> {
            numberOfCustomers[0]++;
            numberOfCustomersTextView.setText(String.valueOf(numberOfCustomers[0]));
        });

        // Set up the minus button to decrease the number of customers
        minusButton.setOnClickListener(v -> {
            if (numberOfCustomers[0] > 1) {
                numberOfCustomers[0]--;
                numberOfCustomersTextView.setText(String.valueOf(numberOfCustomers[0]));
            }
        });

        // Handle the assignment of the table
        assignButton.setOnClickListener(v -> {
            // Fetch the maximum order number and assign the table
            DatabaseReference maxOrderRef = FirebaseDatabase.getInstance()
                    .getReference("Restaurants")
                    .child(restaurantName)
                    .child("maxorderno");

            maxOrderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Integer maxOrderNo = dataSnapshot.getValue(Integer.class);
                    if (maxOrderNo == null) {
                        maxOrderNo = 0; // If maxOrderNo doesn't exist, start with 0
                    }

                    int newOrderNo = maxOrderNo + 1;

                    // Create a new order entry in the database
                    DatabaseReference newOrderRef = FirebaseDatabase.getInstance()
                            .getReference("Restaurants")
                            .child(restaurantName)
                            .child("orders")
                            .child(String.valueOf(newOrderNo));

                    newOrderRef.setValue(new OrderDetails(String.valueOf(numberOfCustomers[0]), username, "occupied", tableIndex, 0));

                    // Update the max order number in the database
                    maxOrderRef.setValue(newOrderNo);

                    Toast.makeText(Waiter_Tables.this, "Table " + tableIndex + " assigned with order no " + newOrderNo, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(Waiter_Tables.this, "Failed to assign table.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}

