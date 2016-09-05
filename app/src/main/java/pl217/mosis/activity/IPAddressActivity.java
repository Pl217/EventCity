package pl217.mosis.activity;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.regex.Pattern;

import pl217.mosis.R;
import pl217.mosis.RESTful;

public class IPAddressActivity extends AppCompatActivity {

    private static final Pattern PATTERN = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

    public static boolean validate(String ip) {
        return PATTERN.matcher(ip).matches();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ipaddress);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button saveButton = (Button) findViewById(R.id.saveButt);
        if (saveButton != null)
            saveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText ipAddress = (EditText) findViewById(R.id.addressConfig);
                    EditText port = (EditText) findViewById(R.id.portConfig);
                    if (validate(ipAddress.getText().toString()) && validatePort(port.getText().toString())) {
                        RESTful.IP_ADDRESS = ipAddress.getText().toString();
                        RESTful.PORT_NUMBER = Integer.parseInt(port.getText().toString());

                        finish();
                    } else {
                        Snackbar snackbar = Snackbar.make(v, "Invalid IP address or port number", Snackbar.LENGTH_SHORT);
                        View sbView = snackbar.getView();
                        sbView.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                        snackbar.show();
                    }
                }
            });
    }

    private boolean validatePort(String port) {
        return !port.equals("") && Integer.parseInt(port) > 1024 && Integer.parseInt(port) <= 65535;
    }
}
