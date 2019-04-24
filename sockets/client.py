import socket
import sys
import time
import os
import threading

sys.path.insert(0, '../Lights')

from led import Lights

HOST = socket.gethostbyname("rp1")
PORT = 5210        # The port used by the server

light = Lights()

'''
Using the example code from the BlueZ libary to create an BLE advertisement
'''
def advertiseBLE():
    os.system("sudo /home/pi/bluez-5.43/test/example-advertisement") 

if __name__ == "__main__":
    advertise = threading.Thread(target=advertiseBLE)
    advertise.start()

    time.sleep(.5)
    printf("")

    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((HOST, PORT))
            #Telling the server what the hostname of this pi is
            name = socket.gethostname()
            s.send(name.encode())
            print("\nServer has been sent the hostname {:}.".format(name))
        except socket.error as socketerror:
            print("Error:", socketerror)
            print("Waiting to try again..\n.")
            time.sleep(3)
    

    while True:
        data = s.recv(1024)
        if not data:
            break

        data = data.split("|")
        color = data[0]
        brightness = data[1]

        light.setColor( color )
        light.setBrightness( brightness )

    print("Master Pi is down. Stopping...")
    quit()
