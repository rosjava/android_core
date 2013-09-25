.. _building:

Building
========

android_core uses the `Gradle`_ build system in tandem with an external maven
repository which supplies dependencies (.jar's and .aar's).

To build debug APKs for all android_core packages, execute the `gradle wrapper`_.

.. code-block:: bash

  cd android_core
  ./gradlew assemble

You may deploy the android libraries (.aar's) to your local maven repository so
that other android packages outside android_core can use them with:

.. code-block:: bash
  ./gradlew publishToMavenLocal

To build the documentation, you may execute the docs task:

.. code-block:: bash

  ./gradlew docs

At this point, you may interact with your Android projects via Android Studio as described
in the `RosWiki`_ pages.

.. _RosWiki: http://wiki.ros.org/android/Android Studio
.. _Gradle: http://www.gradle.org/
.. _gradle wrapper: http://gradle.org/docs/current/userguide/gradle_wrapper.html
.. _Android documentation: http://developer.android.com/guide/developing/building/building-cmdline.html

