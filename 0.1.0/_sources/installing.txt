Installing
==========

These instructions assume that you have already completed the rosjava_core
:ref:`installation <rosjava_core:installing>` and :ref:`build
<rosjava_core:building>` instructions.

Prerequisites
-------------

There are a few additional dependencies required for building android_core:

* `Android Studio & SDK`_

If you would like to build the android_core documentation, you will also need
Pygments 1.5+ and Sphinx 1.1.3+.

.. code-block:: bash

  sudo pip install --upgrade sphinx Pygments

Non-ROS Installation
--------------------

As with rosjava_core, this repository no longer requires a ros environment to be
installed. In this case, 

you simply need to clone the github repository

.. code-block:: bash

  git clone https://github.com/rosjava/android_core
  git checkout -b hydro origin/hydro

and proceed immediately to the section on :ref:`building`.

ROS Installation
----------------

If you would like a full ros environment backending your installation (you might
be building rosjava packages at the same time) then refer to the `RosWiki`_
for more details.

.. _RosWiki: http://wiki.ros.org/android
.. _Android Studio & SDK: http://wiki.ros.org/android/Android%20Studio/Download

