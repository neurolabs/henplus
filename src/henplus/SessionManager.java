/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SessionManager.java,v 1.3 2004-03-05 23:34:38 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus;

import henplus.view.util.NameCompleter;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class SessionManager {

    private static SessionManager instance;

    private final SortedMap<String,SQLSession> _sessions;
    private SQLSession _currentSession;

    private SessionManager() {
        _sessions = new TreeMap<String,SQLSession>();
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void addSession(final String sessionName, final SQLSession session) {
        _sessions.put(sessionName, session);
    }

    public SQLSession removeSessionWithName(final String sessionName) {
        return _sessions.remove(sessionName);
    }

    public SQLSession getSessionByName(final String name) {
        return _sessions.get(name);
    }

    public String getFirstSessionName() {
        return _sessions.firstKey();
    }

    public boolean closeCurrentSession() {
        _currentSession.close();
        return removeSession(_currentSession);
    }

    private boolean removeSession(final SQLSession session) {
        boolean result = false;
        Map.Entry entry = null;
        final Iterator it = _sessions.entrySet().iterator();
        while (it.hasNext()) {
            entry = (Map.Entry) it.next();
            if (entry.getValue() == session) {
                it.remove();
                result = true;
                break;
            }
        }
        return result;
    }

    public void closeAll() {
        final Iterator sessIter = _sessions.values().iterator();
        while (sessIter.hasNext()) {
            final SQLSession session = (SQLSession) sessIter.next();
            session.close();
        }
    }

    public int renameSession(final String oldSessionName, final String newSessionName) {
        int result = Command.EXEC_FAILED;

        if (sessionNameExists(newSessionName)) {
            System.err.println("A session with that name already exists");
        } else {
            final SQLSession session = removeSessionWithName(oldSessionName);
            if (session != null) {
                addSession(newSessionName, session);
                _currentSession = session;
                result = Command.SUCCESS;
            }
        }

        return result;
    }

    public SortedSet getSessionNames() {
        final SortedSet result = new TreeSet();
        final Iterator iter = _sessions.keySet().iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    public int getSessionCount() {
        return _sessions.size();
    }

    public boolean hasSessions() {
        return !_sessions.isEmpty();
    }

    public boolean sessionNameExists(final String sessionName) {
        return _sessions.containsKey(sessionName);
    }

    public void setCurrentSession(final SQLSession session) {
        this._currentSession = session;
    }

    public SQLSession getCurrentSession() {
        return _currentSession;
    }

    /* ===================== Helper methods ====================== */

    /**
     * Used from several commands that need session name completion.
     */
    public Iterator completeSessionName(final String partialSession) {
        Iterator result = null;
        if (_sessions != null) {
            final NameCompleter completer = new NameCompleter(getSessionNames());
            // System.out.println(
            // "[SessionManager.completeSessionName] created completer for sessionnames "
            // +getSessionNames().toString());
            result = completer.getAlternatives(partialSession);
        }
        return result;
    }

}
