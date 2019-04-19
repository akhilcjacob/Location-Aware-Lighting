# master.py
import socket
import sys
import os
import time
import json
import queue
import SignalQueue

PORT = 5210

QUEUE_SIZE = 10
MAX_SIG_VARIANCE = .50	#Max a signal strength can vary from the average

clients = []

#Holds the historical signal data for each pi
signals = {
	"pi1": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE),
	"pi2": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE),
	"pi3": SignalQueue.SignalQueue(QUEUE_SIZE, MAX_SIG_VARIANCE)
} 

'''
Gets the IP from the command line by parsing out ifconfig data.
'''
def getIP():
	ip = os.popen('ifconfig lo0 | grep "inet\ "').read().strip().split(" ") 
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
	print("Server listening for a client.")
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
			clients.append( conn )
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
def parseSignals( data ):
	load = json.loads( data )
	color = load["color"]

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
	return ranks

'''
Runs the code to set up and process signal data
'''
def main():
	HOST = getIP()

	if HOST == -1: sys.exit(1)

	master = startServer( HOST )

	# Sit and wait until both clients connect
	while ( len(clients) < 2 ):
		bindClients(master)
		print("Got one")

	print("Got both client Pi's, now handling lighting control...")

	# Infinite Loop
	while (True):
		time.sleep(5)
		for client in clients:
			client.sendall(b'Here is some data for you!\n')

		watchData = receiveSignals()
		signals = parseSignals( watchData )
		#distributeBrightness()

# RUNNING MAIN
main()