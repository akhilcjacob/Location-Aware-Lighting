# master.py
import socket
import os
import time
import json
import queue
import SignalQueue
import requests
import threading
from flask import Flask
from flask import request
from flask import jsonify

import sys
sys.path.insert(0, '../Lights')

from led import Lights

PORT = 5210

QUEUE_SIZE = 10
MAX_SIG_VARIANCE = .50	#Max a signal strength can vary from the average

clients = []

signalServer = Flask(__name__)

#Holds the historical signal data for each pi
signals = {
	"pi1": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE),
	"pi2": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE),
	"pi3": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE)
} 

#The light that the master pi controls
light = Lights()

'''
Gets the IP from the command line by parsing out ifconfig data.
'''
def getIP():
	ip = os.popen('ifconfig wlan0 | grep "inet\ "').read().strip().split(" ") 
	if len(ip) > 1:
		return ip[1]
	return -1

'''
Creates the TCP server for the master to use to send information.
Args:
	HOST: An IPv4 address of the master PI (the local pi's network address)
'''
def startServer( HOST ):
    print("Creating host server {:}:{:}".format( HOST, PORT ))
    master = socket.socket( socket.AF_INET, socket.SOCK_STREAM )
    master.bind( (HOST, PORT) )

    print("Server established on port {:}.".format( PORT ))
    return master

'''
Binds the client Pis to the master.
Args:
	server: A socket server that is bound to a port and host
'''
def bindClients( server ):
	print("\nServer listening for a client.")
	server.listen()

	dots = 0
	while True:
		try:
			server.settimeout(1)
			conn, addr = server.accept()
		except socket.timeout:
			print("Waiting for connection.{:}   ".format('.'*dots), end="\r")
			if dots == 2:
				dots = 0
			else:
				dots += 1
			pass
		except:
			raise
		else:
			print("Connected to {:}".format(addr))
			server.settimeout(60)
			name = conn.recv(1024)
			clients.append( (conn, name) )
			return

'''
Updates the global signal queues for the given pi hostname
Args:
	pi: An integer defining which pi hostname to look at (e.g., pi1, pi2, pi3)
	strength: A float that is the signal strength to be added to the queues
Returns: True if the value was added, False if skipping this value
'''
def updateQueue( pi, strength ):
	currentPi = "pi{:}".format(pi)

	# If the value is outside the margin of error in terms of the previous signals gathered
	if signals[currentPi].isOutlier(strength):
		# We will skip adding that value to the queue
		return False

	#Otherwise, we will add the value to the queue
	#Make room for the new value if there isn't any
	if signals[currentPi].queueFull(): 
		signals[currentPi].popFront()

	signals[currentPi].put(strength)
	return True

'''
Returns a list of the Pi hostnames in terms of the highest signal strength
Args:
	signals: A dict with signal strengths as the keys and pi hostnames as the val
'''
def getSignalRanking( signals ):
	ranks = [None, None, None]
	ranks[0] = signals[ max(signals.keys()) ]
	ranks[2] = signals[ min(signals.keys()) ]

	#Adding the final one in the middle spot
	if "rp1" not in ranks:
		ranks[1] = "rp1"
	elif "rp2" not in ranks:
		ranks[1] = "rp2"
	else:
		ranks[1] = "rp3"

	return ranks

'''
Figures out which PI is closest to the watch to adjust lights.
Args:
	data: A JSON string containing the color and brightness information
		for each of the lights
'''
def parseSignals( load ):
	color = load["color"]
	brightness = load["brightness"]

	newSignal = {}
	for pi in [1,2,3]:
		currentPi = "pi{:}".format(pi)
		newSignal[load[currentPi]] = currentPi 

	successful = False
	pi = 1
	#Running through the 3 Pi's hostnames
	will_update = [False, False, False]
	for pi in [1,2,3]:
		will_update[pi] = updateQueue(pi, load[currentPi])

		#VERIFY THIS IS OK ===============================================================
		#If we will not update, take the most recent signal level and use that instead
		if not will_update[pi]:
			most_recent = signals[currentPi].peekLast()
			newSignal.pop( load[currentPi] )
			newSignal[most_recent] = currentPi

	ranks = getSignalRanking(newSignal)
	return color, brightness, ranks

'''
Sending the brightness values to the client Pis for them to adjust
their LEDs appropriately
'''
def distributeBrightness( signalOrder, color, brightness ):
	for client in clients:
		divisor = 1
		if (signalOrder[1] == client[1]): divisor = 2
		if (signalOrder[2] == client[1]): divisor = brightness
		client[0].sendall(b'{:}|{:}'.format(color, brightness/divisor))

'''
Using the example code from the BlueZ libary to create an BLE advertisement
'''
def advertiseBLE():
	print("\nSTARTING BLE ADVERTISEMENT")
	os.system("sudo /home/pi/bluez-5.43/test/example-advertisement")
	print("\n")

'''
When we get a new HTTP request, we can update the lighting
'''
def handleEvent( signalData ):	
	color, brightness, signals = parseSignals( signalData )

	#Changing lighting and telling the clients to do as well
	divisor = 1
	if ( signals[1] == "rp1"): divisor = 2
	if ( signals[2] == "rp1"): divisor = brightness

	light.setColor( color )
	light.setBrightness( brightness/divisor )

	distributeBrightness( signals, color, brightness )


@signalServer.route("/", methods=['GET', 'POST'])
def home():
	print("Data received.")

	signalData = request.get_json()
	print(signalData)
	
	handleEvent( signalData )
	return jsonify(success=True)


'''
Runs the code to set up and process signal data
'''
def init():
	HOST = getIP()

	if HOST == -1: sys.exit(1)

	master = startServer( HOST )

	advertise = threading.Thread(target=advertiseBLE)
	advertise.start()
	
	time.sleep(1)
	print("BLE Advertisement running.")

	# Sit and wait until both clients connect
	while ( len(clients) < 2 ):
		bindClients(master)
		print("Got one")

	print("Got both client Pi's, now handling lighting control...")
init()

if __name__ == "__main__":
	print("Starting up flask server for signal data...")
	signalServer.run()
	
