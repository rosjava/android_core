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

package org.ros.rosjava.android;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.ros.node.NodeConfiguration;

import com.google.common.base.Preconditions;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Displays a text box to allow the user to enter a URI or scan a QR code. Then
 * it returns that uri to the calling activity. When this activity is started
 * the last used (or the default) uri is displayed to the user.
 * 
 * @author ethan.rublee@gmail.com (Ethan Rublee)
 * @author damonkohler@google.com (Damon Kohler)
 * @author munjaldesai@google.com (Munjal Desai)
 */
public class MasterChooser extends Activity {

  /**
   * The key with which the last used uri will be stored as a preference.
   */
  private static final String PREFS_KEY_NAME = "URI_KEY";
  /**
   * Package name of the QR code reader used to scan QR codes.
   */
  private static final String BAR_CODE_SCANNER_PACKAGE_NAME =
      "com.google.zxing.client.android.SCAN";
  private String masterUri = "";
  private EditText uriText;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.master_chooser);
    uriText = (EditText) findViewById(R.id.master_chooser_uri);
    // Get the URI from preferences and display it. Since only primitive types
    // can be saved in preferences the URI is stored as a string.
    masterUri =
        getPreferences(MODE_PRIVATE).getString(PREFS_KEY_NAME,
            NodeConfiguration.DEFAULT_MASTER_URI.toString());
    uriText.setText(masterUri);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent intent) {
    // If the Barcode Scanner returned a string then display that string.
    if (requestCode == 0) {
      if (resultCode == RESULT_OK) {
        Preconditions.checkState(intent.getStringExtra("SCAN_RESULT_FORMAT").equals("TEXT_TYPE"));
        String contents = intent.getStringExtra("SCAN_RESULT");
        uriText.setText(contents);
      }
    }
  }

  public void qrCodeButtonClicked(View unused) {
    Intent intent = new Intent(BAR_CODE_SCANNER_PACKAGE_NAME);
    intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
    // Check if the Barcode Scanner is installed.
    if (!isQRCodeReaderInstalled(intent)) {
      // Open the Market and take them to the page from which they can download
      // the Barcode Scanner app.
      startActivity(new Intent(Intent.ACTION_VIEW,
          Uri.parse("market://details?id=com.google.zxing.client.android")));
    } else {
      // Call the Barcode Scanner to let the user scan a QR code.
      startActivityForResult(intent, 0);
    }
  }

  public void okButtonClicked(View unused) {
    // Get the current text entered for URI.
    String userUri = uriText.getText().toString();

    if (userUri.length() == 0) {
      // If there is no text input then set it to the default URI.
      userUri = NodeConfiguration.DEFAULT_MASTER_URI.toString();
      uriText.setText(userUri);
      Toast.makeText(MasterChooser.this, "Empty URI not allowed.", Toast.LENGTH_SHORT).show();
    }
    // Make sure the URI can be parsed correctly.
    try {
      new URI(userUri); // Test the supplied URI.
    } catch (URISyntaxException e) {
      Toast.makeText(MasterChooser.this, "Invalid URI.", Toast.LENGTH_SHORT).show();
      return;
    }

    // If the displayed URI is valid then pack that into the intent.
    masterUri = userUri;
    SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
    editor.putString(PREFS_KEY_NAME, masterUri);
    editor.commit();
    // Package the intent to be consumed by the calling activity.
    Intent intent = new Intent();
    intent.putExtra("ROS_MASTER_URI", masterUri);
    setResult(RESULT_OK, intent);
    finish();
  }

  public void cancelButtonClicked(View unused) {
    setResult(RESULT_CANCELED);
    finish();
  }

  /**
   * Check if the specified app is installed.
   * 
   * @param intent
   *          The activity that you wish to look for.
   * @return true if the desired activity is install on the device, false
   *         otherwise.
   */
  private boolean isQRCodeReaderInstalled(Intent intent) {
    List<ResolveInfo> list =
        getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    return (list.size() > 0);
  }
}
