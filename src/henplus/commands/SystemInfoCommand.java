/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: SystemInfoCommand.java,v 1.6 2005-12-14 10:53:03 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.HenPlus;
import henplus.SQLSession;
import henplus.util.ListMap;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.Formatter;

import java.util.Iterator;
import java.util.Map;

/**
 * Prints out some system information.<br>
 * Created on: Mar 23, 2004<br>
 */
public final class SystemInfoCommand extends AbstractCommand {
    
    private static final String CMD = "system-info";
    private static final int ONE_KILOBYTE = 1024;
    
    private double _memoryUsedBefore = 0.0;
    
    // ===================  rendering  =============

    private final static ColumnMetaData[] DESC_META;
    static {
        DESC_META = new ColumnMetaData[2];
        DESC_META[0] =
            new ColumnMetaData("System Property", ColumnMetaData.ALIGN_LEFT);
        DESC_META[1] = new ColumnMetaData("Value", ColumnMetaData.ALIGN_RIGHT);
    }
    
    //  =======================================

    public SystemInfoCommand() {
        super();
    }

    /* (non-Javadoc)
     * @see henplus.Command#getCommandList()
     */
    public String[] getCommandList() {
        return new String[] { CMD };
    }

    /* (non-Javadoc)
     * @see henplus.Command#participateInCommandCompletion()
     */
    public boolean participateInCommandCompletion() {
        return false;
    }

    /* (non-Javadoc)
     * @see henplus.Command#execute(henplus.SQLSession, java.lang.String, java.lang.String)
     */
    public int execute(SQLSession session, String command, String parameters) {
        
        final Map info = new ListMap();
        info.put("Java Version", System.getProperty("java.version") );
        info.put("Java VM", System.getProperty("java.vm.info") );
        info.put("Java Home",System.getProperty("java.home") );
        info.put("Java Vendor", System.getProperty("java.vendor") );
            
        final StringBuffer osInfo = new StringBuffer();
        osInfo.append( System.getProperty("os.name") );
        osInfo.append( " ");
        osInfo.append( System.getProperty("os.version") );
        osInfo.append( " " );
        osInfo.append( System.getProperty("os.arch") );
        info.put("Operating System", osInfo.toString());

        info.put("Default File Encoding", System.getProperty("file.encoding"));
        
        //-- make sure we get almost reliable memory usage information.
        System.gc();
        System.gc();
        System.gc();

        Runtime rt = Runtime.getRuntime();
        double totalMemory = rt.totalMemory() / ONE_KILOBYTE;
        double freeMemory = rt.freeMemory() / ONE_KILOBYTE;
        double maxMemory = rt.maxMemory() / ONE_KILOBYTE;
        double memoryUsed = totalMemory - freeMemory;
        double diffMemory = memoryUsed - _memoryUsedBefore;
        _memoryUsedBefore = memoryUsed;

        info.put("Max memory [KB]", Formatter.formatNumber( maxMemory, 2 ) );
        info.put("Allocated memory [KB]", Formatter.formatNumber( totalMemory, 2 ) );
        info.put("Free memory [KB]", Formatter.formatNumber( freeMemory, 2 ) );
        info.put("Used memory [KB]", Formatter.formatNumber( memoryUsed, 2 ) );
        info.put("Diff. of used memory (now-before) [KB]", Formatter.formatNumber( diffMemory, 2 ) );

        renderInfo( info );
        
        return SUCCESS;
    }

    public boolean requiresValidSession(String cmd) { 
        return false;
    }

    /* (non-Javadoc)
     * @see henplus.Command#getShortDescription()
     */
    public String getShortDescription() {
        return "print out some system information like memory usage.";
    }

    /* (non-Javadoc)
     * @see henplus.Command#getSynopsis(java.lang.String)
     */
    public String getSynopsis(String cmd) {
        return CMD;
    }

    /* (non-Javadoc)
     * @see henplus.Command#getLongDescription(java.lang.String)
     */
    public String getLongDescription(String cmd) {
        return "\tPrint out some system information like memory usage.";
    }
    
    //  ==================  rendering  ================
    
    private void renderInfo( Map info ) {

        TableRenderer table = new TableRenderer(DESC_META, HenPlus.out());
        
        Iterator iter = info.keySet().iterator();
        while ( iter.hasNext() ) {
            Object key = iter.next();
            Object value = info.get( key );
            
            Column[] row = new Column[2];
            row[0] = new Column( key.toString() );
            // don't call toString() on the value as it might be null
            row[1] = new Column( (String) value );
            
            table.addRow( row );
        }
        table.closeTable();
    }
}
