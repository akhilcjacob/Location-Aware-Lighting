#kill pigpiod if its running
sudo killall pigpiod

#Run the client code
sudo pigpiod
python3 client.py
