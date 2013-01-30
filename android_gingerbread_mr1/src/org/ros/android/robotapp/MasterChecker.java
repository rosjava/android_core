package org.ros.android.robotapp;

import android.util.Log;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
/**
 * Threaded ROS-master checker. Runs a thread which checks for a valid ROS
 * master and sends back a {@link RobotDescription} (with robot name and type)
 * on success or a failure reason on failure.
 *
 * @author hersh@willowgarage.com
 */
public class MasterChecker {
  public interface RobotDescriptionReceiver {
    /** Called on success with a description of the robot that got checked. */
    void receive(RobotDescription robotDescription);
  }
  public interface FailureHandler {
    /**
     * Called on failure with a short description of why it failed, like
     * "exception" or "timeout".
     */
    void handleFailure(String reason);
  }
  private CheckerThread checkerThread;
  private RobotDescriptionReceiver foundMasterCallback;
  private FailureHandler failureCallback;
  /** Constructor. Should not take any time. */
  public MasterChecker(RobotDescriptionReceiver foundMasterCallback, FailureHandler failureCallback) {
    this.foundMasterCallback = foundMasterCallback;
    this.failureCallback = failureCallback;
  }
  /**
   * Start the checker thread with the given robotId. If the thread is
   * already running, kill it first and then start anew. Returns immediately.
   */
  public void beginChecking(RobotId robotId) {
    stopChecking();
    if (robotId.getMasterUri() == null) {
      failureCallback.handleFailure("empty master URI");
      return;
    }
    URI uri;
    try {
      uri = new URI(robotId.getMasterUri());
    } catch (URISyntaxException e) {
      failureCallback.handleFailure("invalid master URI");
      return;
    }
    checkerThread = new CheckerThread(robotId, uri);
    checkerThread.start();
  }
  /** Stop the checker thread. */
  public void stopChecking() {
    if (checkerThread != null && checkerThread.isAlive()) {
      checkerThread.interrupt();
    }
  }
  private class CheckerThread extends Thread {
    private URI masterUri;
    private RobotId robotId;
    public CheckerThread(RobotId robotId, URI masterUri) {
      this.masterUri = masterUri;
      this.robotId = robotId;
      setDaemon(true);
      // don't require callers to explicitly kill all the old checker threads.
      setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
        @Override
        public void uncaughtException(Thread thread, Throwable ex) {
          failureCallback.handleFailure("exception: " + ex.getMessage());
        }
      });
    }
    @Override
    public void run() {
      try {
        ParameterClient paramClient = new ParameterClient(
                  NodeIdentifier.forNameAndUri("/master_checker", masterUri.toString()), masterUri);
        String robotName = (String) paramClient.getParam(GraphName.of("robot/name")).getResult();
        String robotType = (String) paramClient.getParam(GraphName.of("robot/type")).getResult();
        Date timeLastSeen = new Date();
        RobotDescription robotDescription = new RobotDescription(robotId, robotName, robotType,
                                                                 timeLastSeen);
        foundMasterCallback.receive(robotDescription);
        return;
      } catch (Throwable ex) {
        Log.e("RosAndroid", "Exception while creating node in MasterChecker for master URI "
              + masterUri, ex);
        failureCallback.handleFailure(ex.toString());
      }
    }
  }
}