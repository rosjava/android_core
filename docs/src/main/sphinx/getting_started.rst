Getting started
===============

Before diving into ROS enabled Android application development, you should be
familiar with :ref:`rosjava <rosjava_core:getting_started>` and `Android
application development`_ in general.

.. _Android application development: http://developer.android.com/resources/tutorials/hello-world.html

Creating a new Android application
----------------------------------

TODO

Life of a RosActivity
---------------------

The :javadoc:`org.ros.android.RosActivity` class is the base class for all of
your ROS enabled Android applications. Let's consider the following example
from the android_tutorial_pubsub package. In this example, we create a
:javadoc:`org.ros.node.topic.Publisher` and a
:javadoc:`org.ros.node.topic.Subscriber` that will exchange "Hello, World"
messages.

.. literalinclude:: ../../../../android_tutorial_pubsub/src/org/ros/android/android_tutorial_pubsub/MainActivity.java
  :language: java
  :linenos:

On line 30, we extend :javadoc:`org.ros.android.RosActivity`.  When our
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

On line 38 we call the super constructor with two strings that become the title
and ticker message of an Android `notification`_. The user may tap on the
notification to shut down all ROS nodes associated with the application.

Lines 42-46 should look familiar to Android developers. We load the `activity`_
layout and get a reference to our
:javadoc:`org.ros.android.view.RosTextView<T>`. More on that later.

On line 58 we define the abstract method
:javadoc:`org.ros.android.RosActivity#init(org.ros.node.NodeMainExecutor)`.
This is where we kick off our :javadoc:`org.ros.node.NodeMain`\s and other
business logic.

And that's it. :javadoc:`org.ros.android.RosActivity` handles the rest of the
application's lifecycle management including:

* acquiring and releasing `wake and WiFi locks`_,
* binding and unbinding the `service`_,
* and shutting down :javadoc:`org.ros.node.NodeMain`\s when the application exits.

.. _wake and WiFi locks: http://developer.android.com/reference/android/os/PowerManager.html

Nodes and Views
---------------

TODO


