package pl217.mosis.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl217.mosis.R;
import pl217.mosis.RESTful;

public class LoginActivity extends AppCompatActivity {

    private static final String WRONG_PASSWORD_MESSAGE = "Wrong password";
    private static final String WRONG_USERNAME_MESSAGE = "No such user";

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;

    private Handler guiThread;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.
        mUsernameView = (EditText) findViewById(R.id.username);

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    ExecutorService transThread = Executors.newSingleThreadExecutor();
                    transThread.submit(new Runnable() {
                        @Override
                        public void run() {
                            guiStartProgressDialog("Connecting", "Logging in");

                            attemptLogin();

                            guiDissmissProgressDialog();
                        }
                    });
                    return true;
                }
                return false;
            }
        });

        guiThread = new Handler();
        progressDialog = new ProgressDialog(this);

        SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean(getString(R.string.stored_download), false);
        editor.commit();

        Button mUsernameSignInButton = (Button) findViewById(R.id.sign_in_button);
        if (mUsernameSignInButton != null)
            mUsernameSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ExecutorService transThread = Executors.newSingleThreadExecutor();
                    transThread.submit(new Runnable() {
                        @Override
                        public void run() {
                            guiStartProgressDialog("Connecting", "Logging in");

                            attemptLogin();

                            guiDissmissProgressDialog();
                        }
                    });
                }
            });

        TextView mCreateAccount = (TextView) findViewById(R.id.link_signup);
        if (mCreateAccount != null)
            mCreateAccount.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
                    startActivity(intent);
                    finish();
                }
            });
    }

    private void attemptLogin() {

        String username = mUsernameView.getText().toString();
        String password = mPasswordView.getText().toString();

        if (RESTful.attemptLogin(username, password)) {
            Intent mainActivity = new Intent(LoginActivity.this, MainActivity.class);
            mainActivity.putExtra("username", username);
            startActivity(mainActivity);
            finish();
        } else {
            guiNotifyUser(RESTful.RESPONSE_STRING);
        }
    }

    private void guiNotifyUser(final String message) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                mUsernameView.setError(null);
                mPasswordView.setError(null);

                if (message.equals(WRONG_USERNAME_MESSAGE)) {
                    mUsernameView.setError(message);
                    mUsernameView.requestFocus();
                } else if (message.equals(WRONG_PASSWORD_MESSAGE)) {
                    mPasswordView.setError(message);
                    mPasswordView.requestFocus();
                } else {
                    Snackbar snackbar = Snackbar.make(findViewById(R.id.sign_in_button), "Login failed!", Snackbar.LENGTH_LONG);
                    View view = snackbar.getView();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                    } else {
                        view.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                    }
                    snackbar.show();
                }
            }
        });
    }

    private void guiStartProgressDialog(final String title, final String message) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.setTitle(title);
                progressDialog.setMessage(message);
                progressDialog.show();
            }
        });
    }

    private void guiDissmissProgressDialog() {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                progressDialog.dismiss();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.ip_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.ip_menu) {
            startActivity(new Intent(this, IPAddressActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }
}

