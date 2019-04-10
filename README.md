# Smart-Lighting

This project utilizes 3 Raspberry Pis each connected to its own light strip. As the user moves the watch around the room, it collects the Bluetooth signals from the master and two slave Raspberry Pi 3's. The watch then sends the strength information to the master Pi, which determines which Pi the user is closest to, and turns on the light that the Pi controls. The slave Pis will be placed in different rooms, with the master placed between them. The master and slaves will communicate to each other over WiFi using sockets, while the watch will communicate to the master over Bluetooth. At all times, the Pis will be broadcasting Bluetooth signals for the watch to read, but the watch will only ever be connected to the 
master Pi.

![Screenshot_20190409_133548](Screenshot_20190409_133548.png)

![Screenshot_20190409_133548](Screenshot_20190409_133649.png)
