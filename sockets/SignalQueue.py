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
        return self.itemCount

    def queueFull(self):
        if self.itemCount >= self.maxsize:
            return True
        return False

    def put(self, val):
        self.queue.append(val)
        self.itemCount += 1
        if self.itemCount > self.maxsize:
            self.popFront()
            self.itemCount -= 1

    def peekAt(self, loc):
        if self.itemCount > 0 and loc < self.itemCount:
            return self.queue[loc]
        else:
            return None

    def peek(self):
        return self.peekAt(0)

    def peekLast(self):
        return self.peekAt( self.itemCount-1 )

    def averageQueue(self):
        if self.itemCount > 0:
            return sum(self.queue)/self.itemCount
        else:
            return 0

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

