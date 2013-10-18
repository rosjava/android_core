Overview
========

The android_core stack is currently under active development. Consider all
APIs and documentation to be volatile.

`Javadoc <javadoc/index.html>`_ is used extensively and cross referenced from
this documentation.

ROS-enabling Android applications
----------------------------------

android_core provides `Android Library Projects`_ to help you write ROS
applications for Android. The library projects are named for the `Android API
level`_ they require (e.g. android_10 and android_15).
Each class or feature is defined in the library project that represents the
minimum version of Android required for it to work.

Your application can depend on multiple library projects. This allows you to
easily target your application for different API levels.

Beyond specific features, android_core defines the pattern of combining the
Android `View`_ and :javadoc:`org.ros.node.NodeMain` concepts to enable the
development of data driven Android UIs (e.g.
:javadoc:`org.ros.android.view.RosTextView`).

.. _Android Library Projects: http://developer.android.com/guide/developing/projects/index.html#LibraryProjects
.. _Android API level: http://developer.android.com/guide/appendix/api-levels.html
.. _View: http://developer.android.com/reference/android/view/View.html

android_10 library project
---------------------------------------

android_10 (API level 10) is the lowest API level supported. It
provides the base `Activity`_ (:javadoc:`org.ros.android.RosActivity`) and
`Service`_ (:javadoc:`org.ros.android.NodeMainExecutorService`) for executing
and managing the lifecycle of your :javadoc:`org.ros.node.NodeMain`\s.

A few of the other features provided include:

* camera publisher
* image view
* orientation publisher

.. _Activity: http://developer.android.com/reference/android/app/Activity.html
.. _Service: http://developer.android.com/reference/android/app/Service.html

android_15 library project
-------------------------------------

android_15 (API level 13) provides features that require multitouch
and other APIs that are only available in devices with Android Honeycomb MR2 or
higher.

A few of the features provided include:

* 2D mapping and navigation
* 2D laser scan visualization
* virtual joystick
