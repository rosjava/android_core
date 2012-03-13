Building android_core
=====================

android_core uses the `Gradle`_ build and `Apache Ant`_ build systems.
`rosmake`_ is not supported.

To build android_core, execute the `gradle wrapper`_:

.. _Gradle: http://www.gradle.com/
.. _Apache Ant: http://ant.apache.org/
.. _rosmake: http://ros.org/wiki/rosmake/
.. _gradle wrapper: http://gradle.org/docs/current/userguide/gradle_wrapper.html

Note that the build process currently involves extra steps that will be folded
into Gradle tasks or otherwise eliminated.

Before building ROS applications for Android, you must complete the
instructions for building rosjava_core. Once you have completed that
successfully, you may proceed as follows.

For each android_core package you're interested in (e.g. foo):

#. rosrun rosjava_bootstrap install_generated_modules.py foo
#. ./gradlew foo:debug

Automatic generation of Eclipse project files is not currently supported. To
create an Eclipse project from an existing ROS Android package:

#. From Eclipse, create a new Android project from existing source (your
   package directory).
#. Add all the jars in the libs directory to your project's build path.

