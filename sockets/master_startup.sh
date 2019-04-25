#kill pigpiod if its running
sudo killall pigpiod

#Set up the environment and run the server
sudo pigpiod

export LC_ALL=C.UTF-8
export LANG=C.UTF-8
export FLASK_APP=master.py
flask run --host=0.0.0.0
