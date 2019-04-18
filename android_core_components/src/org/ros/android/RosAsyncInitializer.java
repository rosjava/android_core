package org.ros.android;

import android.app.Activity;
import android.os.AsyncTask;

public class RosAsyncInitializer<T extends Activity & RosInterface> extends AsyncTask<T, Void, Void> {
    
    @Override
    protected Void doInBackground(T... params) {
        params[0].init(params[0].getNodeMainExecutorService());
        return null;
    }
}