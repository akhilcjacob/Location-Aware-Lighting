'''
A class that manages a single queue of length 10, which are used to
store historical signal levels to help ensure data accuracy in the face
of unexpected events like data spikes.
'''
class Full(Exception):
    pass

class SignalQueue(object):
    def __init__(self, maxsize = 10, tolerance = 0.50):
        self.queue = []
        self.itemCount = 0
        self.maxsize = maxsize
        self.tolerance = tolerance

    def __len__(self):
        return itemCount

    def queueFull(self):
        if self.itemCount >= self.maxsize:
            return True
        return False

    def put(self, val):
        if self.itemCount < MAX_SIZE:
            self.queue[self.itemCount] = val
            self.itemCount += 1
        else:
            raise Full

    def peekAt(self, loc):
        return self.queue[loc]

    def peek(self):
        return self.peekAt(0)

    def peekLast(self):
        return self.peekAt( self.itemCount-1 )

    def averageQueue(self):
        return sum(self.queue)/self.itemCount

    def isOutlier(self, val):
        avg = self.averageQueue()
        upper = avg + (avg * self.tolerance)
        lower = avg - (avg * self.tolerance)

        if val <= upper and val >= lower:
            return False
        else:
            return True

    def popFront(self):
        self.queue = self.queue[1:]
        self.itemCount -= 1

