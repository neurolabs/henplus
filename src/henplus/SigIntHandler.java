/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SigIntHandler.java,v 1.6 2002-10-09 17:41:55 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.Stack;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Signal handler, that reacts on CTRL-C.
 */
public class SigIntHandler implements SignalHandler {
    private boolean once;
    private static SigIntHandler instance = null;
    private final Stack toInterruptStack;

    public static SigIntHandler install() {
	Signal interruptSignal = new Signal("INT"); // Interrupt: Ctrl-C
        instance = new SigIntHandler();
	// don't care about the original handler.
	Signal.handle(interruptSignal, instance);
	return instance;
    }
    
    public static SigIntHandler getInstance() {
	return instance;
    }

    public SigIntHandler() {
	once = false;
        toInterruptStack = new Stack();
    }
    
    public void pushInterruptable(Interruptable t) {
	toInterruptStack.push(t);
    }
    
    public void popInterruptable() {
        once = false;
        toInterruptStack.pop();
    }

    public void reset() {
	once = false;
	toInterruptStack.clear();
    }

    public void handle(Signal sig) {
	if (once) {
	    // got the interrupt twice. Just exit.
	    return;
	}

	once = true;
	if (!toInterruptStack.empty()) {
            Interruptable toInterrupt = (Interruptable)toInterruptStack.peek();
	    // this doesn't work, since the JDBC driver is not in a 'wait()'
	    //System.err.println("try to interrupt: " + toInterrupt);
	    toInterrupt.interrupt();
	    toInterrupt = null;
	}
	else {
	    System.err.println("[Ctrl-C ; interrupted]");
	    System.exit(1);
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
