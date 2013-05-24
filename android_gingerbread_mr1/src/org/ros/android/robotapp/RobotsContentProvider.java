/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.robotapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;
public class RobotsContentProvider extends ContentProvider {
  DatabaseHelper dbh;
  private class DatabaseHelper extends SQLiteOpenHelper {
    DatabaseHelper(Context context) {
      super(context, DATABASE_NAME, null, DATABASE_VERSION); 
    } 
    /**
     * Called by the database helper.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
      Log.i("MasterChooser", "Creating database...");
      db.execSQL(TABLE_CREATE);
    }
    /**
     * On upgrade of the database, do nothing
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
      Log.i("MasterChooser", "The database is being upgraded from " + oldVersion + " to " + newVersion);
    }
  }

  public static final Uri CONTENT_URI = Uri.parse("content://org.ros.android.robotapp");
  private static final int DATABASE_VERSION = 2;
  private static final String DATABASE_NAME = "robotlist_table";
  private static final String TABLE_NAME = "robotlist";
  public static final String TABLE_COLUMN = "robots";
  private static final String TABLE_CREATE =
    "CREATE TABLE " + TABLE_NAME + " (" + TABLE_COLUMN + " TEXT);";
  @Override
  public int delete(Uri arg0, String arg1, String[] arg2) {
    Log.e("CurrentRobotContentProvider", "Invalid method: delete");
    return 0;
  }
         
  @Override
  public String getType(Uri uri) {
    Log.e("CurrentRobotContentProvider", "Invalid method: getType");
    return null;
  }
         
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    SQLiteDatabase db = dbh.getWritableDatabase();
    if (db == null) {
      Log.e("CurrentRobotContentProvider", "Could not get the writable database.");
      return null;
    }
    db.beginTransaction();
    if (values.get(TABLE_COLUMN) != null) {
      Log.i("CurrentRobotContentProvider", "Saving robot...");
     
      Cursor c = db.query(TABLE_NAME, new String[] {TABLE_COLUMN, },
                          null, new String[] {}, null, null, null);
      if (c.getCount() > 0) {
        Log.i("CurrentRobotContentProvider", "Update currently existing row");
        db.update(TABLE_NAME, values, null, new String[] {});
      } else {
        Log.i("CurrentRobotContentProvider", "Insert new row");
        if (db.insert(TABLE_NAME, null, values) < 0) {
          Log.e("CurrentRobotContentProvider", "Error inserting row!");
        }
      }
    } else {
      Log.i("CurrentRobotContentProvider", "Deleting saved robot...");
      db.delete(TABLE_NAME, null, new String[] {});
    }
   
    db.setTransactionSuccessful();
    db.endTransaction();   
    db.close();
    Log.i("CurrentRobotContentProvider", "Done saving current robot");
    return CONTENT_URI;
  }
         
  @Override
  public boolean onCreate() {
    dbh = new DatabaseHelper(this.getContext());
    if(dbh == null) {
      return false;
    } else {
      return true;
    }
  }
 
  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
                      String[] selectionArgs, String sortOrder) {
    SQLiteDatabase db = dbh.getReadableDatabase();
    Cursor res = db.query(TABLE_NAME, new String[] {TABLE_COLUMN, },
                          null, new String[] {}, null, null, null);
    //db.close();
    return res;
  }
 
  @Override
  public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
    Log.e("CurrentRobotContentProvider", "Invalid method: update");
    // TODO Auto-generated method stub
    return 0;
  }     
}