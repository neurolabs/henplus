/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.importparser.TypeParser;
import henplus.importparser.StringParser;
import henplus.importparser.IgnoreTypeParser;
import henplus.SQLSession;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.AbstractCommand;
import henplus.view.util.NameCompleter;
import henplus.view.util.SortedMatchIterator;
import java.nio.charset.Charset;

import java.util.StringTokenizer;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * document me.
 */
public class ImportCommand extends AbstractCommand {
    private final String DEFAULT_ROW_DELIM = "\n";
    private final String DEFAULT_COL_DELIM = "\t";

    private final ListUserObjectsCommand _tableCompleter;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "import", "import-check"
	};
    }
    
    public ImportCommand(ListUserObjectsCommand tc) {
        _tableCompleter = tc;
    }

    // FIXME: TEMPORARY for testing.
    public boolean requiresValidSession(String cmd) { return false; }

    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) {
        ConfigurationParser parser = new ConfigurationParser(_tableCompleter);
        if ("".equals(lastWord)) {
            partialCommand += " ";
        }
        else if (!partialCommand.endsWith(lastWord)) {
            partialCommand += lastWord;
        }
        return parser.complete(stripCommand(partialCommand));
    }
    
    private String stripCommand(String cmd) {
        int len = cmd.length();
        for (int i=0; i < len; ++i) {
            if (Character.isWhitespace(cmd.charAt(i))) {
                return cmd.substring(i);
            }
        }
        return "";
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	HenPlus.msg().println("sorry, not implemented yet.");
	HenPlus.msg().println("cmd='" + cmd + "';"
                              + "param='" + param + "'");
        ConfigurationParser parser = new ConfigurationParser(param);
        String error = parser.getParseError();
        if (error != null) {
            HenPlus.msg().println(error);
            return SYNTAX_ERROR;
        }
	return SUCCESS;
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "import delimited data into table";
    }

    public String getSynopsis(String cmd) {
        return cmd + " from <filename> into <tablename> columns (col1[:type][,col2[:type]]) [column-delim \"\\t\"] [row-delim \"\\n\"] [encoding <encoding>] [start-row <number>] [row-count|end-row <number>]\n"
            + "\tcol could be a column name or '-' if the column is to be ignored\n"
            + "\tthe optional type can be one of [string,number,date]";
    }

    public String getLongDescription(String cmd) {
	String dsc = null;
	if ("import-check".equals(cmd)) {
            dsc= "\tDry-run: read the file but do not insert anything\n";
        }
        else {
            dsc="\tImport the content of the file into table according to the format";
        }
        dsc+="\tIf the filename ends with '.gz', the\n"
            +"\tcontent is unzipped automatically\n\n";
        return dsc;
    }

    private interface CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                                     String partialValue);
    }

    private final static class ConfigurationParser {
        private final static Object[][] KEYWORDS = {
            /* (+) means: completable */
            { "from", new FilenameCompleterFactory() },  /*(+) filename */
            { "into", new TableCompleterFactory() },     /*(+) table */
            { "columns",  new ColumnCompleterFactory()}, /*(+) (...) */
            { "column-delim", null },                    /* string */
            { "row-delim", null },                       /* string */
            { "encoding", new EncodingCompleterFactory() },/*(+) any supported encoding */
            { "start-row", null },                       /* integer */
            { "row-count", null },                       /* integer */
            { "end-row", null }                          /* integer */
        };
        
        private String _parseError;
        private final ImportConfiguration _config;
        private final ListUserObjectsCommand _tableCompleter;

        public ConfigurationParser(ListUserObjectsCommand tableCompleter) {
            _config = new ImportConfiguration();
            _tableCompleter = tableCompleter;
        }

        public ConfigurationParser(String command) 
        {
            this((ListUserObjectsCommand) null); // we never complete anything
            parseConfig(command);
        }
        
        public ListUserObjectsCommand getTableCompleter() {
            return _tableCompleter;
        }

        /**
         * return the last parse error, if any.
         */
        public String getParseError() {
            return _parseError;
        }

        private void addError(String error) {
            if (_parseError == null) {
                _parseError = error;
            }
            else {
                _parseError += "\n" + error;
            }
        }

        private void resetError() {
            _parseError = null;
        }

        /**
         * parse the configuration an return the completer of the last
         * property.
         */
        private Iterator complete(String partial) {
            //System.err.println("tok: '" + cmd + "'");
            resetError();
            CommandTokenizer cmdTok = new CommandTokenizer(partial, "\"\"()");
            while (cmdTok.hasNext()) {
                String commandName = cmdTok.nextToken();
                if (!cmdTok.isCurrentTokenFinished()) {
                    //System.err.println("not finished: '" + cmd + "'");
                    return getCommandCompleter(commandName);
                }
                String commandValue = "";
                boolean needsCompletion = true;
                if (cmdTok.hasNext()) {
                    commandValue = cmdTok.nextToken();
                    needsCompletion = !cmdTok.isCurrentTokenFinished();
                }
                if (needsCompletion) {
                    CompleterFactory cfactory = findCompleter(commandName);
                    if (cfactory != null) {
                        return cfactory.getCompleter(this, commandValue);
                    }
                    return null;
                }
                else {
                    setParsedValue(commandName, commandValue);
                }
            }
            return getCommandCompleter("");
        }

        /**
         * parse a configuration that is complete
         */
        private void parseConfig(String complete) {
            resetError();
            CommandTokenizer cmdTok = new CommandTokenizer(complete, "\"\"()");
            while (cmdTok.hasNext()) {
                String commandName = cmdTok.nextToken();
                if (!cmdTok.isCurrentTokenFinished()) {
                    addError("command ends prematurely at '" 
                             + commandName + "'");
                    return;
                }
                String commandValue = "";
                if (cmdTok.hasNext()) {
                    commandValue = cmdTok.nextToken();
                }
                else {
                    addError("expecting value for '" + commandName + "'");
                    return;
                }
                setParsedValue(commandName, commandValue);
            }
        }
        
        private CompleterFactory findCompleter(String command) {
            for (int i=0; i < KEYWORDS.length; ++i) {
                if (KEYWORDS[i][0].equals(command)) {
                    return (CompleterFactory) KEYWORDS[i][1];
                }
            }
            addError("unknown option '" + command + "'");
            return null;
        }

        private void setParsedValue(String commandName, String commandValue) {
            try {
                if ("from".equals(commandName)) {
                    _config.setFilename(commandValue);
                }
                else if ("into".equals(commandName)) {
                    _config.setTable(commandValue);
                }
                else if ("columns".equals(commandName)) {
                    _config.setRawColumns(commandValue);
                }
                else if ("column-delim".equals(commandName)) {
                    _config.setColDelimiter(commandValue);
                }
                else if ("row-delim".equals(commandName)) {
                    _config.setRowDelimiter(commandValue);
                }
                else if ("encoding".equals(commandName)) {
                    _config.setEncoding(commandValue);
                }
                // start-row, row-count, end-row missing.
                else {
                    addError("unknown option '" + commandName + "'");
                }
            }
            catch (Exception e) {
                addError("invalid value for " + commandName + " : " 
                         + e.getMessage());
            }
        }

        private Iterator getCommandCompleter(String partial) {
            NameCompleter completer = new NameCompleter();
            // first: check for must have parameters; then rest.
            if (_config.getFilename() == null) {
                completer.addName("from");
            }
            else if (_config.getTable() == null) {
                completer.addName("into");
            }
            else if (_config.getColumns() == null) {
                completer.addName("columns");
            }
            else {
                if (_config.getColDelimiter() == null)
                    completer.addName("column-delim");
                if (_config.getRowDelimiter() == null)
                    completer.addName("row-delim");
                if (_config.getEncoding() == null)
                    completer.addName("encoding");
                if (_config.getStartRow() < 0)
                    completer.addName("start-row");
                if (_config.getRowCount() < 0) {
                    completer.addName("row-count");
                    completer.addName("end-row");
                }
            }
            return completer.getAlternatives(partial);
        }
        
        public ImportConfiguration getConfig() {
            return _config;
        }
    }

    private final static class ImportConfiguration {
        private String _filename;
        private String _schema;
        private String _table;
        private String _colDelimiter;
        private String _rowDelimiter;
        private Charset _charset;
        private int _startRow = -1;
        private int _rowCount = -1;
        private String[] _columns;

        public void setFilename(String filename) { _filename = filename; }
        public String getFilename() { return _filename; }
        
        public void setSchema(String schema) { _schema = schema; }
        public String getSchema() { return _schema; }
        
        public void setTable(String table) { _table = table; }
        public String getTable() { return _table; }

        public void setColDelimiter(String colDelimiter) { _colDelimiter = colDelimiter; }
        public String getColDelimiter() { return _colDelimiter; }

        public void setRowDelimiter(String rowDelimiter) { _rowDelimiter = rowDelimiter; }
        public String getRowDelimiter() { return _rowDelimiter; }

        public void setEncoding(String encoding) { 
            _charset = Charset.forName(encoding);
        }
        public String getEncoding() { 
            if (_charset == null) return null;
            return _charset.name();
        }
        public Charset getCharset() {
            return _charset;
        }
        public void setStartRow(int startRow) { _startRow = startRow; }
        public int getStartRow() { return _startRow; }

        public void setRowCount(int rowCount) { _rowCount = rowCount; }
        public int getRowCount() { return _rowCount; }

        public void setRawColumns(String commaDelimColumns) {
            if (!commaDelimColumns.startsWith("(")) {
                throw new IllegalArgumentException("columns must start with '('");
            }
            StringTokenizer tok = new StringTokenizer(commaDelimColumns,
                                                      " \t,()");
            String result[] = new String [ tok.countTokens() ];
            for (int i=0; tok.hasMoreElements(); ++i) {
                String token = tok.nextToken();
                result[i] = "-".equals(token) ? null : token;
                System.err.println(result[i]);
            }
            setColumns(result);
            if (!commaDelimColumns.endsWith(")")) {
                throw new IllegalArgumentException("columns must end with ')'");
            }
        }
        public void setColumns(String[] columns) { _columns = columns; }
        public String[] getColumns() { return _columns; }

    }

    private final static class FilenameCompleterFactory 
        implements CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                                     String lastCommand) {
            return new FileCompletionIterator(" " + lastCommand, "");
        }
    }

    private final static class TableCompleterFactory 
        implements CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                                     String partialName) {
            return parser.getTableCompleter()
                .completeTableName(HenPlus.getInstance().getCurrentSession(),
                                   partialName);
        }
    }

    private final static class ColumnCompleterFactory 
        implements CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                                     String lastCommand) {
            if ("".equals(lastCommand)) {
                List paren = new ArrayList();
                paren.add("(");
                return paren.iterator();
            }
            if (lastCommand.endsWith(" ")) {
                //...
            }
            String tab = parser.getConfig().getTable();
            return null;
        }
    }

    private final static class EncodingCompleterFactory 
        implements CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                                     String partialName) {
            Collection allEncodings = Charset.availableCharsets().keySet();
            NameCompleter completer = new NameCompleter(allEncodings);
            return completer.getAlternatives(partialName);
        }
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
