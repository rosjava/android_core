android_core
============

android_core is a collection of components and examples that are useful for
developing ROS applications on Android. It provides a base `Activity`_
(:javadoc:`org.ros.android.RosActivity`) and `Service`_
(:javadoc:`org.ros.android.NodeMainExecutorService`) for executing and managing
the lifecycle of your :javadoc:`org.ros.node.NodeMain`\s.

In addition, android_core defines the pattern of combining the Android `View`_
and :javadoc:`org.ros.node.NodeMain` concepts that enables the development of
data driven Android UIs. Several such RosViews (e.g.
:javadoc:`org.ros.android.view.RosTextView`,
:javadoc:`org.ros.android.view.RosImageView`,
and :javadoc:`org.ros.android.view.RosCameraPreviewView`) are provided.

Support is best found on http://answers.ros.org/.

Please file bugs and feature requests on the rosjava `issues`_ page. Starring
issues that are important to you will help developers prioritize their work.

In addition to the following documentation, android_core makes liberal use of
`Javadoc`_.

.. _issues: http://code.google.com/p/rosjava/issues/list
.. _Javadoc: javadoc/index.html
.. _Activity: http://developer.android.com/reference/android/app/Activity.html
.. _Service: http://developer.android.com/reference/android/app/Service.html
.. _View: http://developer.android.com/reference/android/view/View.html

Contents:

.. toctree::
   :maxdepth: 2

   installing
   building

