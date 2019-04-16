import socket

HOST = socket.gethostbyname("rp1")
PORT = 5210        # The port used by the server

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect((HOST, PORT))
data = s.recv(1024)

print('Received', repr(data))
