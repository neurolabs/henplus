/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ProgressWriter.java,v 1.2 2005-03-25 15:39:44 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import henplus.OutputDevice;
import henplus.commands.TimeRenderer;

/**
 * A utility class that can write the progress of an operation to the screen.
 */
public class ProgressWriter {
    private final static int DEFAULT_SCREEN_WIDTH = 65;

    /** min time before presenting an eta */
    private final static long MIN_ETA_RUNNING_TIME = 30 * 1000L;
    /** min time between two eta updates */
    private final static long MIN_ETA_DIFF_TIME = 1 * 1000L;

    private final long _expectedTargetValue;
    private final OutputDevice _out;
    private final long _startTime;
    private final CancelWriter _etaWriter;

    private long _lastEtaUpdate;

    private int _progressDots;
    private int _screenWidth;

    public ProgressWriter(final long expectedTargetValue, final OutputDevice out) {
        _expectedTargetValue = expectedTargetValue;
        _out = out;
        _progressDots = 0;
        _startTime = System.currentTimeMillis();
        _lastEtaUpdate = -1;
        _etaWriter = new CancelWriter(_out);
        setScreenWidth(DEFAULT_SCREEN_WIDTH);
    }

    public void setScreenWidth(final int screenWidth) {
        _screenWidth = screenWidth;
    }

    public int getScreenWidth() {
        return _screenWidth;
    }

    public void update(final long value) {
        if (_expectedTargetValue > 0 && value <= _expectedTargetValue) {
            final long newDots = _screenWidth * value / _expectedTargetValue;
            if (newDots > _progressDots) {
                _etaWriter.cancel(false);
                while (_progressDots < newDots) {
                    _out.print(".");
                    ++_progressDots;
                }
                _out.flush();
            }
            writeEta(value);
        }
    }

    public void finish() {
        _etaWriter.cancel();
    }

    private void writeEta(final long value) {
        if (!_etaWriter.isPrinting()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final long runningTime = now - _startTime;
        if (runningTime < MIN_ETA_RUNNING_TIME) {
            return;
        }
        final long lastUpdateDiff = now - _lastEtaUpdate;
        if (!_etaWriter.hasCancellableOutput()
                || lastUpdateDiff > MIN_ETA_DIFF_TIME) {
            final long etaTime = _expectedTargetValue * runningTime / value;
            final long rest = etaTime - runningTime;
            _etaWriter.print("ETA: " + TimeRenderer.renderTime(rest));
            _lastEtaUpdate = now;
        }
    }
}
