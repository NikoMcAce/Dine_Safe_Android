package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ComplaintsActivity extends AppCompatActivity {
    private String restaurantName, username;
    private Spinner orderSpinner;
    private LinearLayout orderedItemsContainer;
    private EditText complaintText;
    private Button submitComplaintButton;
    private DatabaseReference ordersRef, complaintsRef;
    private Map<String, Object> returnedItems = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_complaints);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        orderSpinner = findViewById(R.id.order_spinner);
        orderedItemsContainer = findViewById(R.id.ordered_items_container);
        complaintText = findViewById(R.id.complaint_text);
        submitComplaintButton = findViewById(R.id.submit_complaint_button);

        // Get restaurant name and username from SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        username = sharedPreferences.getString("username", "Username");

        // Initialize Firebase references
        ordersRef = FirebaseDatabase.getInstance().getReference("Restaurants").child(restaurantName).child("orders");
        complaintsRef = FirebaseDatabase.getInstance().getReference("Restaurants").child(restaurantName).child("Complaints");

        // Load order numbers into spinner
        loadOrderNumbers();

        // Handle order selection
        orderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedOrderNo = (String) orderSpinner.getSelectedItem();
                if (selectedOrderNo != null && !selectedOrderNo.isEmpty()) {
                    loadOrderedItems(selectedOrderNo);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Handle case when nothing is selected (if needed)
            }
        });

        // Handle complaint submission
        submitComplaintButton.setOnClickListener(v -> submitComplaint());
    }

    private void loadOrderNumbers() {
        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<String> orderNumbers = new ArrayList<>();
                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    String orderNo = orderSnapshot.getKey();
                    if (orderNo != null) {
                        orderNumbers.add(orderNo);
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(ComplaintsActivity.this, android.R.layout.simple_spinner_item, orderNumbers);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                orderSpinner.setAdapter(adapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ComplaintsActivity.this, "Failed to load order numbers.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadOrderedItems(String orderNo) {
        ordersRef.child(orderNo).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                orderedItemsContainer.removeAllViews(); // Clear previous items
                returnedItems.clear();

                DataSnapshot foodOrderedSnapshot = dataSnapshot.child("food_ordered");
                for (DataSnapshot foodItemSnapshot : foodOrderedSnapshot.getChildren()) {
                    String foodName = foodItemSnapshot.getKey();
                    int qty = foodItemSnapshot.child("qty").getValue(Integer.class);
                    double unitPrice = foodItemSnapshot.child("unit_price").getValue(Double.class);

                    addOrderedItemCard(foodName, qty, unitPrice);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ComplaintsActivity.this, "Failed to load ordered items.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addOrderedItemCard(String foodName, int qty, double unitPrice) {
        View itemCard = getLayoutInflater().inflate(R.layout.ordered_item_card, orderedItemsContainer, false);

        TextView foodNameTextView = itemCard.findViewById(R.id.food_name);
        TextView foodQtyTextView = itemCard.findViewById(R.id.food_qty);
        TextView unitPriceTextView = itemCard.findViewById(R.id.unit_price);
        CheckBox returnedCheckBox = itemCard.findViewById(R.id.returned_checkbox);

        foodNameTextView.setText(foodName);
        foodQtyTextView.setText("Qty: " + qty);
        unitPriceTextView.setText("£" + unitPrice);

        returnedCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                returnedItems.put(foodName, new OrderItem(foodName, qty, String.valueOf(unitPrice)));
            } else {
                returnedItems.remove(foodName);
            }
        });

        orderedItemsContainer.addView(itemCard);
    }

    private void submitComplaint() {
        String selectedOrderNo = (String) orderSpinner.getSelectedItem();
        String complaint = complaintText.getText().toString().trim();

        if (selectedOrderNo == null || selectedOrderNo.isEmpty()) {
            Toast.makeText(this, "Please select an order number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (complaint.isEmpty()) {
            Toast.makeText(this, "Please write a complaint.", Toast.LENGTH_SHORT).show();
            return;
        }

        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

        Map<String, Object> complaintData = new HashMap<>();
        complaintData.put("raised_by", username);
        complaintData.put("complain", complaint);
        complaintData.put("raised_on", currentDate);
        complaintData.put("raised_at", currentTime);
        complaintData.put("order_no", selectedOrderNo);

        // Add returned items to complaint
        Map<String, Object> foodReturnedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : returnedItems.entrySet()) {
            OrderItem orderItem = (OrderItem) entry.getValue();
            Map<String, Object> itemData = new HashMap<>();
            itemData.put("qty", orderItem.getQuantity());
            itemData.put("unit_price", orderItem.getPrice());
            foodReturnedData.put(orderItem.getName(), itemData);
        }
        complaintData.put("food_returned", foodReturnedData);

        // Submit complaint to Firebase
        complaintsRef.push().setValue(complaintData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ComplaintsActivity.this, "Complaint submitted successfully.", Toast.LENGTH_SHORT).show();
                    complaintText.setText(""); // Clear the text area
                    returnedItems.clear(); // Clear the returned items map
                    orderedItemsContainer.removeAllViews(); // Clear the ordered items
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(ComplaintsActivity.this, "Failed to submit complaint: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}