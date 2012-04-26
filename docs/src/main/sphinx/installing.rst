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

  cd ~/my_workspace
  rosws merge http://android.rosjava.googlecode.com/hg/.rosinstall
  rosws update
  source setup.bash

.. note:: You should source the correct setup script for your shell (e.g.
  setup.bash for Bash or setup.zsh for Z shell).

If you would like to build the android_core documentation, you will also need
Pygments 1.5+ and Sphinx 1.1.3+.

.. code-block:: bash

  sudo pip install --upgrade sphinx Pygments

.. _rosws tutorial: http://www.ros.org/doc/api/rosinstall/html/rosws_tutorial.html
.. _Apache Ant: http://ant.apache.org/
.. _Android SDK: http://developer.android.com/sdk/installing.html

