/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SessionManager.java,v 1.2 2004-01-27 18:16:33 hzeller Exp $ 
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
    
    private static SessionManager _instance;
    
    private HenPlus _henplus;
    private final SortedMap/*<String,SQLSession>*/ _sessions;
    private SQLSession _currentSession;

    private SessionManager(HenPlus henplus) {
        _henplus = henplus;
        _sessions  = new TreeMap();
    }
    
    public static SessionManager getInstance(HenPlus henplus) {
        if (_instance == null)
            _instance = new SessionManager(henplus);
        return _instance;
    }
    
    public void addSession(String sessionName, SQLSession session) {
        _sessions.put(sessionName, session);
    }
    
    public SQLSession removeSessionWithName(String sessionName) {
        return (SQLSession)_sessions.remove(sessionName);
    }
    
    public SQLSession getSessionByName(String name) {
        return (SQLSession)_sessions.get(name);
    }
    
    public String getFirstSessionName() {
        return (String)_sessions.firstKey();
    }
    
    public boolean closeCurrentSession() {
        _currentSession.close();
        return removeSession(_currentSession);
    }
    
    private boolean removeSession(SQLSession session) {
        boolean result = false;
        Map.Entry entry = null;
        Iterator it = _sessions.entrySet().iterator();
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
        Iterator sessIter = _sessions.values().iterator();
        while (sessIter.hasNext()) {
            SQLSession session = (SQLSession) sessIter.next();
            session.close();
        }
    }
    
    public int renameSession(String oldSessionName, String newSessionName) {
        int result = Command.EXEC_FAILED;
        
        if (sessionNameExists(newSessionName)) {
            System.err.println("A session with that name already exists");
        }
        else {
            SQLSession session = removeSessionWithName(oldSessionName);
            if (session != null) {
                addSession(newSessionName, session);
                _currentSession = session;
                result = Command.SUCCESS;
            }
        }
        
        return result;
    }
    
    public SortedSet getSessionNames() {
        SortedSet result = new TreeSet();
        final Iterator iter = _sessions.keySet().iterator();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }
    
    public int getSessionCount () {
        return _sessions.size();
    }
    
    public boolean hasSessions() {
        return !_sessions.isEmpty();
    }
    
    public boolean sessionNameExists(String sessionName) {
        return _sessions.containsKey(sessionName);
    }
    
    public void setCurrentSession(SQLSession session) {
        this._currentSession = session;
    }
    
    public SQLSession getCurrentSession() {
        return _currentSession;
    }
    
    /*  =====================  Helper methods  ======================  */

    /**
     * Used from several commands that need session name completion.
     */
    public Iterator completeSessionName(String partialSession) {
        Iterator result = null;
        if (_sessions != null) {
            NameCompleter completer = new NameCompleter(getSessionNames());
            // System.out.println("[SessionManager.completeSessionName] created completer for sessionnames "+getSessionNames().toString());
            result = completer.getAlternatives(partialSession);
        }
        return result;
    }
    
}
