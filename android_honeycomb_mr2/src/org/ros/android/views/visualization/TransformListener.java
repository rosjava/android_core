package org.ros.android.views.visualization;

import org.ros.message.MessageListener;
import org.ros.message.geometry_msgs.TransformStamped;
import org.ros.message.tf.tfMessage;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

public class TransformListener implements NodeMain {

  private Transformer transformer = new Transformer();
  private Subscriber<tfMessage> tfSubscriber;

  public Transformer getTransformer() {
    return transformer;
  }

  public void setTransformer(Transformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public void onStart(Node node) {
    transformer.setPrefix(node.newParameterTree().getString("~tf_prefix", ""));
    tfSubscriber = node.newSubscriber("tf", "tf/tfMessage", new MessageListener<tfMessage>() {
      @Override
      public void onNewMessage(tfMessage message) {
        for (TransformStamped transform : message.transforms) {
          transformer.updateTransform(transform);
        }
      }
    });
  }

  @Override
  public void onShutdown(Node node) {
    tfSubscriber.shutdown();
  }

}
