/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.android;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.ros.android.android_core_components.R;
import org.ros.internal.node.client.MasterClient;
import org.ros.internal.node.xmlrpc.XmlRpcTimeoutException;
import org.ros.namespace.GraphName;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

/**
 * Allows the user to configue a master {@link URI} then it returns that
 * {@link URI} to the calling {@link Activity}.
 * <p>
 * When this {@link Activity} is started, the last used (or the default)
 * {@link URI} is displayed to the user.
 *
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MasterChooserWear extends WearableActivity {

  /**
   * Lookup text for catching a ConnectionException when attempting to
   * connect to a master.
   */
  private static final String CONNECTION_EXCEPTION_TEXT = "ECONNREFUSED";

  /**
   * Lookup text for catching a UnknownHostException when attemping to
   * connect to a master.
   */
  private static final String UNKNOW_HOST_TEXT = "UnknownHost";

  /**
   * Default port number for master URI. Apended if the URI does not
   * contain a port number.
   */
  private static final int DEFAULT_PORT = 11311;

  private EditText ip_field;
  private EditText port_number;
  private Button connectButton;
  boolean ipValid = false;
  boolean portValid = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.master_chooser_wear);
    final Pattern ipPattern = RosURIPattern.IP_ADDRESS;
    final Pattern portPattern = RosURIPattern.PORT;
    ip_field = findViewById(R.id.ip_number);
    ip_field.setKeyListener(IPAddressKeyListener.getInstance());
    port_number = findViewById(R.id.portNumber);
    connectButton = findViewById(R.id.connectButton);

    ip_field.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String ip = s.toString();
        ipValid = ipPattern.matcher(ip).matches();
        connectButton.setEnabled(ipValid && portValid);
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });

    port_number.addTextChangedListener(new TextWatcher() {
      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
        final String port = s.toString();
        portValid = portPattern.matcher(port).matches() && Integer.parseInt(port) >= 0 && Integer.parseInt(port) <= 65535;
        connectButton.setEnabled(ipValid && portValid);
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
      }
    });
  }

  @SuppressLint("StaticFieldLeak")
  public void connectButtonClicked(View unused) {
    final String ip = ip_field.getText().toString();
    final String port = port_number.getText().toString();
    final String uri = "http://" + ip + ":" + port;

    // Prevent further edits while we verify the URI.
    // Note: This was placed after the URI port check due to odd behavior
    // with setting the connectButton to disabled.
    ip_field.setEnabled(false);
    port_number.setEnabled(false);
    connectButton.setEnabled(false);

    // Make sure the URI can be parsed correctly and that the master is
    // reachable.
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected void onPreExecute() {
      }

      @Override
      protected Boolean doInBackground(Void... params) {
        try {
          MasterClient masterClient = new MasterClient(new URI(uri));
          masterClient.getUri(GraphName.of("android/master_chooser_activity"));
          toast("Connected");
          return true;
        } catch (URISyntaxException e) {
          toast("Invalid URI.");
          return false;
        } catch (XmlRpcTimeoutException e) {
          toast("Master unreachable!");
          return false;
        } catch (Exception e) {
          String exceptionMessage = e.getMessage();
          if (exceptionMessage.contains(CONNECTION_EXCEPTION_TEXT))
            toast("Unable to communicate with master!");
          else if (exceptionMessage.contains(UNKNOW_HOST_TEXT))
            toast("Unable to resolve URI hostname!");
          else
            toast("Communication error!");
          return false;
        }
      }

      @Override
      protected void onPostExecute(Boolean result) {
        if (result) {
          // If the displayed URI is valid then pack that into the intent.
          // Package the intent to be consumed by the calling activity.
          Intent intent = createNewMasterIntent(false, true);
          setResult(RESULT_OK, intent);
          finish();
        } else {
          connectButton.setEnabled(true);
          ip_field.setEnabled(true);
          port_number.setEnabled(true);
        }
      }
    }.execute();
  }

  protected void toast(final String text) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        Toast.makeText(MasterChooserWear.this, text, Toast.LENGTH_SHORT).show();
      }
    });
  }

  public Intent createNewMasterIntent(boolean newMaster, boolean isPrivate) {
    Intent intent = new Intent();
    final String ip = ip_field.getText().toString();
    final String port = port_number.getText().toString();
    final String uri = "http://" + ip + ":" + port;
    intent.putExtra("ROS_MASTER_CREATE_NEW", newMaster);
    intent.putExtra("ROS_MASTER_PRIVATE", isPrivate);
    intent.putExtra("ROS_MASTER_URI", uri);
    return intent;
  }


  /**
   * Regular expressions used with ROS URIs.
   * <p>
   * The majority of the expressions and variables were copied from
   * {@link android.util.Patterns}. The {@link android.util.Patterns} class could not be
   * utilized because the PROTOCOL regex included other web protocols besides http. The
   * http protocol is required by ROS.
   */
  private static class RosURIPattern {
    /* A word boundary or end of input.  This is to stop foo.sure from matching as foo.su */
    private static final String WORD_BOUNDARY = "(?:\\b|$|^)";

    /**
     * Valid UCS characters defined in RFC 3987. Excludes space characters.
     */
    private static final String UCS_CHAR = "[" +
            "\u00A0-\uD7FF" +
            "\uF900-\uFDCF" +
            "\uFDF0-\uFFEF" +
            "\uD800\uDC00-\uD83F\uDFFD" +
            "\uD840\uDC00-\uD87F\uDFFD" +
            "\uD880\uDC00-\uD8BF\uDFFD" +
            "\uD8C0\uDC00-\uD8FF\uDFFD" +
            "\uD900\uDC00-\uD93F\uDFFD" +
            "\uD940\uDC00-\uD97F\uDFFD" +
            "\uD980\uDC00-\uD9BF\uDFFD" +
            "\uD9C0\uDC00-\uD9FF\uDFFD" +
            "\uDA00\uDC00-\uDA3F\uDFFD" +
            "\uDA40\uDC00-\uDA7F\uDFFD" +
            "\uDA80\uDC00-\uDABF\uDFFD" +
            "\uDAC0\uDC00-\uDAFF\uDFFD" +
            "\uDB00\uDC00-\uDB3F\uDFFD" +
            "\uDB44\uDC00-\uDB7F\uDFFD" +
            "&&[^\u00A0[\u2000-\u200A]\u2028\u2029\u202F\u3000]]";

    /**
     * Valid characters for IRI label defined in RFC 3987.
     */
    private static final String LABEL_CHAR = "a-zA-Z0-9" + UCS_CHAR;

    /**
     * RFC 1035 Section 2.3.4 limits the labels to a maximum 63 octets.
     */
    private static final String IRI_LABEL =
            "[" + LABEL_CHAR + "](?:[" + LABEL_CHAR + "\\-]{0,61}[" + LABEL_CHAR + "]){0,1}";

    private static final Pattern IP_ADDRESS
            = Pattern.compile(
            "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
                    + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
                    + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
                    + "|[1-9][0-9]|[0-9]))");

    /**
     * Regular expression that matches domain names without a TLD
     */
    private static final String RELAXED_DOMAIN_NAME =
            "(?:" + "(?:" + IRI_LABEL + "(?:\\.(?=\\S))" + "?)+" +
                    "|" + IP_ADDRESS + ")";

    private static final String HTTP_PROTOCOL = "(?i:http):\\/\\/";

    public static final int HTTP_PROTOCOL_LENGTH = ("http://").length();

    private static final String PORT_NUMBER = "^\\d{1,5}\\/?";

    /**
     * Regular expression pattern to match valid rosmaster URIs.
     * This assumes the port number and trailing "/" will be auto
     * populated (default port: 11311) if left out.
     */
    public static final Pattern URI = Pattern.compile("("
            + WORD_BOUNDARY
            + "(?:"
            + "(?:" + HTTP_PROTOCOL + ")"
            + "(?:" + RELAXED_DOMAIN_NAME + ")"
            + "(?:" + PORT_NUMBER + ")?"
            + ")"
            + WORD_BOUNDARY
            + ")");

    public static final Pattern PORT = Pattern.compile(PORT_NUMBER);
  }
}
