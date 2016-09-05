package pl217.mosis.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import pl217.mosis.R;
import pl217.mosis.RESTful;

public class UserRankingsActivity extends AppCompatActivity {

    private static final ArrayList<HashMap<String, Object>> list =
            new ArrayList<HashMap<String, Object>>();
    private ArrayList<String> usernames = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_rankings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupActionBar();

        new DownloadUserRankings().execute();
    }

    private void setAdapter() {
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                list,
                R.layout.row_view,
                new String[]{"rank", "name", "image", "points"},
                new int[]{R.id.ranking, R.id.ranking_name, R.id.ranking_image, R.id.ranking_points}
        );

        adapter.setViewBinder(new SimpleAdapter.ViewBinder() {

            @Override
            public boolean setViewValue(View view, Object data, String textRepresentation) {
                if (view.getId() == R.id.ranking_image) {
                    ImageView imageView = (ImageView) view;
                    Bitmap bmp = (Bitmap) data;
                    imageView.setImageBitmap(bmp);
                    return true;
                }
                return false;
            }
        });

        ListView listView = (ListView) findViewById(R.id.ranking_list);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String username = usernames.get(position);
                SharedPreferences sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
                if (!sharedPref.getString(getString(R.string.stored_username), "Not provided").equals(username)) {
                    Intent userInfo = new Intent(UserRankingsActivity.this, UserInfoActivity.class);
                    userInfo.putExtra("username", username);
                    startActivity(userInfo);
                } else {
                    Snackbar snackbar = Snackbar.make(view, "You should know everything about yourself", Snackbar.LENGTH_LONG);
                    View sbView = snackbar.getView();
                    sbView.setBackgroundColor(getResources().getColor(R.color.rankingsSeparator));
                    snackbar.show();
                }
            }
        });
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

    private class DownloadUserRankings extends AsyncTask<Void, Void, Void> {

        private final ProgressDialog progressDialog = new ProgressDialog(UserRankingsActivity.this);

        @Override
        protected void onPreExecute() {
            progressDialog.setMessage("Downloading user rankings");
            progressDialog.show();

            list.clear();
            usernames.clear();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {

                URL url = new URL("http", RESTful.IP_ADDRESS, 8080, "/rankings");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(10000);
                connection.setDoInput(true);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String str = inputStreamToString(connection.getInputStream());
                    JSONObject jsonObject = new JSONObject(str);
                    JSONArray jsonArray = jsonObject.getJSONArray("images");

                    for (int i = 0; i < jsonArray.length(); i++) {
                        jsonObject = jsonArray.getJSONObject(i);
                        HashMap map = new HashMap();
                        map.put("rank", i + 1);
                        map.put("name", jsonObject.getString("username") + "\n" + jsonObject.getString("name_lastname"));
                        map.put("points", jsonObject.getInt("points"));

                        Bitmap img = RESTful.myImage(jsonObject.getString("username"));
                        map.put("image", img);

                        list.add(map);
                        usernames.add(jsonObject.getString("username"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        private String inputStreamToString(InputStream is) {
            String line = "";
            StringBuilder total = new StringBuilder();

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(is));
            try {
                while ((line = bufferedReader.readLine()) != null)
                    total.append(line);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return total.toString();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (progressDialog.isShowing())
                progressDialog.dismiss();

            setAdapter();
        }
    }
}
