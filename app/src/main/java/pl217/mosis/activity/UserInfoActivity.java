package pl217.mosis.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import pl217.mosis.R;
import pl217.mosis.RESTful;
import pl217.mosis.model.User;

public class UserInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        final Button callButton = (Button) findViewById(R.id.call_button);
        if (callButton != null)
            callButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent call = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + callButton.getText().toString()));
                    startActivity(call);
                }
            });

        new DownloadUserInfo().execute(getIntent().getStringExtra("username"));
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

    private class DownloadUserInfo extends AsyncTask<String, Void, Void> {

        private final ProgressDialog progressDialog = new ProgressDialog(UserInfoActivity.this);
        private User user = new User();
        private Bitmap img = null;

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Downloading user rankings");
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(String... params) {
            user = RESTful.getUser(params[0]);
            img = RESTful.myImage(user.getUsername());

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            ImageView imageView = (ImageView) findViewById(R.id.user_info_image);
            if (imageView != null)
                imageView.setImageBitmap(img);

            TextView username = (TextView) findViewById(R.id.user_info_name);
            if (username != null)
                username.setText("Username:  " + user.getUsername() + "\n\nFull name:  " + user.getFullName());

            TextView points = (TextView) findViewById(R.id.user_info_points);
            if (points != null)
                points.setText("Collected points:\n" + user.getPoints() + " points");

            Button callButton = (Button) findViewById(R.id.call_button);
            if (callButton != null)
                callButton.setText(user.getPhone());
        }
    }

}
