package pl217.mosis.activity;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pl217.mosis.R;
import pl217.mosis.RESTful;

public class SignupActivity extends AppCompatActivity {

    private static final int CAPTURE_IMAGE = 2;
    private Handler guiThread;
    private ProgressDialog progressDialog;
    // UI references.
    private EditText mNameView;
    private EditText mUsernameView;
    private EditText mPasswordView;
    private EditText mPhoneView;
    private Bitmap profileImage = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mNameView = (EditText) findViewById(R.id.name);
        mUsernameView = (EditText) findViewById(R.id.username);
        mPasswordView = (EditText) findViewById(R.id.password);
        mPhoneView = (EditText) findViewById(R.id.phonenumber);

        guiThread = new Handler();
        progressDialog = new ProgressDialog(this);

        // Reset errors.
        mNameView.setError(null);
        mUsernameView.setError(null);
        mPasswordView.setError(null);
        mPhoneView.setError(null);

        if (savedInstanceState != null) profileImage = savedInstanceState.getParcelable("image");

        Button mSignUpButton = (Button) findViewById(R.id.sign_up_button);
        if (mSignUpButton != null)
            mSignUpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ExecutorService transThread = Executors.newSingleThreadExecutor();
                    transThread.submit(new Runnable() {
                        @Override
                        public void run() {
                            guiStartProgressDialog("Connecting", "Signing up");

                            String name = mNameView.getText().toString();
                            String username = mUsernameView.getText().toString();
                            String password = mPasswordView.getText().toString();
                            String phone = mPhoneView.getText().toString();
                            attemptSignUp(name, username, password, phone);

                            guiDissmissProgressDialog();
                        }
                    });
                }
            });

        TextView mLoginLink = (TextView) findViewById(R.id.link_signup);
        if (mLoginLink != null)
            mLoginLink.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            });

        Button mTakePicture = (Button) findViewById(R.id.take_picture_button);
        if (mTakePicture != null)
            mTakePicture.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, CAPTURE_IMAGE);
                }
            });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("image", profileImage);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_IMAGE && resultCode == RESULT_OK) {
            profileImage = (Bitmap) data.getExtras().get("data");
            showImage(profileImage);

            profileImage = ThumbnailUtils.extractThumbnail(profileImage, 96, 96);
        }
    }

    private void setError(final EditText editText, final String message) {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                editText.setError(message);
            }
        });
    }

    private void attemptSignUp(String name, String username, String password, String phone) {
        if (mNameView.getText().toString().length() < 3) {
            setError(mNameView, "Name too short");
            guiDissmissProgressDialog();
            return;
        }
        if (!StringUtils.isAlphanumeric(mUsernameView.getText().toString())) {
            setError(mUsernameView, "Username not alphanumeric");
            guiDissmissProgressDialog();
            return;
        }
        if (!StringUtils.isAlphanumeric(mPasswordView.getText().toString())) {
            setError(mPasswordView, "Password not alphanumeric");
            guiDissmissProgressDialog();
            return;
        }
        if (!mPhoneView.getText().toString().matches("((0)|(\\+381))6[01234569]{1}\\d{6,7}")) {
            setError(mPhoneView, "Invalid phone number");
            guiDissmissProgressDialog();
            return;
        }
        if (profileImage == null) {
            showInfo();
            guiDissmissProgressDialog();
            return;
        }

        /*if (mNameView.getText().toString().equals("") || mUsernameView.getText().toString().equals("") ||
                mPasswordView.getText().toString().equals("") || mPhoneView.getText().toString().equals("")
                || profileImage == null) {
            guiDissmissProgressDialog();
            showInfo();
            return;
        }*/

        try {
            if (RESTful.attemptSignUp(name, username, password, phone, profileImage)) {
                Intent mainActivity = new Intent(SignupActivity.this, MainActivity.class);
                mainActivity.putExtra("username", mUsernameView.getText().toString());
                startActivity(mainActivity);
                finish();
            } else {
                guiNotifyUser();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void guiNotifyUser() {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                mUsernameView.setError(RESTful.RESPONSE_STRING);
                mUsernameView.requestFocus();
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

    private void showImage(Bitmap bmp) {
        Dialog builder = new Dialog(this);
        builder.requestWindowFeature(Window.FEATURE_NO_TITLE);
        builder.getWindow().setBackgroundDrawable(
                new ColorDrawable(android.graphics.Color.TRANSPARENT));
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                //nothing;
            }
        });

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(bmp);
        builder.addContentView(imageView, new RelativeLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        builder.show();
    }

    private void showInfo() {
        guiThread.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(SignupActivity.this);
                builder.setTitle("Take a selfie");
                builder.setMessage("Profile picture is required for registration!");
                builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setIcon(android.R.drawable.ic_dialog_info);

                builder.show();
            }
        });
    }
}
