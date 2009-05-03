/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: EchoCommandProperty.java,v 1.4 2004-03-07 14:22:02 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.event.ExecutionListener;

import henplus.property.BooleanPropertyHolder;

/**
 * The Property echo-commands that simply registers itself at the command
 * dispatcher to echo the commands it is executing.
 */
public final class EchoCommandProperty extends BooleanPropertyHolder implements
ExecutionListener {
    private final CommandDispatcher _dispatcher;

    public EchoCommandProperty(final CommandDispatcher disp) {
        super(false);
        _dispatcher = disp;
    }

    @Override
    public String getDefaultValue() {
        return "off";
    }

    @Override
    public void booleanPropertyChanged(final boolean echoCommands) {
        if (echoCommands) {
            _dispatcher.addExecutionListener(this);
        } else {
            _dispatcher.removeExecutionListener(this);
        }
    }

    @Override
    public String getShortDescription() {
        return "echo commands prior to execution.";
    }

    // -- Execution listener

    public void beforeExecution(final SQLSession session, final String command) {
        HenPlus.msg().println(command.trim());
    }

    public void afterExecution(final SQLSession session, final String command, final int result) {
        /* don't care */
    }
}
