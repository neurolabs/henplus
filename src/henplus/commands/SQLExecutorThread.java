/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.HenPlus;
import henplus.SQLSession;

import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * Runnable, that runs statements in a separate thread in order
 * to be able to interrupt it.
 */
public class SQLExecutorThread implements Runnable {
    private volatile boolean _running;

    private SQLSession _session;
    private String _command;
    private Statement _stmt;
    private ResultSet _rset;
    private Exception _exceptionResult;
    private volatile boolean _executing;

    SQLExecutorThread() {
        _running = true;
    }

    synchronized void executeAsync(SQLSession session, String command) {
        _session = session;
        _command = command;

        _executing = true;
        _exceptionResult = null;
        _stmt = null;
        _rset = null;
        notify();
    }

    synchronized void stopThread() {
        _running = false;
        notify();
    }

    boolean isExecuting() {
        return _executing;
    }

    /** returns the result set if any */
    ResultSet getResult() {
        return _rset;
    }

    /** returns the exception if any */
    Exception getException() {
        return _exceptionResult;
    }
    
    int getUpdateCount() throws SQLException {
        return _stmt.getUpdateCount();
    }

    void cancel() throws Exception {
        if (_stmt != null) {
            _stmt.cancel();
        }
        synchronized (this) {
            while (_executing) {
                wait();
            }
        }
    }
    
    void finishResult() {
        try { if (_rset != null) _rset.close(); } catch (Exception e) {}
        try { if (_stmt != null) _stmt.close(); } catch (Exception e) {}
    }

    public void run() {
        while (_running) {
            try {
                synchronized (this) {
                    wait();
                    if (!_running) return;
                    if (_session == null || _command == null) {
                        continue;
                    }
                }
                _stmt = _session.createStatement();
                try {
                    _stmt.setFetchSize(200);
                }
                catch (Exception e) {
                    // ignore
                }
                final boolean hasResultSet = _stmt.execute(_command);
                if (hasResultSet) {
                    _rset = _stmt.getResultSet();
                }
            }
            catch (Exception e) {
                _exceptionResult = e;
            }
            _executing = false;
            synchronized (this) {
                notifyAll();
            }
        }
    }
}
