package com.example.dine_safe_android;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class FireLogs extends AppCompatActivity {
    private String restaurantName;
    private LinearLayout fireLogsContainer;
    private List<View> allFireLogCards = new ArrayList<>();  // List to hold all fire log cards
    private SearchView searchView;
    private TextView tvSelectedDate;
    private String selectedDate = "";  // Store selected date in "yyyy-mm-dd" format

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fire_logs);

        fireLogsContainer = findViewById(R.id.fireLogsContainer);
        searchView = findViewById(R.id.searchView);
        Button btnDatePicker = findViewById(R.id.btnDatePicker);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        restaurantName = sharedPreferences.getString("restaurant_name", "Restaurant Name");

        loadFireLogs();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterFireLogs(query, selectedDate);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterFireLogs(newText, selectedDate);
                return false;
            }
        });

        btnDatePicker.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year1, monthOfYear, dayOfMonth) -> {
                    selectedDate = String.format("%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                    tvSelectedDate.setText("Selected Date: " + selectedDate);
                    filterFireLogs(searchView.getQuery().toString(), selectedDate);
                },
                year, month, day);

        datePickerDialog.show();
    }

    private void loadFireLogs() {
        DatabaseReference fireLogsRef = FirebaseDatabase.getInstance()
                .getReference("Restaurants")
                .child(restaurantName)
                .child("Fire_Logs");

        fireLogsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                fireLogsContainer.removeAllViews();
                allFireLogCards.clear();

                for (DataSnapshot logSnapshot : dataSnapshot.getChildren()) {
                    String placeOfFire = logSnapshot.child("place_of_fire").getValue(String.class);
                    String date = logSnapshot.child("date").getValue(String.class);
                    String fireStartedTime = logSnapshot.child("fire_started_time").getValue(String.class);
                    String timeDeactivated = logSnapshot.child("time_deactivated").getValue(String.class);

                    View fireLogCard = addFireLogCard(placeOfFire, date, fireStartedTime, timeDeactivated);
                    allFireLogCards.add(fireLogCard);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(FireLogs.this, "Failed to load fire logs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private View addFireLogCard(String placeOfFire, String date, String fireStartedTime, String timeDeactivated) {
        View fireLogCard = LayoutInflater.from(this).inflate(R.layout.fire_log_card, fireLogsContainer, false);

        TextView tvPlaceOfFire = fireLogCard.findViewById(R.id.tvPlaceOfFire);
        TextView tvDate = fireLogCard.findViewById(R.id.tvDate);
        TextView tvFireStartedTime = fireLogCard.findViewById(R.id.tvFireStartedTime);
        TextView tvTimeDeactivated = fireLogCard.findViewById(R.id.tvTimeDeactivated);

        tvPlaceOfFire.setText(placeOfFire);
        tvDate.setText("Date: " + date);
        tvFireStartedTime.setText("Fire Started Time: " + fireStartedTime);
        tvTimeDeactivated.setText("Time Deactivated: " + timeDeactivated);

        fireLogsContainer.addView(fireLogCard);
        return fireLogCard;
    }

    private void filterFireLogs(String query, String selectedDate) {
        fireLogsContainer.removeAllViews();

        for (View card : allFireLogCards) {
            TextView tvPlaceOfFire = card.findViewById(R.id.tvPlaceOfFire);
            TextView tvDate = card.findViewById(R.id.tvDate);

            String placeOfFire = tvPlaceOfFire.getText().toString().toLowerCase();
            String date = tvDate.getText().toString().replace("Date: ", "");

            boolean matchesPlace = placeOfFire.contains(query.toLowerCase());
            boolean matchesDate = TextUtils.isEmpty(selectedDate) || selectedDate.equals(date);

            if (matchesPlace && matchesDate) {
                fireLogsContainer.addView(card);
            }
        }
    }
}