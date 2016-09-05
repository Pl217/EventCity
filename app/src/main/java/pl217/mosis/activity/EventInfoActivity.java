package pl217.mosis.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl217.mosis.R;
import pl217.mosis.RESTful;
import pl217.mosis.model.Event;

public class EventInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_info);

        setupActionBar();

        new DownloadEventInfo().execute(getIntent());
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

    private class DownloadEventInfo extends AsyncTask<Intent, Void, Void> {

        private final ProgressDialog progressDialog = new ProgressDialog(EventInfoActivity.this);
        private Event event = new Event();

        private double latitude, longitude;

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Downloading event info");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Intent... params) {
            latitude = params[0].getDoubleExtra("latitude", 0);
            longitude = params[0].getDoubleExtra("longitude", 0);

            event = RESTful.getEventByUid(params[0].getLongExtra("uid", 0),
                    latitude, longitude);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            if (event == null) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                    }
                });
                return;
            }

            TextView name = (TextView) findViewById(R.id.event_name);
            if (name != null)
                name.setText(Html.fromHtml("<font color=#ff2e5a>" + name.getText() + "</font><br>" + event.getName()));

            TextView category = (TextView) findViewById(R.id.event_category);
            if (category != null)
                category.setText(Html.fromHtml("<font color=#ff2e5a>" + category.getText() + "</font><br>" + event.getCategory()));

            TextView about = (TextView) findViewById(R.id.event_about);
            if (about != null)
                about.setText(Html.fromHtml("<font color=#ff2e5a>" + about.getText() + "</font><br>" + event.getAbout()));

            TextView deadline = (TextView) findViewById(R.id.event_deadline);
            if (deadline != null)
                deadline.setText(Html.fromHtml("<font color=#ff2e5a>" + deadline.getText() + "</font><br>" +
                        event.getDeadline().replace('T', ' ').substring(0, event.getDeadline().indexOf('.'))));

            if (event.getDistance() > 100) {
                Button button = (Button) findViewById(R.id.event_button);
                if (button != null)
                    button.setVisibility(View.GONE);
            } else {
                Button button = (Button) findViewById(R.id.event_button);
                final SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);

                if (button != null) {
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(final View v) {
                            ExecutorService transThread = Executors.newSingleThreadExecutor();
                            transThread.submit(new Runnable() {
                                @Override
                                public void run() {
                                    if (RESTful.checkIn(sharedPref.getString(getString(R.string.stored_username), "Not provided"),
                                            getIntent().getLongExtra("uid", 0))) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                Intent data = new Intent();
                                                data.putExtra("latitude", latitude);
                                                data.putExtra("longitude", longitude);
                                                setResult(RESULT_OK, data);
                                                Toast.makeText(EventInfoActivity.this, "You are in", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        });
                                    } else {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                setResult(RESULT_CANCELED);
                                                Toast.makeText(EventInfoActivity.this, "You already checked in", Toast.LENGTH_SHORT).show();
                                                finish();
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    });
                }
            }
        }
    }
}
