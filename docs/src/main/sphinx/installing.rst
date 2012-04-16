Installing android_core
=======================

These instructions assume that you have already completed the rosjava_core
:ref:`installation <rosjava_core:installing>` and :ref:`build
<rosjava_core:building>` instructions.

These instructions also assume you are using Ubuntu. However, the differences
between platforms should be minimal.

There are a few additional dependencies required for building android_core:

* the `Android SDK`_ and
* `Apache Ant`_ (e.g. on Ubuntu Lucid: ``sudo apt-get install ant1.8 ant1.8-optional``)

As with rosjava_core, the recommend installation procedure for android_core is
to use rosws. See the `rosws tutorial`_ for more information if you find the
following quick start instructions to be insufficient.

.. code-block:: bash

  cd ~/my_ros_workspace
  rosws merge http://android.rosjava.googlecode.com/hg/.rosinstall
  rosws update

.. note:: The rosws tool will remind you as well, but don't forget to source
  the appropriate, newly generated setup script.

If you would like to build the android_core documentation, you will also need
Pygments 1.5+.

.. code-block:: bash

  easy_install --prefix ~/.local -U pygments

.. _rosws tutorial: http://www.ros.org/doc/api/rosinstall/html/rosws_tutorial.html
.. _Apache Ant: http://ant.apache.org/
.. _Android SDK: http://developer.android.com/sdk/installing.html

