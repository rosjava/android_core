Building android_core
=====================

android_core uses the `Gradle`_ build and `Apache Ant`_ build systems.
`rosmake`_ is not supported.

To build android_core, execute the `gradle wrapper`_:

.. _Gradle: http://www.gradle.org/
.. _Apache Ant: http://ant.apache.org/
.. _rosmake: http://ros.org/wiki/rosmake/
.. _gradle wrapper: http://gradle.org/docs/current/userguide/gradle_wrapper.html

Before building ROS applications for Android, you must complete the
instructions for `building rosjava_core`_. Once you have completed that
successfully, you may proceed as follows.

.. _building rosjava_core: http://docs.rosjava.googlecode.com/hg/rosjava_core/html/building.html

The prerequisites for building android_core are the Android SDK and `Apache Ant`_.

* Install the `Android SDK`_.
* Install `Apache Ant`_ (e.g. on Ubuntu Lucid: ``sudo apt-get install ant1.8 ant1.8-optional``)

.. _Android SDK: http://developer.android.com/sdk/installing.html

Then, for each project in android_core you have to create a local.properties
file. This will be automated in the future.

#. roscd android_xxx
#. $ANDROID_SDK/tools/android update project -p \`pwd\`

Finally, you can build debug APKs for all android_core packages using `Gradle`_.

#. roscd android_core
#. ./gradlew debug

At this point, you may interact with your Android projects as described in the `Android documentation`_.

.. _Android documentation: http://developer.android.com/guide/developing/building/building-cmdline.html

Automatic generation of Eclipse project files is not currently supported. To
create an Eclipse project from an existing ROS Android package:

#. From Eclipse, create a new Android project from existing source (your
   package directory).
#. Add all the jars in the libs directory to your project's build path.

