#master.py
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
#sys.path.insert(0, '../Lights')

#from led import Lights

PORT = 5210

QUEUE_SIZE = 5
MAX_SIG_VARIANCE = 20	#Max a signal strength can vary from the average

clients = []

signalServer = Flask(__name__)

#Holds the historical signal data for each pi
signals = {
	"pi1": SignalQueue.SignalQueue(maxsize=QUEUE_SIZE, tolerance=MAX_SIG_VARIANCE),
	"pi2": SignalQueue.SignalQueue(maxsize=QUEUE_SIZE, tolerance=MAX_SIG_VARIANCE),
	"pi3": SignalQueue.SignalQueue(maxsize=QUEUE_SIZE, tolerance=MAX_SIG_VARIANCE)
}
update_count = [0,0,0]

#The light that the master pi controls
#light = Lights()

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
			name = conn.recv(1024).decode("UTF-8")
			clients.append( (conn, name))
			return

'''
Updates the global signal queues for the given pi hostname
Args:
	pi: An integer defining which pi hostname to look at (e.g., pi1, pi2, pi3)
	strength: A float that is the signal strength to be added to the queues
Returns: True if the value was added, False if skipping this value
'''
def updatedQueue( pi, strength ):
	currentPi = "pi{:}".format(pi)

	# If the value is outside the margin of error in terms of the previous signals gathered
	if len(signals[currentPi]) > QUEUE_SIZE//2:	#If there is anything to compare it to
		if signals[currentPi].isOutlier(strength):
			# We will skip adding that value to the queue
			return False

	signals[currentPi].put(strength)
	return True

'''
Returns a list of the Pi hostnames in terms of the highest signal strength
Args:
	signals: A dict with signal strengths as the keys and pi hostnames as the val
'''
def getSignalRanking( averages ):
	ranks = [None, None, None]

	#ranks[0] = max( list( (signals[n].averageQueue(), n) for n in signals.keys() ))[1]
	#ranks[2] = min( list( (signals[n].averageQueue(), n) for n in signals.keys() ))[1]

	ranks[0] = "pi{:}".format( averages.index( max( averages ))+1 )
	ranks[2] = "pi{:}".format( averages.index( min( averages ))+1 )

	#Adding the final one in the middle spot
	if "pi1" not in ranks:
		ranks[1] = "pi1"
	elif "pi2" not in ranks:
		ranks[1] = "pi2"
	elif "pi3" not in ranks:
		ranks[1] = "pi3"

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

	updates = [load["pi1_update"], load["pi2_update"], load["pi3_update"]]
	averages = [load["pi1_avg"], load["pi2_avg"], load["pi3_avg"]]

	newSignal = {}
	currentPi = "pi{:}"

	#Running through the 3 Pi's hostnames
	for pi in [1,2,3]:
		cPi = currentPi.format(pi)
		#if updates[pi-1] != update_count[pi-1]:
		#	update_count[pi-1] = updates[pi-1]

		strength = averages[pi-1]
		newSignal[strength] = cPi 

			#updated = updatedQueue(pi, strength)

			#print("updated pi{:} =".format(pi), updated)

			#VERIFY THIS IS OK ===============================================================
			#If we will not update, take the most recent signal level and use that instead
			#if not updated:
			#	most_recent = signals[cPi].peekLast()
			#	newSignal.pop( strength )
			#	newSignal[most_recent] = cPi
		#else:
			#newSignal[pi-1] =

	ranks = getSignalRanking( averages )
	return color, brightness, ranks

'''
Sending the brightness values to the client Pis for them to adjust
their LEDs appropriately
'''
def distributeBrightness( signalOrder, color, brightness ):
	#Changing telling the clients to do change their lights

	print("signalOrder = ", signalOrder)

	for client in clients:
		if (signalOrder[0] == client[1]):
			print("Sending {:} data: color = {:}, brightness = {:}".format(client[1], color, brightness))
			client[0].sendall(('{:}|{:03d}&'.format(color, brightness).encode() ))
			continue
		elif (signalOrder[1] == client[1]): 
			print("Sending {:} data: color = {:}, brightness = {:}".format(client[1], color, brightness//10))
			client[0].sendall(('{:}|{:03d}&'.format(color, brightness//10).encode() ))
			continue
		elif (signalOrder[2] == client[1]): 
			print("Sending {:} data: color = {:}, brightness = {:}".format(client[1], color, 0))
			client[0].sendall(('{:}|{:03d}&'.format(color, 0).encode() ))

	print()

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

	# Sit and wait until all three clients connect
	while ( len(clients) < 3 ):
		bindClients(master)

	print("Got all client Pi's, now handling lighting control...")
init()

if __name__ == "__main__":
	print("Starting up flask server for signal data...")
	signalServer.run()
	
