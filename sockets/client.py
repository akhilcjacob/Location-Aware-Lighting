import socket
from led import Lights

HOST = socket.gethostbyname("rp1")
PORT = 5210        # The port used by the server

light = Lights()

'''
Using the example code from the BlueZ libary to create an BLE advertisement
'''
def advertiseBLE():
    exec(open("~/bluez-5.43/test/example-advertisement").read())

if __name__ == "__main__":
    advertise = thread.Thread(target=advertiseBLE)
    advertise.start()

    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((HOST, PORT))
    #Telling the server what the hostname of this pi is
    s.send(b'{:}'.format(socket.gethostname()))

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
