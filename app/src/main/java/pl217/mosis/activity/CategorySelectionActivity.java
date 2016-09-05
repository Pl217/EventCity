package pl217.mosis.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;

import pl217.mosis.R;

public class CategorySelectionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_selection);

        Button button = (Button) findViewById(R.id.category_search_button);
        if (button != null)
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent bundle = new Intent();

                    Spinner typeSpinner = (Spinner) findViewById(R.id.category_spinner);
                    if (typeSpinner != null)
                        bundle.putExtra("category", typeSpinner.getSelectedItem().toString());

                    setResult(RESULT_OK, bundle);
                    finish();
                }
            });
    }
}
