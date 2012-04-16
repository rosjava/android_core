Building android_core
=====================

android_core uses the `Gradle`_ build and `Apache Ant`_ build systems.
`rosmake`_ is not supported.

To build android_core, execute the `gradle wrapper`_:

You can build debug APKs for all android_core packages using `Gradle`_.

.. code-block:: bash

  roscd android_core
  ./gradlew debug

At this point, you may interact with your Android projects as described in the
`Android documentation`_.

Automatic generation of Eclipse project files is not currently supported. To
create an Eclipse project from an existing ROS Android package:

#. From Eclipse, create a new Android project from existing source (your
   package directory).
#. Add all the jars in the libs directory to your project's build path.

.. _Gradle: http://www.gradle.org/
.. _Apache Ant: http://ant.apache.org/
.. _rosmake: http://ros.org/wiki/rosmake/
.. _gradle wrapper: http://gradle.org/docs/current/userguide/gradle_wrapper.html
.. _Android documentation: http://developer.android.com/guide/developing/building/building-cmdline.html

