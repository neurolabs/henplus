/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.Interruptable;

/**
 * A thread to be used to cancel a statement running in another thread.
 */
final class StatementCanceller implements Runnable, Interruptable {
    private final CancelTarget _target;
    private boolean _armed;
    private boolean _running;
    private volatile boolean _cancelStatement;

    /**
     * The target to be cancelled. Must not throw an Execption and may to
     * whatever it needs to do.
     */
    public interface CancelTarget {
        void cancelRunningStatement();
    }

    public StatementCanceller(final CancelTarget target) {
        _cancelStatement = false;
        _armed = false;
        _running = true;
        _target = target;
    }

    /** inherited: interruptable interface */
    public void interrupt() {
        _cancelStatement = true;
        /*
         * watch out, we must not call notify, since we are in the midst of a
         * signal handler
         */
    }

    public synchronized void stopThread() {
        _running = false;
        notify();
    }

    public synchronized void arm() {
        _armed = true;
        _cancelStatement = false;
        notify();
    }

    public synchronized void disarm() {
        _armed = false;
        _cancelStatement = false;
        notify();
    }

    public synchronized void run() {
        try {
            for (;;) {
                while (_running && !_armed) {
                    wait();
                }
                if (!_running) {
                    return;
                }
                while (_armed && !_cancelStatement) {
                    wait(300);
                }
                if (_cancelStatement) {
                    try {
                        _target.cancelRunningStatement();
                    } catch (final Exception e) {
                        /* ignore */
                    }
                    _armed = false;
                }
            }
        } catch (final InterruptedException e) {
            return;
        }
    }
}
