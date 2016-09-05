package pl217.mosis.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.Calendar;

import pl217.mosis.R;

public class NewEventActivity extends AppCompatActivity {

    public static long getDateFromDatePicker(DatePicker datePicker, TimePicker timePicker) {
        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();
        int hour = timePicker.getCurrentHour();

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, 0);

        return (calendar.getTimeInMillis() + calendar.getTimeZone().getOffset(calendar.getTimeInMillis()));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_event);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
        if (timePicker != null)
            timePicker.setIs24HourView(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        Button addButton = (Button) findViewById(R.id.addEventButton);
        if (addButton != null)
            addButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    EditText nameET = (EditText) findViewById(R.id.eventName);
                    String eventName = nameET.getText().toString();
                    if (eventName.equals("")) {
                        Snackbar snackbar = Snackbar.make(v, "Please enter event name", Snackbar.LENGTH_SHORT);
                        View sbView = snackbar.getView();
                        sbView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                        snackbar.show();
                        return;
                    }

                    EditText aboutET = (EditText) findViewById(R.id.eventAbout);
                    String eventAbout = aboutET.getText().toString();

                    Spinner typeSpinner = (Spinner) findViewById(R.id.spinner);
                    String eventType = typeSpinner.getSelectedItem().toString();

                    DatePicker endDP = (DatePicker) findViewById(R.id.datePicker);
                    TimePicker timePicker = (TimePicker) findViewById(R.id.timePicker);
                    long endDate = getDateFromDatePicker(endDP, timePicker);

                    Intent bundle = new Intent();
                    bundle.putExtra("name", eventName);
                    bundle.putExtra("about", eventAbout);
                    bundle.putExtra("type", eventType);
                    bundle.putExtra("deadline", endDate);

                    setResult(RESULT_OK, bundle);

                    finish();
                }
            });

        setupActionBar();
    }

    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
