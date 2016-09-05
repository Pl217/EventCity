package pl217.mosis.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;

import pl217.mosis.R;

public class NearbySearchActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_search);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        Button button = (Button) findViewById(R.id.nearby_search_button);
        if (button != null)
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent bundle = new Intent();
                    EditText searchRadius = (EditText) findViewById(R.id.search_radius);

                    if (searchRadius != null && searchRadius.getText().length() == 0) {
                        setResult(RESULT_CANCELED);
                        finish();
                        return;
                    }

                    if (searchRadius != null)
                        bundle.putExtra("radius", Integer.parseInt(searchRadius.getText().toString()));
                    setResult(RESULT_OK, bundle);

                    finish();
                }
            });
    }
}
