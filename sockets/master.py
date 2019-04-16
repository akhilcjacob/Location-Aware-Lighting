# master.py
import socket
import sys
import os
import time

PORT = 5210

clients = []

'''
Gets the IP from the command line by parsing out ifconfig data
'''
def getIP():
	ip = os.popen('ifconfig wlan0 | grep "inet\ "').read().strip().split(" ") 
	if len(ip) > 1:
		return ip[1]
	return -1

'''
Creates the TCP server for the master to use to send information
'''
def startServer( HOST ):
    print("Creating host server {:}:{:}".format( HOST, PORT ))
    master = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
    master.bind( (HOST, PORT) )

    print("Server established on port {:}.".format( PORT ))
    return master

'''
Binds the client Pis to the master
'''
def bindClients( server ):
    print("Server listening for a client.")
    server.listen()
    conn, addr = server.accept()

    print("Connected to {:}".format(addr))
    clients.append( conn )

if __name__ == "__main__":
	HOST = getIP()
	
	if HOST == -1: sys.exit(1)

	master = startServer( HOST )

	while ( len(clients) < 2 ):
		bindClients(master)
		print("Got one")

	print("Got both client Pi's, now handling lighting control...")

    # Infinite Loop
	while (True):
		time.sleep(5)
		for client in clients:
			client.sendall(b'Here is some shit\n')
		#receiveSignals()
		#parseSignals()
		#distributeBrightness()
