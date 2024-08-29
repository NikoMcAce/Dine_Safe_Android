package com.example.dine_safe_android;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
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

public class OrderStatus extends AppCompatActivity {
    private String restaurantName;
    private String username;
    private LinearLayout tableList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_order_status);

        tableList = findViewById(R.id.table_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        username = sharedPreferences.getString("username", "Username");

        loadOrders();
    }

    private void loadOrders() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("Restaurants")
                .child(restaurantName)
                .child("orders");

        ordersRef.orderByChild("status").equalTo("occupied").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                tableList.removeAllViews(); // Clear previous views

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    if (orderSnapshot.hasChild("food_ordered")) {
                        addTableCard(orderSnapshot);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(OrderStatus.this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addTableCard(DataSnapshot orderSnapshot) {
        // Inflate the layout for each table card
        View tableCardView = LayoutInflater.from(this).inflate(R.layout.table_card, tableList, false);

        TextView tableNumberTextView = tableCardView.findViewById(R.id.table_number);
        LinearLayout foodList = tableCardView.findViewById(R.id.food_list);

        // Set table number
        Integer tableNo = orderSnapshot.child("table_no").getValue(Integer.class);
        tableNumberTextView.setText("Table " + tableNo);

        // Add food items to the card
        for (DataSnapshot foodSnapshot : orderSnapshot.child("food_ordered").getChildren()) {
            addFoodCard(foodList, orderSnapshot.getKey(), foodSnapshot);
        }

        tableList.addView(tableCardView);
    }

    private void addFoodCard(LinearLayout foodList, String orderNo, DataSnapshot foodSnapshot) {
        // Inflate the layout for each food item card
        View foodCardView = LayoutInflater.from(this).inflate(R.layout.food_item_card, foodList, false);

        TextView foodNameTextView = foodCardView.findViewById(R.id.food_name);
        TextView foodQtyTextView = foodCardView.findViewById(R.id.food_qty);
        Button serveButton = foodCardView.findViewById(R.id.ready_button);

        String foodName = foodSnapshot.getKey();
        Integer qty = foodSnapshot.child("qty").getValue(Integer.class);
        String status = foodSnapshot.child("status").getValue(String.class);

        // Set food item details
        //foodNameTextView.setText(foodName);
        foodNameTextView.setText(foodName + " - " + (status != null ? status : "Preparing"));

        foodQtyTextView.setText("Qty: " + qty);

        // Only show the "Serve" button if the status is "prepared"
        if ("prepared".equals(status)) {
            serveButton.setText("Serve");
            serveButton.setVisibility(View.VISIBLE); // Ensure the button is visible
            serveButton.setOnClickListener(v -> markFoodAsServed(orderNo, foodName, foodSnapshot.getRef()));
        } else {
            serveButton.setVisibility(View.GONE); // Hide the button if status is not "prepared"
        }

        foodList.addView(foodCardView);
    }

    private void markFoodAsServed(String orderNo, String foodName, DatabaseReference foodRef) {
        foodRef.child("status").setValue("served");
        foodRef.child("served_by").setValue(username);

        Toast.makeText(this, "Marked " + foodName + " as served", Toast.LENGTH_SHORT).show();

        // Reload orders to update the UI
        loadOrders();
    }
}