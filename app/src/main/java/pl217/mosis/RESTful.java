package pl217.mosis;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import pl217.mosis.model.Event;
import pl217.mosis.model.User;

public class RESTful {

    public static String IP_ADDRESS = "192.168.0.100";
    public static int PORT_NUMBER = 8080;
    public static String RESPONSE_STRING = "";

    public static boolean attemptSignUp(String name, String username, String password, String phone, Bitmap profileImage) {
        boolean response = false;
        RESPONSE_STRING = "";

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/register");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            profileImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
            String image = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("username", username);
            data.put("password", password);
            data.put("phone", phone);
            data.put("image", image);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                response = jsonObject.getBoolean("response");
                RESPONSE_STRING = jsonObject.getString("message");
            }

        } catch (Exception e) {
            e.printStackTrace();
            RESPONSE_STRING = e.getMessage();
        }

        return response;
    }

    public static boolean attemptLogin(String username, String password) {
        boolean response = false;
        RESPONSE_STRING = "";

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("password", password);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                response = jsonObject.getBoolean("response");
                RESPONSE_STRING = jsonObject.getString("message");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return response;
    }

    public static ArrayList<Event> getEventsInRadius(double latitude, double longitude, int radius) {

        ArrayList<Event> retList = new ArrayList<>();

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/eventsinradius");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("radius", radius);
            data.put("latitude", latitude);
            data.put("longitude", longitude);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONArray jsonArray = new JSONArray(str);
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(j);

                    Event event = new Event();
                    /*event.setName(jsonObject.getString("name"));
                    event.setAbout(jsonObject.getString("about"));*/
                    event.setCategory(jsonObject.getString("category"));
                    event.setLatitude(jsonObject.getDouble("latitude"));
                    event.setLongitude(jsonObject.getDouble("longitude"));
                    event.setuID(jsonObject.getLong("uid"));

                    retList.add(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retList;
    }

    public static ArrayList<Event> getEventsByCategory(String category) {

        ArrayList<Event> retList = new ArrayList<>();

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/events/" + category);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONArray jsonArray = new JSONArray(str);
                for (int j = 0; j < jsonArray.length(); j++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(j);

                    Event event = new Event();
                    /*event.setName(jsonObject.getString("name"));
                    event.setCategory(jsonObject.getString("category"));
                    event.setAbout(jsonObject.getString("about"));
                    event.setDeadline(jsonObject.getString("deadline"));*/
                    event.setLatitude(jsonObject.getDouble("latitude"));
                    event.setLongitude(jsonObject.getDouble("longitude"));
                    event.setuID(jsonObject.getLong("uid"));

                    retList.add(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retList;
    }

    public static boolean checkIn(String username, long uid) {
        boolean nModified = false;
        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/checkin");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("username", username);
            data.put("uid", uid);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject response = new JSONObject(str);
                nModified = response.getInt("nModified") > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return nModified;
    }

    public static Event getEventByUid(long Uid, double latitude, double longitude) {
        Event returnEvent = new Event();

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/event/" + Uid +
                    "/" + latitude + "/" + longitude);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                returnEvent.setName(jsonObject.getString("name"));
                returnEvent.setCategory(jsonObject.getString("category"));
                returnEvent.setAbout(jsonObject.getString("about"));
                returnEvent.setDeadline(jsonObject.getString("deadline"));
                returnEvent.setDistance(jsonObject.getLong("distance"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnEvent;
    }

    public static boolean addNewEvent(String name, String about, String category, double latitude,
                                      double longitude, long deadline) {
        boolean ret = false;

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/event");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("about", about);
            data.put("category", category);
            data.put("latitude", latitude);
            data.put("longitude", longitude);
            data.put("deadline", deadline);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                ret = jsonObject.getBoolean("response");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    public static User getUser(String username) {
        User retUser = null;

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/user/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                retUser = new User();
                retUser.setUsername(jsonObject.getString("username"));
                retUser.setFullName(jsonObject.getString("name_lastname"));
                retUser.setPhone(jsonObject.getString("phone"));
                retUser.setPoints(jsonObject.getInt("points"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retUser;
    }

    public static Bitmap myImage(String username) {

        Bitmap retImg = null;

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/image/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "image/jpeg");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                byte[] imgBytes = toByteArray(connection.getInputStream());

                retImg = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retImg;
    }

    public static Event updateUserLocation(String jsonObject) {

        Event retEvent = null;
        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/location");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("PUT");
            connection.setDoOutput(true);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(jsonObject);
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject json = new JSONObject(str);
                retEvent = new Event();
                retEvent.setuID(json.getLong("uid"));
                retEvent.setCategory(json.getString("category"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retEvent;
    }

    public static ArrayList<String> getFriends(String username) {
        ArrayList<String> retList = new ArrayList<>();

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/friends/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONArray jsonArray = new JSONArray(str);
                for (int j = 0; j < jsonArray.length(); j++) {
                    String friend = jsonArray.getString(j);
                    retList.add(friend);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return retList;
    }

    public static LatLng getUserPosition(String username) {
        LatLng latLng = null;

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/position/" + username);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                latLng = new LatLng(jsonObject.getDouble("lastLatitude"), jsonObject.getDouble("lastLongitude"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return latLng;
    }

    public static boolean becomeFriends(String firstUser, String secondUser, boolean increasePoints) {
        boolean returnBoolean = false;

        try {

            URL url = new URL("http", IP_ADDRESS, PORT_NUMBER, "/bluetooth");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(10000);
            connection.setDoInput(true);
            connection.setDoOutput(true);

            JSONObject data = new JSONObject();
            data.put("firstUser", firstUser);
            data.put("secondUser", secondUser);
            data.put("incScore", increasePoints);

            OutputStream stream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream, "UTF-8"));
            writer.write(data.toString());
            writer.flush();
            writer.close();
            stream.close();

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                String str = inputStreamToString(connection.getInputStream());
                JSONObject jsonObject = new JSONObject(str);
                returnBoolean = jsonObject.getBoolean("response");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return returnBoolean;
    }

    private static String inputStreamToString(InputStream is) {
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

    private static byte[] toByteArray(InputStream is) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }

            buffer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return buffer.toByteArray();
    }
}
