/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: EchoCommandProperty.java,v 1.2 2004-03-05 23:34:38 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.property.BooleanPropertyHolder;

/**
 * The Property echo-commands that simply registers itself at the
 * command dispatcher to echo the commands it is executing.
 */
public final class EchoCommandProperty 
    extends BooleanPropertyHolder 
    implements ExecutionListener 
{
    private CommandDispatcher _dispatcher;

    public EchoCommandProperty(CommandDispatcher disp) {
        super(false);
        _dispatcher = disp;
    }
    
    public String getDefaultValue() { 
        return "off"; 
    }

    public void booleanPropertyChanged(boolean echoCommands) {
        if (echoCommands) {
            _dispatcher.addExecutionListener(this);
        }
        else {
            _dispatcher.removeExecutionListener(this);
        }
    }

    public String getShortDescription() {
        return "echo commands prior to execution.";
    }

    //-- Execution listener

    public void beforeExecution(SQLSession session, String command) {
        HenPlus.msg().println(command);
    }
    public void afterExecution(SQLSession session, String command, int result){
        /* don't care */
    }
}
