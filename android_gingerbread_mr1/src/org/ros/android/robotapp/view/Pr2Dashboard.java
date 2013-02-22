package org.ros.android.robotapp.view;

import org.ros.android.android_gingerbread_mr1.R;
import org.ros.android.robotapp.Dashboard.DashboardInterface;
import org.ros.android.view.BatteryLevelView;
import org.ros.exception.RemoteException;
import org.ros.exception.ServiceNotFoundException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.service.ServiceClient;
import org.ros.node.service.ServiceResponseListener;
import org.ros.node.topic.Subscriber;

import pr2_msgs.DashboardState;
import pr2_power_board.PowerBoardCommandRequest;
import pr2_power_board.PowerBoardCommandResponse;
import std_srvs.EmptyRequest;
import std_srvs.EmptyResponse;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class Pr2Dashboard extends LinearLayout implements DashboardInterface {

	private ImageButton modeButton;
	private ProgressBar modeWaitingSpinner;
	private BatteryLevelView robotBattery;
	private ImageView wirelessEstop;
	private ImageView physicalEstop;

	private enum Pr2RobotState {
		UNKNOWN, ANY, NONE, BREAKERS_OUT, MOTORS_OUT, WORKING
	}

	private Pr2RobotState state = Pr2RobotState.UNKNOWN;
	private Pr2RobotState waitingState = Pr2RobotState.ANY;
	private int nBreakers;
	private long serialNumber;
	private ConnectedNode connectedNode;
	private Subscriber<DashboardState> dashboardSubscriber;
	private boolean clickOnTransition = false;
	AlertDialog.Builder alertBuilder;

	public Pr2Dashboard(Context context) {
		super(context);
		inflateSelf(context);
	}

	public Pr2Dashboard(Context context, AttributeSet attrs) {
		super(context, attrs);
		inflateSelf(context);
	}

	private void inflateSelf(Context context) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.pr2_dashboard, this);
		modeButton = (ImageButton) findViewById(R.id.pr2_mode_button);
		modeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				onModeButtonClicked();
			}
		});
		modeWaitingSpinner = (ProgressBar) findViewById(R.id.pr2_mode_waiting_spinner);
		modeWaitingSpinner.setIndeterminate(true);
		modeWaitingSpinner.setVisibility(View.GONE);
		setModeWaiting(true);
		robotBattery = (BatteryLevelView) findViewById(R.id.pr2_robot_battery);
		wirelessEstop = (ImageView) findViewById(R.id.pr2_wireless_estop);
		physicalEstop = (ImageView) findViewById(R.id.pr2_physical_estop);
		state = Pr2RobotState.UNKNOWN;
		waitingState = Pr2RobotState.ANY;
		clickOnTransition = false;
		alertBuilder = new AlertDialog.Builder(context)
				.setTitle("Error")
				.setCancelable(false)
				.setNeutralButton("Dismiss",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int which) {
							}
						});
	}

	/**
	 * Set the ROS Node to use to get status data and connect it up. Disconnects
	 * the previous node if there was one.
	 */
	@Override
	public void onStart(ConnectedNode connectedNode) {
		stop();
		this.connectedNode = connectedNode;
		try {
			dashboardSubscriber = connectedNode.newSubscriber("dashboard_agg",
					"pr2_msgs/DashboardState");
			dashboardSubscriber
					.addMessageListener(new MessageListener<DashboardState>() {
						@Override
						public void onNewMessage(final DashboardState msg) {
							Pr2Dashboard.this.post(new Runnable() {
								@Override
								public void run() {
									Pr2Dashboard.this.handleDashboardState(msg);
								}
							});
						}
					});
			// NameResolver resolver =
			// connectedNode.getResolver().createResolver(GraphName.of("/"));
		} catch (Exception ex) {
			this.connectedNode = null;
		}
	}

	public void stop() {
		if (dashboardSubscriber != null) {
			dashboardSubscriber.shutdown();
		}
		dashboardSubscriber = null;
		connectedNode = null;
	}

	/**
	 * Populate view with new diagnostic data. This must be called in the UI
	 * thread.
	 */
	private void handleDashboardState(DashboardState msg) {
		robotBattery.setBatteryPercent((int) msg.getPowerState().getRelativeCapacity());
		robotBattery.setPluggedIn(msg.getPowerState().getACPresent() != 0);
		if (msg.getPowerBoardState().getWirelessStop() == false) {
			physicalEstop.setColorFilter(Color.GRAY);
			wirelessEstop.setColorFilter(Color.RED);
		} else {
			wirelessEstop.setColorFilter(Color.GREEN);
			if (msg.getPowerBoardState().getRunStop() == true) {
				physicalEstop.setColorFilter(Color.GREEN);
			} else {
				physicalEstop.setColorFilter(Color.RED);
			}
		}
		Pr2RobotState previous_state = state;
		boolean breaker_state = true;
		if (msg.getPowerBoardStateValid() == true
				&& msg.getPowerStateValid() == true) {
			for (int i = 0; i < msg.getPowerBoardState().getCircuitState().capacity(); i++) {
				if (msg.getPowerBoardState().getCircuitState().getInt(i) != 3) { // Breaker
																	// invalid
					breaker_state = false;
				}
			}
			nBreakers = msg.getPowerBoardState().getCircuitState().capacity();
			serialNumber = msg.getPowerBoardState().getSerialNum();
		} else {
			breaker_state = false;
		}
		if (breaker_state == false) {
			modeButton.setColorFilter(Color.RED);
			state = Pr2RobotState.BREAKERS_OUT;
		} else {
			if (msg.getMotorsHaltedValid() == true
					&& msg.getMotorsHalted().getData() == true) {
				modeButton.setColorFilter(Color.YELLOW);
				state = Pr2RobotState.MOTORS_OUT;
			} else { // FIXME: diagnostics
				modeButton.setColorFilter(Color.GREEN);
				state = Pr2RobotState.WORKING;
			}
		}
		if (state != previous_state) {
			if ((state == waitingState || waitingState == Pr2RobotState.ANY)
					&& waitingState != Pr2RobotState.NONE) {
				setModeWaiting(false);
				waitingState = Pr2RobotState.NONE;
			}
			if (clickOnTransition) {
				clickOnTransition = false;
				onModeButtonClicked();
			}
		}
	}

	private void onModeButtonClicked() {
	    ServiceClient<EmptyRequest, EmptyResponse> motorServiceClient = null;
	    ServiceClient<PowerBoardCommandRequest, PowerBoardCommandResponse> modeServiceClient = null;
	    EmptyRequest motorRequest = connectedNode.getTopicMessageFactory().newFromType(EmptyRequest._TYPE);
	    PowerBoardCommandRequest modeRequest;
	    switch (state) {
	    case BREAKERS_OUT:
	      waitingState = Pr2RobotState.MOTORS_OUT;
	      setModeWaiting(true);
	      clickOnTransition = true;
	      //Send reset to the breakers.
	      for (int i = 0; i < nBreakers; i++) {
	                modeRequest = connectedNode.getTopicMessageFactory().newFromType(PowerBoardCommandRequest._TYPE);
	        modeRequest.setBreakerNumber(i);
	        modeRequest.setCommand("start");
	        modeRequest.setSerialNumber((int) serialNumber);
	        try {
	          modeServiceClient =
	          connectedNode.newServiceClient("power_board/control", "pr2_power_board/PowerBoardCommand");
	        } catch( ServiceNotFoundException ex ) {
	          this.connectedNode = null;
	          //throw( new RosException( ex.toString() ));
	        }
	        modeServiceClient.call(modeRequest, new ServiceResponseListener<PowerBoardCommandResponse>() {
	            @Override
	            public void onSuccess(PowerBoardCommandResponse message) { } //Diagnostics will update.
	            @Override
	            public void onFailure(RemoteException ex) {
	              final Exception e = ex;
	              Pr2Dashboard.this.post(new Runnable() {
	                  public void run() {
	                    alertBuilder.setMessage("Cannot reset the breakers: " + e.toString()).show();
	                  }});
	            }});
	      }
	      break;
	    case MOTORS_OUT:
	      waitingState = Pr2RobotState.WORKING;
	      setModeWaiting(true);
	      //Send reset to the motors.
	      try {
	        motorServiceClient =
	          connectedNode.newServiceClient("pr2_etherCAT/reset_motors", "std_srvs/Empty");
	      } catch( ServiceNotFoundException ex ) {
	        this.connectedNode = null;
	        //throw( new RosException( ex.toString() ));
	      }
	      motorServiceClient.call(motorRequest, new ServiceResponseListener<EmptyResponse>() {
	          @Override
	          public void onSuccess(EmptyResponse message) { } //Diagnostics will update.
	          @Override
	          public void onFailure(RemoteException ex) {
	            final Exception e = ex;
	            Pr2Dashboard.this.post(new Runnable() {
	                public void run() {
	                  alertBuilder.setMessage("Cannot reset the motors: " + e.toString()).show();
	                }});
	          }});
	      break;
	    case WORKING:
	      setModeWaiting(true);
	      waitingState = Pr2RobotState.BREAKERS_OUT;
	      //Stop the breakers
	      for (int i = 0; i < nBreakers; i++) {
	                modeRequest = connectedNode.getTopicMessageFactory().newFromType(PowerBoardCommandRequest._TYPE);
	        modeRequest.setBreakerNumber(i);
	        modeRequest.setCommand("stop");
	        modeRequest.setSerialNumber((int) serialNumber);
	        try {
	          modeServiceClient =
	            connectedNode.newServiceClient("power_board/control", "pr2_power_board/PowerBoardCommand");
	        } catch( ServiceNotFoundException ex ) {
	          this.connectedNode = null;
	          //throw( new RosException( ex.toString() ));
	        }
	        modeServiceClient.call(modeRequest, new ServiceResponseListener<PowerBoardCommandResponse>() {
	            @Override
	            public void onSuccess(PowerBoardCommandResponse message) { } //Diagnostics will update.
	            @Override
	            public void onFailure(RemoteException ex) {
	              final Exception e = ex;
	              Pr2Dashboard.this.post(new Runnable() {
	                  public void run() {
	                    alertBuilder.setMessage("Cannot reset the breakers: " + e.toString()).show();
	                  }});
	            }});
	      }
	      //Send halt to the motors.
	      try {
	        motorServiceClient =
	          connectedNode.newServiceClient("pr2_etherCAT/halt_motors", "std_srvs/Empty");
	      } catch( ServiceNotFoundException ex ) {
	        this.connectedNode = null;
	        //throw( new RosException( ex.toString() ));
	      }
	      motorServiceClient.call(motorRequest, new ServiceResponseListener<std_srvs.EmptyResponse>() {
	          @Override
	          public void onSuccess(EmptyResponse message) { } //Diagnostics will update.
	          @Override
	          public void onFailure(RemoteException ex) {
	            final Exception e = ex;
	            Pr2Dashboard.this.post(new Runnable() {
	                public void run() {
	                  alertBuilder.setMessage("Cannot reset the motors: " + e.toString()).show();
	                }});
	          }});
	      break;
	    default:
	      Pr2Dashboard.this.post(new Runnable() {
	          public void run() {
	            alertBuilder.setMessage("Robot is in an unknown or invalid state. Please wait and try again.").show();
	          }});
	      break;
	    }
	}

	private void setModeWaiting(final boolean waiting) {
		post(new Runnable() {
			@Override
			public void run() {
				modeWaitingSpinner.setVisibility(waiting ? View.VISIBLE
						: View.GONE);
			}
		});
	}

	@Override
	public void onShutdown(Node node) {
		// TODO Auto-generated method stub

	}
}
