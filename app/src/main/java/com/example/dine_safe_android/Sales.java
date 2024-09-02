package com.example.dine_safe_android;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Sales extends AppCompatActivity {

    private String restaurantName;
    private TextView tvTotalSales, tvDishesToday, tvTopDishes, tvChefAnalytics, tvWaiterTableAnalytics, tvWaiterBillingAnalytics, tvTableIncome,tvRestaurantName;
    private Spinner spinnerChefs, spinnerWaiters, spinnerBillers, spinnerTables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sales);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        Button adminbtnOrderHistory = findViewById(R.id.adminbtnOrderHistory);
        adminbtnOrderHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent (Sales.this,OrderHistoryActivity.class);
                startActivity(i);
            }
        });

        // Initialize views
        tvRestaurantName = findViewById(R.id.tvRestaurantName);
        tvRestaurantName.setText(restaurantName);
        tvTotalSales = findViewById(R.id.tvTotalSales);
        tvDishesToday = findViewById(R.id.tvDishesToday);
        tvTopDishes = findViewById(R.id.tvTopDishes);
        tvChefAnalytics = findViewById(R.id.tvChefAnalytics);
        tvWaiterTableAnalytics = findViewById(R.id.tvWaiterTableAnalytics);
        tvWaiterBillingAnalytics = findViewById(R.id.tvWaiterBillingAnalytics);
        tvTableIncome = findViewById(R.id.tvTableIncome);
        spinnerChefs = findViewById(R.id.spinnerChefs);
        spinnerWaiters = findViewById(R.id.spinnerWaiters);
        spinnerBillers = findViewById(R.id.spinnerBillers);
        spinnerTables = findViewById(R.id.spinnerTables);

        loadSalesData();
    }

    private void loadSalesData() {
        DatabaseReference ordersRef = FirebaseDatabase.getInstance().getReference()
                .child("Restaurants")
                .child(restaurantName)
                .child("orders");

        ordersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                double totalSales = 0.0;
                int dishesToday = 0;
                Map<String, Integer> chefAnalytics = new HashMap<>();
                Map<String, Integer> waiterTableAnalytics = new HashMap<>();
                Map<String, Double> waiterBillingAnalytics = new HashMap<>();
                Map<String, Integer> dishCounts = new HashMap<>();
                Map<Integer, Double> tableIncome = new HashMap<>();

                String today = new java.text.SimpleDateFormat("dd/MM/yyyy").format(new java.util.Date());
                //Log.d(TAG, "NikoMcAce today date "+ today);

                for (DataSnapshot orderSnapshot : dataSnapshot.getChildren()) {
                    double orderTotal = orderSnapshot.child("total_price").getValue(Double.class);
                    String billedDate = orderSnapshot.child("billed_date").getValue(String.class);
                    String billedBy = orderSnapshot.child("billed_by").getValue(String.class);
                    Integer tableNo = orderSnapshot.child("table_no").getValue(Integer.class);

                    totalSales += orderTotal;

                    if (today.equals(billedDate)) {
                        dishesToday += orderSnapshot.child("food_ordered").getChildrenCount();
                    }

                    // Chef and waiter analytics
                    for (DataSnapshot foodSnapshot : orderSnapshot.child("food_ordered").getChildren()) {
                        String preparedBy = foodSnapshot.child("prepared_by").getValue(String.class);
                        String servedBy = foodSnapshot.child("served_by").getValue(String.class);
                        String foodName = foodSnapshot.getKey();
                        int qty = foodSnapshot.child("qty").getValue(Integer.class);

                        if (preparedBy != null) {
                            chefAnalytics.put(preparedBy, chefAnalytics.getOrDefault(preparedBy, 0) + qty);
                        }

                        if (servedBy != null) {
                            waiterTableAnalytics.put(servedBy, waiterTableAnalytics.getOrDefault(servedBy, 0) + 1);
                        }

                        if (foodName != null) {
                            dishCounts.put(foodName, dishCounts.getOrDefault(foodName, 0) + qty);
                        }
                    }

                    if (billedBy != null) {
                        waiterBillingAnalytics.put(billedBy, waiterBillingAnalytics.getOrDefault(billedBy, 0.0) + orderTotal);
                    }

                    if (tableNo != null) {
                        tableIncome.put(tableNo, tableIncome.getOrDefault(tableNo, 0.0) + orderTotal);
                    }
                }

                // Update the views
                tvTotalSales.setText(String.format("$%.2f", totalSales));
                tvDishesToday.setText(String.valueOf(dishesToday));

                List<Map.Entry<String, Integer>> topDishes = new ArrayList<>(dishCounts.entrySet());
                topDishes.sort((a, b) -> b.getValue() - a.getValue());
                tvTopDishes.setText(topDishes.stream().limit(3).map(e -> e.getKey() + " (" + e.getValue() + ")").reduce((a, b) -> a + ", " + b).orElse("None"));

                setupSpinner(spinnerChefs, chefAnalytics, tvChefAnalytics, "Dishes cooked: ");
                setupSpinner(spinnerWaiters, waiterTableAnalytics, tvWaiterTableAnalytics, "Total tables: ");
                setupSpinner(spinnerBillers, waiterBillingAnalytics, tvWaiterBillingAnalytics, "Total billed: $");

                setupTableSpinner(spinnerTables, tableIncome, tvTableIncome);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle possible errors
            }
        });
    }

    private void setupSpinner(Spinner spinner, Map<String, ?> analyticsMap, TextView outputView, String prefix) {
        List<String> keys = new ArrayList<>(analyticsMap.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, keys);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedKey = keys.get(position);
                Object value = analyticsMap.get(selectedKey);
                outputView.setText(prefix + value);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                outputView.setText(prefix + "0");
            }
        });
    }

    private void setupTableSpinner(Spinner spinner, Map<Integer, Double> tableIncomeMap, TextView outputView) {
        List<String> keys = new ArrayList<>();
        for (Integer key : tableIncomeMap.keySet()) {
            keys.add("Table " + key);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, keys);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedKey = keys.get(position);
                Integer tableNo = Integer.parseInt(selectedKey.split(" ")[1]);
                Double income = tableIncomeMap.get(tableNo);
                outputView.setText("Total income: $" + String.format("%.2f", income));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                outputView.setText("Total income: $0.00");
            }
        });
    }
}