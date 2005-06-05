/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.importparser.TypeParser;
import henplus.importparser.ImportParser;
import henplus.importparser.StringParser;
import henplus.importparser.ValueRecipient;
import henplus.importparser.IgnoreTypeParser;
import henplus.Interruptable;

import henplus.SQLSession;
import henplus.CommandDispatcher;
import henplus.SigIntHandler;
import henplus.HenPlus;
import henplus.AbstractCommand;
import henplus.view.util.NameCompleter;
import henplus.view.util.SortedMatchIterator;
import java.nio.charset.Charset;

import java.io.File;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.StringTokenizer;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;

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
	    "import", "import-check", "import-print"
	};
    }
    
    public ImportCommand(ListUserObjectsCommand tc) {
        _tableCompleter = tc;
    }

    public boolean requiresValidSession(String cmd) { 
        return "import".equals(cmd); 
    }

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
        /*
	HenPlus.msg().println("cmd='" + cmd + "';"
                              + "param='" + param + "'");
        */
        ConfigurationParser parser = new ConfigurationParser(param);
        String error = parser.getParseError();
        if (error != null) {
            HenPlus.msg().println(error);
            return SYNTAX_ERROR;
        }
        final ImportConfiguration config = parser.getConfig();

        try {
            final long startTime = System.currentTimeMillis();
            final long startRow = config.getStartRow();
            final long rowCount = config.getRowCount();
            long endRow = -1;
            if (rowCount >= 0) {
                endRow = startRow > 0 ? startRow + rowCount : rowCount;
            }
            long rows = -1;
            if ("import-print".equals(cmd)) {
                final PrintRecipient printRecipient = new PrintRecipient(config.getColumns(),
                                                                         startRow,
                                                                         endRow);
                SigIntHandler.getInstance().pushInterruptable(printRecipient);
                importFile(config, printRecipient);
                rows = printRecipient.getRowCount();
            }
            else if ("import-check".equals(cmd)) {
                final CountRecipient countRecipient = new CountRecipient();
                importFile(config, countRecipient);
                rows = countRecipient.getRowCount();
            }
            else if ("import".equals(cmd)) {
                //final ImportProcessor importProcessor = new ImportProcessor();
            }
            final long execTime = System.currentTimeMillis()-startTime;

            // TODO: procssed rows ?
            HenPlus.msg().print("reading " + rows + " rows from '" + config.getFilename() + "' took ");
            TimeRenderer.printTime(execTime, HenPlus.msg());
            HenPlus.msg().print(" total; ");
            TimeRenderer.printFraction(execTime, rows, HenPlus.msg());
            HenPlus.msg().println(" / row");
        }
        catch (Exception e) {
            e.printStackTrace();
            return EXEC_FAILED;
        }
	return SUCCESS;
    }

    private void importFile(ImportConfiguration config, ValueRecipient recipient) 
        throws Exception 
    {
        final File file = new File(config.getFilename());
        final String encoding = ((config.getEncoding() != null) 
                                 ? config.getEncoding()
                                 : "ISO-8859-1");
        final Reader reader = new InputStreamReader(new FileInputStream(file), encoding);
        final int colCount = config.getColumns().length;

        // TODO: parse type after colon.
        final TypeParser[] colParser = new TypeParser[ colCount ];
        for (int i=0; i < colCount; ++i) {
            colParser[i] = new StringParser(i);
        }
        try {
            final String colDelim = (config.getColDelimiter() != null
                                     ? config.getColDelimiter()
                                     : DEFAULT_COL_DELIM);
            final String rowDelim = (config.getRowDelimiter() != null
                                     ? config.getRowDelimiter()
                                     : DEFAULT_ROW_DELIM);
            ImportParser parser = new ImportParser(colParser, colDelim,
                                                   rowDelim);
            parser.parse(reader, recipient);
        }
        finally {
            reader.close();
        }
    }

    private final static class CountRecipient implements ValueRecipient, Interruptable {
        private long _rows;
        private volatile boolean _finished;

        CountRecipient() {
            _rows = 0;
            _finished = false;
        }

        public void setLong(int fieldNumber, long value) { }
        public void setString(int fieldNumber, String value) { }
        public void setDate(int fieldNumber, Calendar cal) { }
        public boolean finishRow() { 
            ++_rows;
            return _finished;
        }

        public void interrupt() {
            _finished = true;
        }

        long getRowCount() { return _rows; }
    }

    private final static class PrintRecipient implements ValueRecipient, Interruptable {
        private final String[] _columnNames;
        private final long _startRow;
        private final long _endRow;
        private long _rows;
        private volatile boolean _finished;

        public PrintRecipient(String[] columnNames,
                              long startRow,
                              long endRow) 
        {
            _columnNames = columnNames;
            _rows = 0;
            _startRow = startRow;
            _endRow = endRow;
        }
        
        private boolean rangeValid() {
            if (_startRow >= 0) {
                if (_rows < _startRow)
                    return false;
            }
            if (_endRow >= 0) {
                if (_rows >= _endRow) {
                    return false;
                }
            }
            return true;
        }

        private boolean printColName(int fieldNumber) {
            if (!rangeValid()) return false;
            if (fieldNumber > _columnNames.length) return false;
            final String colName = _columnNames[fieldNumber];
            if (colName == null) return false;
            HenPlus.msg().print(colName); // TODO: padding.
            HenPlus.msg().print(" = ");
            return true;
        }

        public void setLong(int fieldNumber, long value) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(String.valueOf(value));
            }
        }
        public void setString(int fieldNumber, String value) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(value);
            }
        }
        public void setDate(int fieldNumber, Calendar cal) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(cal != null ? cal.getTime().toString() : null);
            }
        }

        public void interrupt() {
            _finished = true;
        }
        
        long getRowCount() { return _rows; }
        public boolean finishRow() throws Exception {
            if (rangeValid()) {
                HenPlus.msg().println("----------> finished row " + _rows + " <----------");
            }
            _rows++;
            return _finished || (_endRow >= 0 && _rows >= _endRow);
        }
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
            //{ "end-row", null }                          /* integer */
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
                else if ("start-row".equals(commandName)) {
                    _config.setStartRow(Long.parseLong(commandValue));
                }
                else if ("row-count".equals(commandName)) {
                    _config.setRowCount(Long.parseLong(commandValue));
                }
                // end-row missing.
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
        private long _startRow = -1;
        private long _rowCount = -1;
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
        public void setStartRow(long startRow) { _startRow = startRow; }
        public long getStartRow() { return _startRow; }

        public void setRowCount(long rowCount) { _rowCount = rowCount; }
        public long getRowCount() { return _rowCount; }

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
                //System.err.println(result[i]);
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
