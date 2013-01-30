package org.ros.android.view;

import java.util.HashMap;
import java.util.List;

import org.ros.android.android_gingerbread_mr1.R;
import org.ros.android.robotapp.Dashboard.DashboardInterface;
import org.ros.exception.RosException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.topic.Subscriber;

import diagnostic_msgs.DiagnosticArray;
import diagnostic_msgs.DiagnosticStatus;
import diagnostic_msgs.KeyValue;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

public class TurtlebotDashboard extends LinearLayout implements DashboardInterface {
    private ImageButton modeButton;
    private ProgressBar modeWaitingSpinner;
    private BatteryLevelView robotBattery;
    private BatteryLevelView laptopBattery;
    private ConnectedNode connectedNode;
    private Subscriber<diagnostic_msgs.DiagnosticArray> diagnosticSubscriber;
    private boolean powerOn = false;
    private int numModeResponses;
    private int numModeErrors;
    
    public TurtlebotDashboard(Context context) {
            super(context);
            inflateSelf(context);
    }
    public TurtlebotDashboard(Context context, AttributeSet attrs) {
            super(context, attrs);
            inflateSelf(context);
    }
    private void inflateSelf(Context context) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            inflater.inflate(R.layout.turtlebot_dashboard, this);
            modeButton = (ImageButton) findViewById(R.id.mode_button);
            modeButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                            onModeButtonClicked();
                    }
            });
            modeWaitingSpinner = (ProgressBar) findViewById(R.id.mode_waiting_spinner);
            modeWaitingSpinner.setIndeterminate(true);
            modeWaitingSpinner.setVisibility(View.GONE);
            robotBattery = (BatteryLevelView) findViewById(R.id.robot_battery);
            laptopBattery = (BatteryLevelView) findViewById(R.id.laptop_battery);
    }
    /**
     * Set the ROS Node to use to get status data and connect it up. Disconnects
     * the previous node if there was one.
     *
     * @throws RosException
     */
    /**
     * Populate view with new diagnostic data. This must be called in the UI
     * thread.
     */
    private void handleDiagnosticArray(DiagnosticArray msg) {
            String mode = null;
            for(DiagnosticStatus status : msg.getStatus()) {
                    if(status.getName().equals("/Power System/Battery")) {
                            populateBatteryFromStatus(robotBattery, status);
                    }
                    if(status.getName().equals("/Power System/Laptop Battery")) {
                            populateBatteryFromStatus(laptopBattery, status);
                    }
                    if(status.getName().equals("/Mode/Operating Mode")) {
                            mode = status.getMessage();
                    }
            }
            showMode(mode);
    }
    
   
    private void onModeButtonClicked() {
     /*       powerOn = !powerOn;
            SetTurtlebotModeRequest modeRequest = node.getTopicMessageFactory().newFromType(SetTurtlebotModeRequest._TYPE);
            SetDigitalOutputsRequest setDigOutRequest = node.getTopicMessageFactory().newFromType(SetDigitalOutputsRequest._TYPE);
            setDigOutRequest.setDigitalOut1((byte) 0);
            setDigOutRequest.setDigitalOut2((byte) 0);
            if(powerOn) {
                    modeRequest.setMode(TurtlebotSensorState.OI_MODE_FULL);
                    setDigOutRequest.setDigitalOut0((byte) 1); // main breaker on
            } else {
                    modeRequest.setMode(TurtlebotSensorState.OI_MODE_PASSIVE);
                    setDigOutRequest.setDigitalOut0((byte) 0); // main breaker off
            }
            setModeWaiting(true);
            numModeResponses = 0;
            numModeErrors = 0;
            // TODO: can't I save the modeServiceClient? Causes trouble.
            try {
                    ServiceClient<SetTurtlebotModeRequest, SetTurtlebotModeResponse> modeServiceClient = node.newServiceClient("turtlebot_node/set_operation_mode", "turtlebot_node/SetTurtlebotMode");
                    modeServiceClient.call(modeRequest, new ServiceResponseListener<SetTurtlebotModeResponse>() {
                            @Override
                            public void onSuccess(SetTurtlebotModeResponse message) {
                                    numModeResponses++;
                                    updateModeWaiting();
                            }
                            @Override
                            public void onFailure(RemoteException e) {
                                    numModeResponses++;
                                    numModeErrors++;
                                    updateModeWaiting();
                            }
                    });
            } catch(Exception ex) {
                    Toast.makeText(getContext(), "Exception in service call for set_operation_mode: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    Log.i("TurtlebotDashboard", "making toast.");
            }
            try {
                    ServiceClient<SetDigitalOutputsRequest, SetDigitalOutputsResponse> setDigOutServiceClient = node.newServiceClient("turtlebot_node/set_digital_outputs", "turtlebot_node/SetDigitalOutputs");
                    setDigOutServiceClient.call(setDigOutRequest, new ServiceResponseListener<SetDigitalOutputsResponse>() {
                            @Override
                            public void onSuccess(final SetDigitalOutputsResponse msg) {
                                    numModeResponses++;
                                    updateModeWaiting();
                            }
                            @Override
                            public void onFailure(RemoteException e) {
                                    numModeResponses++;
                                    numModeErrors++;
                                    updateModeWaiting();
                            }
                    });
            } catch(Exception ex) {
                    Toast.makeText(getContext(), "Exception in service call for set_digital_outputs: " + ex.getMessage(), Toast.LENGTH_LONG).show();
                    Log.i("TurtlebotDashboard", "making toast.");
            }*/
    }
    
    private void updateModeWaiting() {
            if(numModeResponses >= 2) {
                    setModeWaiting(false);
            }
    }
    private void setModeWaiting(final boolean waiting) {
            post(new Runnable() {
                    @Override
                    public void run() {
                            modeWaitingSpinner.setVisibility(waiting ? View.VISIBLE : View.GONE);
                    }
            });
    }
    private void showMode(String mode) {
            if(mode == null) {
                    modeButton.setColorFilter(Color.GRAY);
            } else if(mode.equals("Full")) {
                    modeButton.setColorFilter(Color.GREEN);
                    powerOn = true;
            } else if(mode.equals("Safe")) {
                    modeButton.setColorFilter(Color.YELLOW);
                    powerOn = true;
            } else if(mode.equals("Passive")) {
                    modeButton.setColorFilter(Color.RED);
                    powerOn = false;
            } else {
                    modeButton.setColorFilter(Color.GRAY);
                    Log.w("TurtlebotDashboard", "Unknown mode string: '" + mode + "'");
            }
            setModeWaiting(false);
    }
    private void populateBatteryFromStatus(BatteryLevelView view, DiagnosticStatus status) {
            HashMap<String, String> values = keyValueArrayToMap(status.getValues());
            try {
                    float percent = 100 * Float.parseFloat(values.get("Charge (Ah)")) / Float.parseFloat(values.get("Capacity (Ah)"));
                    view.setBatteryPercent((int) percent);
                    // TODO: set color red/yellow/green based on level (maybe with
                    // level-set
                    // in XML)
            } catch(NumberFormatException ex) {
                    // TODO: make battery level gray
            } catch(ArithmeticException ex) {
                    // TODO: make battery level gray
            } catch(NullPointerException ex) {
                    // Do nothing: data wasn't there.
            }
            try {
                    view.setPluggedIn(Float.parseFloat(values.get("Current (A)")) > 0);
            } catch(NumberFormatException ex) {
            } catch(ArithmeticException ex) {
            } catch(NullPointerException ex) {
            }
    }
    private HashMap<String, String> keyValueArrayToMap(List<KeyValue> list) {
            HashMap<String, String> map = new HashMap<String, String>();
            for(KeyValue kv : list) {
                    map.put(kv.getKey(), kv.getValue());
            }
            return map;
    }
    
	@Override
	public void onShutdown(Node node) {
        if(diagnosticSubscriber != null) {
            diagnosticSubscriber.shutdown();
    }
    diagnosticSubscriber = null;
    connectedNode = null;
		
	}
	
	@Override
	public void onStart(ConnectedNode connectedNode) {

		this.connectedNode = connectedNode;
        try {
                diagnosticSubscriber = connectedNode.newSubscriber("diagnostics_agg", "diagnostic_msgs/DiagnosticArray");
                diagnosticSubscriber.addMessageListener(new MessageListener<diagnostic_msgs.DiagnosticArray>() {
                        @Override
                        public void onNewMessage(final diagnostic_msgs.DiagnosticArray message) {
                                TurtlebotDashboard.this.post(new Runnable() {
                                        @Override
                                        public void run() {
                                                TurtlebotDashboard.this.handleDiagnosticArray(message);
                                        }
                                });
                        }
                });
                NameResolver resolver = connectedNode.getResolver().newChild(GraphName.of("/turtlebot_node"));
        } catch(Exception ex) {
                this.connectedNode = null;
                try {
					throw (new RosException(ex));
				} catch (RosException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
        }
	}
}