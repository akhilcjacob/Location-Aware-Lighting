from gpiozero import LED
import pigpio
import time
pi = pigpio.pi()

class Lights(object):
    def __init__(self):
        self.redPin = 17
        self.red = LED(self.redPin)
        self.greenPin = 22
        self.green = LED(self.greenPin)
        self.bluePin = 24
        self.blue = LED(self.bluePin)
        self.red.off()
        self.green.off()
        self.blue.off()
        self.bright = 0
        self.color = "#000000"
        self.wait = 0.01

    def hexTOrgb(self, hexColor):
        hexColor = hexColor.lstrip('#')
        
        hlen = len(hexColor)
        return tuple(int(hexColor[i:i+hlen//3], 16) for i in range(0, hlen, hlen//3))

    def calculateStep(self, prev, end):
        step = end-prev
        if step:
            step = 1020.0/step
        return step

    def calculateVal(self, step, val, i):
        if (step) and (i % step == 0):
            if step > 0:
                val += 1
            elif step < 0:
                val -= 1
        if val > 255:
            val = 255
        elif val < 0:
            val = 0
        return val

    def colorFade(self, colorFrom, colorTo):
        r = colorTo[0]
        g = colorTo[1]
        b = colorTo[2]
        print(colorTo,colorFrom)
        stepR = self.calculateStep(colorFrom[0], r)
        print("red step:", stepR)
        stepG = self.calculateStep(colorFrom[1], g)
        print("grn step:", stepG)
        stepB = self.calculateStep(colorFrom[2], b)
        print("blu step:", stepB)

        redVal = colorFrom[0]
        grnVal = colorFrom[1]
        bluVal = colorFrom[2]
        for x in range(1021):
            redVal = self.calculateVal(stepR, redVal, x)
            grnVal = self.calculateVal(stepG, grnVal, x)
            bluVal = self.calculateVal(stepB, bluVal, x)

            pi.set_PWM_dutycycle(self.redPin, redVal)
            pi.set_PWM_dutycycle(self.greenPin, grnVal)
            pi.set_PWM_dutycycle(self.bluePin, bluVal)

            time.sleep(self.wait)

    def setBrightness(self, targetBrightness):
        rgb = self.hexTOrgb(self.color)
        redVal = rgb[0]*(targetBrightness/100)
        grnVal = rgb[1]*(targetBrightness/100)
        bluVal = rgb[2]*(targetBrightness/100)
        pi.set_PWM_dutycycle(self.redPin, redVal)
        pi.set_PWM_dutycycle(self.greenPin, grnVal)
        pi.set_PWM_dutycycle(self.bluePin, bluVal)

    def setColor(self, hexColor):
        rgb = self.hexTOrgb(hexColor)

        self.colorFade(self.hexTOrgb(self.color), rgb)
        print(self.color)
        self.color = hexColor

        #pi.set_PWM_dutycycle(self.redPin, r)
        #pi.set_PWM_dutycycle(self.greenPin, g)
        #pi.set_PWM_dutycycle(self.bluePin, b)

if __name__ == "__main__":
    l = Lights()
    while True:
        l.setColor('#0000ff')
        time.sleep(2)
        l.setColor('#ff0000')