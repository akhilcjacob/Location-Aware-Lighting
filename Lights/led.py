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
        self.bright = 80
        self.color = "#000000"
        self.wait = 0.003

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
        stepR = self.calculateStep(colorFrom[0], r)
        stepG = self.calculateStep(colorFrom[1], g)
        stepB = self.calculateStep(colorFrom[2], b)
        redVal = colorFrom[0]
        grnVal = colorFrom[1]
        bluVal = colorFrom[2]
        for x in range(1021):
            redVal = self.calculateVal(stepR, redVal, x)
            grnVal = self.calculateVal(stepG, grnVal, x)
            bluVal = self.calculateVal(stepB, bluVal, x)
            pi.set_PWM_dutycycle(self.redPin, redVal*(self.bright/100.0))
            pi.set_PWM_dutycycle(self.greenPin, grnVal*(self.bright/100.0))
            pi.set_PWM_dutycycle(self.bluePin, bluVal*(self.bright/100.0))
            time.sleep(self.wait)

    def setBrightness(self, targetBrightness):
        rgb = self.hexTOrgb(self.color)
        while (targetBrightness != self.bright):
            if (targetBrightness > self.bright) and (targetBrightness-self.bright > 2):
                self.bright = self.bright + 2
            elif (targetBrightness < self.bright) and (self.bright-targetBrightness > 2):
                self.bright = self.bright - 2
            else:
                self.bright = targetBrightness
            if self.bright > 100:
                self.bright = 100
            elif self.bright < 0:
                self.bright = 0
            redVal = (rgb[0]*(self.bright/100.0))
            grnVal = (rgb[1]*(self.bright/100.0))
            bluVal = (rgb[2]*(self.bright/100.0))
            pi.set_PWM_dutycycle(self.redPin, redVal)
            pi.set_PWM_dutycycle(self.greenPin, grnVal)
            pi.set_PWM_dutycycle(self.bluePin, bluVal)
            #time.sleep(0.05)

    def setColor(self, hexColor):
        rgb = self.hexTOrgb(hexColor)
        self.colorFade(self.hexTOrgb(self.color), rgb)
        self.color = hexColor

if __name__ == "__main__":
 l = Lights()
 while True:
     l.setColor('#ff0000')
     l.setBrightness(60)
     l.setBrightness(50)
 time.sleep(3)
