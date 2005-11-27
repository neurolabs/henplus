/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Implementation of a Command with default settings. Override
 * what is necessary in your Command. It makes sense to derive plug-ins
 * from this AbstractCommand - this makes the plug-in more robust
 * with regard to newly added methods.
 * @author Henner Zeller
 */
public abstract class AbstractCommand implements Command {
    
    private Options options;
    
    // no description by default.
    public String getShortDescription() { return null; }
    public String getLongDescription(String cmd) { return null; }
    public String getSynopsis(String cmd) { return null; }
    
    // All commands are completed by default.
    public boolean participateInCommandCompletion() { return true; }

    // no completion support by the commmand
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, String lastWord) {
	return null; 
    }
    
    public void shutdown() { /* don't care */ }

    // the simple commands are always complete on newline or semicolon
    public boolean isComplete(String command) { return true; }

    public boolean requiresValidSession(String cmd) { return true; }

    /**
     * convenience method: returns the number of elements in this
     * string, separated by whitespace.
     */
    protected int argumentCount(String command) {
	return (new StringTokenizer(command)).countTokens();
    }
    
    
    protected Options getOptions() {
        return options;
    }
    public void setOptions(Options options) {
        this.options = options;
    }
    public Option getOption(String arg0) {
        return options.getOption(arg0);
    }
    
    /**
     * Override this method if you want to register command-specific options
     * @param r
     */
    public void registerOptions(Options r){
        
    }
    
    public void handleCommandline(CommandLine line){
        
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
