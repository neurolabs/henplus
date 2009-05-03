/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.HenPlus;
import henplus.view.util.CancelWriter;

/**
 * After arming, this runnable will display the current time after some timeout.
 * Used in long running SQL-statements to show a) how long it took so far b)
 * keep terminal sessions open that are otherwise being closed by some firewalls
 * :-)
 * 
 * @author hzeller
 * @version $Revision: 1.1 $
 */
public class LongRunningTimeDisplay implements Runnable {
    private final long _startTimeDisplayAfter;
    private final String _message;
    private final CancelWriter _timeDisplay;
    private long _lastArmTime;
    private volatile boolean _running;
    private volatile boolean _armed;

    public LongRunningTimeDisplay(final String msg, final long showAfter) {
        _startTimeDisplayAfter = showAfter;
        _message = msg;
        _running = true;
        _armed = false;
        _timeDisplay = new CancelWriter(HenPlus.msg());
    }

    public synchronized void arm() {
        _lastArmTime = System.currentTimeMillis();
        _armed = true;
        notify();
    }

    public synchronized void disarm() {
        if (_armed) {
            _armed = false;
            _timeDisplay.cancel();
            notify();
        }
    }

    public synchronized void stopThread() {
        _running = false;
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
                final long startDisplayAt = _lastArmTime + _startTimeDisplayAfter;
                while (_armed && System.currentTimeMillis() < startDisplayAt) {
                    wait(300);
                }
                while (_armed) {
                    long totalTime = System.currentTimeMillis() - _lastArmTime;
                    totalTime -= totalTime % 1000; // full seconds.
                    final String time = TimeRenderer.renderTime(totalTime);
                    _timeDisplay.cancel();
                    _timeDisplay.print(_message + " " + time);
                    wait(5000);
                }
                _timeDisplay.cancel();
            }
        } catch (final InterruptedException e) {
            return;
        }
    }
}
