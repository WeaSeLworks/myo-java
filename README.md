myo-java
========

Java Bindings for Myo
---------------------

Java language bindings for the Myo Developer Kit.

OSX
----------------------
To get started with OSX it is necessary to create a directory in /var/local and set the permissions
as follows.


    bash$ sudo mkdir /var/lock
    bash$ sudo chmod a+rw /var/lock


Running the application
----------------------

To run the application execute the command below in the root of the project

    bash$ ./gradlew run

This will start the application and connect to the Myo bluetooth dongle if present.
Currently the device name is hardcoded to /dev/tty.usbmodem1 this is correct for OSX but hasn't been tested on other OSs.