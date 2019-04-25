import socket
import sys
import time
import os
import threading

sys.path.insert(0, '../Lights')

from led import Lights

HOST = "192.168.1.238" 
PORT = 5210        # The port used by the server

light = Lights()

current_color = None

'''
Using the example code from the BlueZ libary to create an BLE advertisement
'''
def advertiseBLE():
    os.system("sudo /home/pi/bluez-5.43/test/example-advertisement") 

if __name__ == "__main__":
    advertise = threading.Thread(target=advertiseBLE)
    advertise.start()

    time.sleep(.5)
    print("")

    while True:
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            s.connect((HOST, PORT))
            #Telling the server what the hostname of this pi is
            name = socket.gethostname()
            name = "pi{:}".format(name[-1])
            s.send(name.encode())
            print("\nServer has been sent the hostname {:}.".format(name))
            break
        except socket.error as socketerror:
            print("Error:", socketerror)
            print("Waiting to try again...\n")
            time.sleep(3)
    

    while True:
        data = s.recv(61).decode("UTF-8")
        if not data:
            break

        print("Received data: \"{:}\"".format(data))
        info = data.split("&")[-2]
        print("Using data", info)
        info = info.split("|")
        color = info[0]
        brightness = int(info[1])

        if color != current_color:
            light.setColor( color )
            current_color = color
        light.setBrightness( brightness )

    print("Master Pi is down. Stopping...")
    quit()

