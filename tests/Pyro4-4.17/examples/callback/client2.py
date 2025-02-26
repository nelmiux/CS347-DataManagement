from __future__ import print_function
import Pyro4

class CallbackHandler(object):
    def crash(self):
        a=1
        b=0
        return a//b
    def call1(self):
        print("callback 1 received from server!")
        print("going to crash - you won't see the exception here, only on the server")
        return self.crash()
    @Pyro4.callback
    def call2(self):
        print("callback 2 received from server!")
        print("going to crash - but you will see the exception here too")
        return self.crash()


daemon=Pyro4.core.Daemon()
callback=CallbackHandler()
daemon.register(callback)

with Pyro4.core.Proxy("PYRONAME:example.callback2") as server:
    server._pyroOneway.add("doCallback")
    server.doCallback(callback)

print("waiting for callbacks to arrive...")
print("(ctrl-c/break the program once it's done)\n")
daemon.requestLoop()
