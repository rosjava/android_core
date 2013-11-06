.. _getting-started:

Getting started
===============

Before diving into ROS enabled Android application development, you should be
familiar with :ref:`rosjava <rosjava-core:getting-started>` and `Android
application development`_ in general. Note that any information regarding
command-line adt and eclipse development is depracating - we have moved early
to a gradle-`android studio`_ environment.

.. _Android application development: http://developer.android.com/training/index.html
.. _android studio: http://wiki.ros.org/android/Android Studio

Creating a new Android application
----------------------------------

Refer to the `RosWiki`_ for tutorials.

.. _RosWiki: http://wiki.ros.org/android

.. _life-of-a-rosactivity:

Using RosActivity
-----------------

The :javadoc:`org.ros.android.RosActivity` class is the base class for all of
your ROS enabled Android applications. Let's consider the following example
from the android_tutorial_pubsub package. In this example, we create a
:javadoc:`org.ros.node.topic.Publisher` and a
:javadoc:`org.ros.node.topic.Subscriber` that will exchange "Hello, World"
messages.

.. literalinclude:: ../../../../android_tutorial_pubsub/src/org/ros/android/android_tutorial_pubsub/MainActivity.java
  :language: java
  :linenos:
  :lines: 17-
  :emphasize-lines: 14,22,28-30,42

On line 14, we extend :javadoc:`org.ros.android.RosActivity`.  When our
`activity`_ starts, the :javadoc:`org.ros.android.RosActivity` super class will:

* start the :javadoc:`org.ros.android.NodeMainExecutorService` as a `service`_
  in the `foreground`_,
* launch the :javadoc:`org.ros.android.MasterChooser` activity to prompt the
  user to configure a master URI,
* and display an ongoing `notification`_ informing the user that ROS nodes are
  running in the background.

.. _activity: http://developer.android.com/reference/android/app/Activity.html
.. _service: http://developer.android.com/reference/android/app/Service.html
.. _foreground: http://developer.android.com/reference/android/app/Service.html#startForeground(int, android.app.Notification)
.. _notification: http://developer.android.com/reference/android/app/Notification.html

On line 22 we call the super constructor with two strings that become the title
and ticker message of an Android `notification`_. The user may tap on the
notification to shut down all ROS nodes associated with the application.

Lines 28-30 should look familiar to Android developers. We load the `activity`_
layout and get a reference to our
:javadoc:`org.ros.android.view.RosTextView` (more on that later).

On line 42 we define the abstract method
:javadoc:`org.ros.android.RosActivity#init(org.ros.node.NodeMainExecutor)`.
This is where we kick off our :javadoc:`org.ros.node.NodeMain`\s and other
business logic.

And that's it. :javadoc:`org.ros.android.RosActivity` handles the rest of the
application's lifecycle management including:

* acquiring and releasing `WakeLocks`_ and `WifiLocks`_,
* binding and unbinding the :javadoc:`org.ros.android.NodeMainExecutorService`,
* and shutting down :javadoc:`org.ros.node.NodeMain`\s when the application exits.

.. _WakeLocks: http://developer.android.com/reference/android/os/PowerManager.WakeLock.html
.. _WifiLocks: http://developer.android.com/reference/android/net/wifi/WifiManager.WifiLock.html

Nodes and Views
---------------

The android_core stack provides a number of Android `Views`_ which implement
:javadoc:`org.ros.node.NodeMain`. For example, let's look at the implementation
of :javadoc:`org.ros.android.view.RosTextView`. The intent of this view is
to display the textual representation of published messages.

.. literalinclude:: ../../../../android_10/src/org/ros/android/view/RosTextView.java
  :language: java
  :linenos:
  :lines: 17-36,50-
  :emphasize-lines: 40,49,56

The view is configured with a topic name, message type, and a
:javadoc:`org.ros.android.MessageCallable`. On line 40, in the
:javadoc:`org.ros.node.NodeMain#onStart(Node)` method, we create a new
:javadoc:`org.ros.node.topic.Subscriber` for the configured topic and message
type.

When a new message arrives, we either use the configured callable to transform
the incoming message to a string (line 49), or we use the default
``toString()`` method if no callable was configured (line 56). We then set the
text of the view to the string representation of the incoming message.

As with any other :javadoc:`org.ros.node.NodeMain`, the
:javadoc:`org.ros.android.view.RosTextView` must be executed by the
:javadoc:`org.ros.node.NodeMainExecutor`. In the :ref:`life-of-a-rosactivity`
example, we execute it in
:javadoc:`org.ros.android.RosActivity#init(NodeMainExecutor)` and use the it to
display incoming messages from the
:javadoc:`org.ros.rosjava_tutorial_pubsub.Talker` node.

.. _Views: http://developer.android.com/reference/android/view/View.html
.. _TextView: http://developer.android.com/reference/android/widget/TextView.html

