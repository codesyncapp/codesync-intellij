CodeSync Developer Guide
------------------------

This document contains information about CodeSync IntelliJ plugin and its development.


Getting Started:
----------------

In order to start the development you will need latest version of IntelliJ IDEA, you can download it from JetBrains Site.
It has a community edition which is more than enough for plugin development.

Dependencies
------------
Here are the dependencies for setting up a development environment for this plugin.

Java Version
------------
Java version required for this plugin can be found by looking at the `sourceCompatibility` attribute inside `build.gradle` file.
Make sure your system has that version installed before you start the development. You can see this from `Project Structure` in main menu commonly named `File`.

Java JDK to use is ``corretto-21`` i.e. ``Amazon Corretto version 21``

Gradle JVM
----------
You need to set Gradle JVM to ``Project SDK`` or same as the above Java JDK i.e. ``corretto-21`` and that can be set by opening "Preferences..." and then going to


``Build, Execution, Development > Build Tools > Gradle``


Run Configurations
------------------

There are already run configurations present and tracked with ``git`` so, you can simply run the appropriate configuration to start he project.

1. ``Run [runIde]``: This will start a new IDE instance with CodeSync plugin pre-installed from the current code.
2. ``1- codesync-intellij [buildPlugin]``: This will build the plugin code.
3. ``2- codesync-intellij [verifyPlugin]``: This one verifies the plugin code.
4. ``3- codesync-intellij [publishPlugin]``: This one is used to publish plugin on to the marketplace, it requires a key to be added by the developer.

Run [runIde]
============

This is the one that you will be mostly using, if you edit this configuration then you will see ``env=dev`` environment variable being set.
``env`` can have the following values.

1. ``dev``: This one means locally running server and webapp will be used.
2. ``prod``: This one mean production server and webapp will be used.
3. ``test``: This one is only for unit/integration tests.

Running Tests
=============
You can run the tests by running the ``codesync-intellij [test].run`` task from the Gradle tool window.

Note: All tests require the environment variable ``env=test`` to be set.
