package com.example.dine_safe_android;

import android.content.Intent;
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

import org.w3c.dom.Text;

public class ChefDashboard extends AppCompatActivity {
    private String restaurantName;
    private String username;
    private FireDetectionManager fireDetectionManager;
    private LinearLayout tableList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_chef_dashboard);

        tableList = findViewById(R.id.table_list);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");
        username = sharedPreferences.getString("username", "Username");

        TextView tvRestaurantName = findViewById(R.id.tvRestaurantName);
        TextView tvUserName = findViewById(R.id.tvUserName);
        Button btnLogoutChef = findViewById(R.id.btnLogoutChef);
        Button chefbtnyourdata = findViewById(R.id.chefbtnyourdata);
        chefbtnyourdata.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(ChefDashboard.this,UsersData.class);
                startActivity(i);

            }
        });

        btnLogoutChef.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(ChefDashboard.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        tvRestaurantName.setText(restaurantName);
        tvUserName.setText(username);

        loadOrders();

        fireDetectionManager = new FireDetectionManager(this);
        fireDetectionManager.checkForFireAndHandle(true);
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
                Toast.makeText(ChefDashboard.this, "Failed to load orders.", Toast.LENGTH_SHORT).show();
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
        Button readyButton = foodCardView.findViewById(R.id.ready_button);



        String foodName = foodSnapshot.getKey();
        Integer qty = foodSnapshot.child("qty").getValue(Integer.class);
        String status = foodSnapshot.child("status").getValue(String.class);

        // Set food item details
        foodNameTextView.setText(foodName);
        foodQtyTextView.setText("Qty: " + qty);

        if ("prepared".equals(status)||"served".equals(status)) {
            readyButton.setVisibility(View.GONE); // Hide the button if already prepared
        } else {
            readyButton.setOnClickListener(v -> markFoodAsPrepared(orderNo, foodName, foodSnapshot.getRef()));
        }

        foodList.addView(foodCardView);

    }

    private void markFoodAsPrepared(String orderNo, String foodName, DatabaseReference foodRef) {
        foodRef.child("status").setValue("prepared");
        foodRef.child("prepared_by").setValue(username);

        Toast.makeText(this, "Marked " + foodName + " as prepared", Toast.LENGTH_SHORT).show();

        // Reload orders to update the UI
        loadOrders();
    }
}