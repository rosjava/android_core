Overview
========

The android_core stack is currently under active development. Consider all
APIs and documentation to be volatile.

`Javadoc <javadoc/index.html>`_ is used extensively and cross referenced from
this documentation.

ROS-enabling Android applications
----------------------------------

android_core provides a base `Activity`_
(:javadoc:`org.ros.android.RosActivity`) and `Service`_
(:javadoc:`org.ros.android.NodeMainExecutorService`) for executing
and managing the lifecycle of your :javadoc:`org.ros.node.NodeMain`\s.

In addition, android_core defines the pattern of combining the Android
`View`_ and :javadoc:`org.ros.node.NodeMain` concepts to enable the
development of data driven Android UIs. Several such RosViews (e.g.
:javadoc:`org.ros.android.view.RosTextView`,
:javadoc:`org.ros.android.view.RosImageView`, and
:javadoc:`org.ros.android.view.RosCameraPreviewView`) are provided.

.. _Activity: http://developer.android.com/reference/android/app/Activity.html
.. _Service: http://developer.android.com/reference/android/app/Service.html
.. _View: http://developer.android.com/reference/android/view/View.html
