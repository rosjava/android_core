/*
 * Copyright (c) 2012, Chad Rockey
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Android Robot Monitor nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.ros.android.view;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;
import android.widget.TableLayout;
import diagnostic_msgs.DiagnosticArray;
import diagnostic_msgs.DiagnosticStatus;
import org.ros.android.android_10.R;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeMain;
import org.ros.node.topic.Subscriber;

import java.util.List;

/**
 * @author damonkohler@google.com (Damon Kohler)
 * @author chadrockey@gmail.com (Chad Rockey)
 */
public class DiagnosticsArrayView extends TableLayout implements NodeMain {

  /**
   * STALE is not part of the diagnostic_msgs/DiagnosticStatus message
   * definition.
   */
  private static final int STALE = 3;
  private static final String DIAGNOSTICS_AGGREGATOR_TOPIC = "/diagnostics_agg";

  private Drawable errorDrawable;
  private Drawable warningDrawable;
  private Drawable okDrawable;
  private Drawable staleDrawable;

  public DiagnosticsArrayView(Context context) {
    super(context);
    init();
  }

  public DiagnosticsArrayView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  private void init() {
    Resources resources = getResources();
    errorDrawable = resources.getDrawable(R.drawable.error);
    warningDrawable = resources.getDrawable(R.drawable.warn);
    okDrawable = resources.getDrawable(R.drawable.ok);
    staleDrawable = resources.getDrawable(R.drawable.stale);
  }

  @Override
  public GraphName getDefaultNodeName() {
    return GraphName.of("android_10/diagnostics_array_view");
  }

  @Override
  public void onStart(ConnectedNode connectedNode) {
    Subscriber<DiagnosticArray> subscriber =
        connectedNode.newSubscriber(DIAGNOSTICS_AGGREGATOR_TOPIC,
            diagnostic_msgs.DiagnosticArray._TYPE);
    subscriber.addMessageListener(new MessageListener<DiagnosticArray>() {
      @Override
      public void onNewMessage(final DiagnosticArray message) {
        final List<DiagnosticStatus> diagnosticStatusMessages = message.getStatus();
        post(new Runnable() {
          @Override
          public void run() {
            removeAllViews();
            for (final DiagnosticStatus diagnosticStatusMessage : diagnosticStatusMessages) {
              Button button = new Button(getContext());
              button.setText(diagnosticStatusMessage.getName());
              if (diagnosticStatusMessage.getLevel() == STALE) {
                button.setCompoundDrawablesWithIntrinsicBounds(staleDrawable, null, null, null);
              } else if (diagnosticStatusMessage.getLevel() == DiagnosticStatus.ERROR) {
                button.setCompoundDrawablesWithIntrinsicBounds(errorDrawable, null, null, null);
              } else if (diagnosticStatusMessage.getLevel() == DiagnosticStatus.WARN) {
                button.setCompoundDrawablesWithIntrinsicBounds(warningDrawable, null, null, null);
              } else {
                button.setCompoundDrawablesWithIntrinsicBounds(okDrawable, null, null, null);
              }
              addView(button);
            }
          }
        });
        postInvalidate();
      }
    });
  }

  @Override
  public void onError(Node node, Throwable throwable) {
  }

  @Override
  public void onShutdown(Node node) {
  }

  @Override
  public void onShutdownComplete(Node node) {
  }
}
