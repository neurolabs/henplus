/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.EventListener;

/**
 * A Listener that is called by the CommandDispatcher
 * whenever a command is executed.
 */
public interface ExecutionListener extends EventListener {
    /**
     * called before an command is to be executed.
     */
    void beforeExecution(SQLSession session, String command);

    /**
     * called after a command is executed.
     */
    void afterExecution(SQLSession session, String command, int result);
}
