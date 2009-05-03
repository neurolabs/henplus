/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.importparser.TypeParser;
import henplus.importparser.ImportParser;
import henplus.importparser.QuotedStringParser;
import henplus.importparser.ValueRecipient;
import henplus.importparser.IgnoreTypeParser;
import henplus.Interruptable;

import henplus.SQLSession;
import henplus.CommandDispatcher;
import henplus.SigIntHandler;
import henplus.HenPlus;
import henplus.AbstractCommand;
import henplus.view.util.NameCompleter;
import java.nio.charset.Charset;

import java.io.File;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.io.Reader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import java.util.StringTokenizer;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Calendar;

import java.sql.PreparedStatement;

/*
 Todo:
 - where clause with regexp
 - fix quoting handling
 */

/**
 * document me.
 */
public class ImportCommand extends AbstractCommand {
    private static final String DEFAULT_ROW_DELIM = "\n";
    private static final String DEFAULT_COL_DELIM = "\t";
    private static final String COMMAND_QUOTES = "\"\"''()";

    private final ListUserObjectsCommand _tableCompleter;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "import", "import-check", "import-print" };
    }

    public ImportCommand(final ListUserObjectsCommand tc) {
        _tableCompleter = tc;
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return "import".equals(cmd);
    }

    @Override
    public Iterator complete(final CommandDispatcher disp, String partialCommand,
            final String lastWord) {
        final ConfigurationParser parser = new ConfigurationParser(_tableCompleter);
        if ("".equals(lastWord)) {
            partialCommand += " ";
        } else if (!partialCommand.endsWith(lastWord)) {
            partialCommand += lastWord;
        }
        return parser.complete(stripCommand(partialCommand));
    }

    private String stripCommand(final String cmd) {
        final int len = cmd.length();
        for (int i = 0; i < len; ++i) {
            if (Character.isWhitespace(cmd.charAt(i))) {
                return cmd.substring(i);
            }
        }
        return "";
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        /*
         * HenPlus.msg().println("cmd='" + cmd + "';" + "param='" + param +
         * "'");
         */
        final ConfigurationParser parser = new ConfigurationParser(param);
        final String error = parser.getParseError();
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
            RowCountingRecipient innerRecipient = null;
            if ("import-print".equals(cmd)) {
                innerRecipient = new PrintRecipient(config.getColumns());
            } else if ("import-check".equals(cmd)) {
                innerRecipient = new CountRecipient();
            } else if ("import".equals(cmd)) {
                innerRecipient = new SqlImportProcessor(session, config);
            }

            final FilterRecipient filterRecipient = new FilterRecipient(startRow,
                    endRow, innerRecipient);
            SigIntHandler.getInstance().pushInterruptable(filterRecipient);
            importFile(config, filterRecipient);
            final long readRows = filterRecipient.getRowCount();
            final long processedRows = innerRecipient.getRowCount();

            final long execTime = System.currentTimeMillis() - startTime;

            HenPlus.msg().print(
                    "reading " + readRows + " rows from '"
                    + config.getFilename() + "' took ");
            TimeRenderer.printTime(execTime, HenPlus.msg());
            HenPlus.msg().print(" total; ");
            TimeRenderer.printFraction(execTime, readRows, HenPlus.msg());
            HenPlus.msg().println(" / row");
            HenPlus.msg().println("processed " + processedRows + " rows");
        } catch (final Exception e) {
            e.printStackTrace();
            return EXEC_FAILED;
        }
        return SUCCESS;
    }

    private void importFile(final ImportConfiguration config, final ValueRecipient recipient)
    throws Exception {
        final File file = new File(config.getFilename());
        final String encoding = config.getEncoding() != null ? config
                .getEncoding() : "ISO-8859-1";
        InputStream fileIn = new FileInputStream(file);
        if (config.getFilename().endsWith(".gz")) {
            fileIn = new GZIPInputStream(fileIn);
        }
        final Reader reader = new InputStreamReader(fileIn, encoding);
        final int colCount = config.getColumns().length;

        // TODO: parse type after colon.
        final TypeParser[] colParser = new TypeParser[colCount];
        int colIndex = 0;
        for (int i = 0; i < colCount; ++i) {
            final String colName = config.getColumns()[i];
            colParser[i] = colName == null ? (TypeParser) new IgnoreTypeParser()
            : (TypeParser) new QuotedStringParser(colIndex++);
        }
        try {
            final String colDelim = config.getColDelimiter() != null ? config
                    .getColDelimiter() : DEFAULT_COL_DELIM;
            final String rowDelim = config.getRowDelimiter() != null ? config
                    .getRowDelimiter() : DEFAULT_ROW_DELIM;
            final ImportParser parser = new ImportParser(colParser, colDelim,
                    rowDelim);
            parser.parse(reader, recipient);
        } finally {
            reader.close();
        }
    }

    private interface RowCountingRecipient extends ValueRecipient {
        long getRowCount();
    }

    private static class FilterRecipient implements RowCountingRecipient,
    Interruptable {
        private final long _startRow;
        private final long _endRow;
        private final ValueRecipient _target;
        private long _rows;
        private volatile boolean _finished;

        public FilterRecipient(final long startRow, final long endRow, final ValueRecipient target) {
            _rows = 0;
            _startRow = startRow;
            _endRow = endRow;
            _target = target;
        }

        private final boolean expressionMatches() {
            return true; // no expression match yet.
        }

        private final boolean rangeValid() {
            if (_startRow >= 0) {
                if (_rows < _startRow) {
                    return false;
                }
            }
            if (_endRow >= 0) {
                if (_rows >= _endRow) {
                    return false;
                }
            }
            return true;
        }

        public void setLong(final int fieldNumber, final long value) throws Exception {
            if (rangeValid()) {
                _target.setLong(fieldNumber, value);
            }
        }

        public void setString(final int fieldNumber, final String value) throws Exception {
            if (rangeValid()) {
                _target.setString(fieldNumber, value);
            }
        }

        public void setDate(final int fieldNumber, final Calendar cal) throws Exception {
            if (rangeValid()) {
                _target.setDate(fieldNumber, cal);
            }
        }

        public void interrupt() {
            _finished = true;
        }

        public long getRowCount() {
            return _rows;
        }

        public boolean finishRow() throws Exception {
            boolean deligeeFinish = false;
            if (rangeValid() && expressionMatches()) {
                deligeeFinish = _target.finishRow();
            }
            _rows++;
            return deligeeFinish || _finished
            || _endRow >= 0 && _rows >= _endRow;
        }
    }

    private final static class CountRecipient implements RowCountingRecipient {
        private long _rows;

        CountRecipient() {
            _rows = 0;
        }

        public void setLong(final int fieldNumber, final long value) {
        }

        public void setString(final int fieldNumber, final String value) {
        }

        public void setDate(final int fieldNumber, final Calendar cal) {
        }

        public boolean finishRow() {
            ++_rows;
            return false;
        }

        public long getRowCount() {
            return _rows;
        }
    }

    private final static class PrintRecipient implements RowCountingRecipient {
        private final String[] _paddedNames;
        private long _rows;
        private boolean _colWritten;

        public PrintRecipient(final String[] columnNames) {
            _rows = 0;
            int maxLen = -1;
            int maxIndex = 0;
            // find max length and col-number
            for (int i = 0; i < columnNames.length; ++i) {
                final String colName = columnNames[i];
                if (colName != null) {
                    if (colName.length() > maxLen) {
                        maxLen = colName.length();
                    }
                    maxIndex++;
                }
            }
            _paddedNames = new String[maxIndex];
            int colIndex = 0;
            // precalculate padded names
            for (int i = 0; i < columnNames.length; ++i) {
                final String colName = columnNames[i];
                if (colName == null) {
                    continue;
                }
                _paddedNames[colIndex] = colName;
                while (_paddedNames[colIndex].length() < maxLen) {
                    _paddedNames[colIndex] += " ";
                }
                _paddedNames[colIndex] += " : ";
                colIndex++;
            }
            _colWritten = false;
        }

        private boolean printColName(final int fieldNumber) {
            if (fieldNumber > _paddedNames.length) {
                return false;
            }
            final String colName = _paddedNames[fieldNumber];
            if (colName == null) {
                return false;
            }
            if (!_colWritten) {
                HenPlus.msg().attributeBold();
                HenPlus.msg().println(
                        "----------------------- row " + _rows + " :");
                HenPlus.msg().attributeReset();
                _colWritten = true;
            }
            HenPlus.msg().attributeBold();
            HenPlus.msg().print(colName); // TODO: padding.
            HenPlus.msg().attributeReset();
            return true;
        }

        public void setLong(final int fieldNumber, final long value) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(String.valueOf(value));
            }
        }

        public void setString(final int fieldNumber, final String value) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(value);
            }
        }

        public void setDate(final int fieldNumber, final Calendar cal) {
            if (printColName(fieldNumber)) {
                HenPlus.msg().println(
                        cal != null ? cal.getTime().toString() : null);
            }
        }

        public long getRowCount() {
            return _rows;
        }

        public boolean finishRow() throws Exception {
            _rows++;
            _colWritten = false;
            return false;
        }
    }

    private final static class SqlImportProcessor implements
    RowCountingRecipient {
        private long _rows;
        private final PreparedStatement _stmt;

        public SqlImportProcessor(final SQLSession session, final ImportConfiguration config)
        throws Exception {
            _rows = 0;
            final StringBuffer cmd = new StringBuffer("insert into ");
            cmd.append(config.getTable()).append(" (");
            boolean isFirst = true;
            for (int i = 0; i < config.getColumns().length; ++i) {
                if (config.getColumns()[i] != null) {
                    if (!isFirst) {
                        cmd.append(",");
                    }
                    isFirst = false;
                    cmd.append(config.getColumns()[i]);
                }
            }
            cmd.append(") values (");
            isFirst = true;
            for (int i = 0; i < config.getColumns().length; ++i) {
                if (config.getColumns()[i] != null) {
                    if (!isFirst) {
                        cmd.append(",");
                    }
                    isFirst = false;
                    cmd.append("?");
                }
            }
            cmd.append(")");
            final String stmtString = cmd.toString();
            System.out.println("INSERTING WITH " + stmtString);
            _stmt = session.getConnection().prepareStatement(stmtString);
        }

        public void setLong(final int fieldNumber, final long value) throws Exception {
            _stmt.setLong(fieldNumber + 1, value);
        }

        public void setString(final int fieldNumber, final String value) throws Exception {
            _stmt.setString(fieldNumber + 1, value);
        }

        public void setDate(final int fieldNumber, final Calendar cal) throws Exception {
            throw new UnsupportedOperationException("not yet.");
        }

        public long getRowCount() {
            return _rows;
        }

        public boolean finishRow() throws Exception {
            _rows++;
            _stmt.execute();
            return false;
        }
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "import delimited data into table";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd
        + " from <filename> into <tablename> columns (col1[:type][,col2[:type]]) [column-delim \"\\t\"] [row-delim \"\\n\"] [encoding <encoding>] [start-row <number>] [row-count|end-row <number>]\n"
        + "\tcol could be a column name or '-' if the column is to be ignored\n"
        + "\tthe optional type can be one of [string,number,date]";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        if ("import-check".equals(cmd)) {
            dsc = "\tDry-run: read the file but do not insert anything\n";
        } else {
            dsc = "\tImport the content of the file into table according to the format\n";
        }
        dsc += "\tIf the filename ends with '.gz', the\n"
            + "\tcontent is unzipped automatically\n\n";
        return dsc;
    }

    private interface CompleterFactory {
        public Iterator getCompleter(ConfigurationParser parser,
                String partialValue);
    }

    private final static class ConfigurationParser {
        private final static Object[][] KEYWORDS = {
            /* (+) means: completable */
            { "from", new FilenameCompleterFactory() }, /* (+) filename */
            { "into", new TableCompleterFactory() }, /* (+) table */
            { "columns", new ColumnCompleterFactory() }, /* (+) (...) */
            /* { "filter", null }, */
            { "column-delim", null }, /* string */
            { "row-delim", null }, /* string */
            { "encoding", new EncodingCompleterFactory() },/*
             * (+) any supported
             * encoding
             */
            { "start-row", null }, /* integer */
            { "row-count", null }, /* integer */
            // { "end-row", null } /* integer */
        };

        private String _parseError;
        private final ImportConfiguration _config;
        private final ListUserObjectsCommand _tableCompleter;

        public ConfigurationParser(final ListUserObjectsCommand tableCompleter) {
            _config = new ImportConfiguration();
            _tableCompleter = tableCompleter;
        }

        public ConfigurationParser(final String command) {
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

        private void addError(final String error) {
            if (_parseError == null) {
                _parseError = error;
            } else {
                _parseError += "\n" + error;
            }
        }

        private void resetError() {
            _parseError = null;
        }

        /**
         * parse the configuration an return the completer of the last property.
         */
        private Iterator complete(final String partial) {
            // System.err.println("tok: '" + cmd + "'");
            resetError();
            final CommandTokenizer cmdTok = new CommandTokenizer(partial,
                    COMMAND_QUOTES);
            while (cmdTok.hasNext()) {
                final String commandName = cmdTok.nextToken();
                if (!cmdTok.isCurrentTokenFinished()) {
                    // System.err.println("not finished: '" + cmd + "'");
                    return getCommandCompleter(commandName);
                }
                String commandValue = "";
                boolean needsCompletion = true;
                if (cmdTok.hasNext()) {
                    commandValue = cmdTok.nextToken();
                    needsCompletion = !cmdTok.isCurrentTokenFinished();
                }
                if (needsCompletion) {
                    final CompleterFactory cfactory = findCompleter(commandName);
                    if (cfactory != null) {
                        return cfactory.getCompleter(this, commandValue);
                    }
                    return null;
                } else {
                    setParsedValue(commandName, commandValue);
                }
            }
            return getCommandCompleter("");
        }

        /**
         * parse a configuration that is complete
         */
        private void parseConfig(final String complete) {
            resetError();
            final CommandTokenizer cmdTok = new CommandTokenizer(complete,
                    COMMAND_QUOTES);
            while (cmdTok.hasNext()) {
                final String commandName = cmdTok.nextToken();
                if (!cmdTok.isCurrentTokenFinished()) {
                    addError("command ends prematurely at '" + commandName
                            + "'");
                    return;
                }
                String commandValue = "";
                if (cmdTok.hasNext()) {
                    commandValue = cmdTok.nextToken();
                } else {
                    addError("expecting value for '" + commandName + "'");
                    return;
                }
                setParsedValue(commandName, commandValue);
            }
        }

        private CompleterFactory findCompleter(final String command) {
            for (int i = 0; i < KEYWORDS.length; ++i) {
                if (KEYWORDS[i][0].equals(command)) {
                    return (CompleterFactory) KEYWORDS[i][1];
                }
            }
            addError("unknown option to complete '" + command + "'");
            return null;
        }

        private void setParsedValue(final String commandName, final String commandValue) {
            try {
                if ("from".equals(commandName)) {
                    _config.setFilename(commandValue);
                } else if ("into".equals(commandName)) {
                    _config.setTable(commandValue);
                } else if ("columns".equals(commandName)) {
                    _config.setRawColumns(commandValue);
                } else if ("column-delim".equals(commandName)) {
                    _config.setColDelimiter(stripQuotes(commandValue));
                } else if ("row-delim".equals(commandName)) {
                    _config.setRowDelimiter(stripQuotes(commandValue));
                } else if ("encoding".equals(commandName)) {
                    _config.setEncoding(commandValue);
                } else if ("start-row".equals(commandName)) {
                    _config.setStartRow(Long.parseLong(commandValue));
                } else if ("row-count".equals(commandName)) {
                    _config.setRowCount(Long.parseLong(commandValue));
                }
                // end-row missing.
                else {
                    addError("unknown option '" + commandName + "'");
                }
            } catch (final Exception e) {
                addError("invalid value for " + commandName + " : "
                        + e.getMessage());
            }
        }

        private String stripQuotes(final String quotedString) {
            if (quotedString == null || quotedString.length() < 2) {
                return quotedString;
            }
            final char first = quotedString.charAt(0);
            if (first == '"' || first == '\'') {
                final int lastPos = quotedString.length() - 1;
                if (quotedString.charAt(lastPos) == first) {
                    return quotedString.substring(1, lastPos);
                }
            }
            return quotedString;
        }

        private Iterator getCommandCompleter(final String partial) {
            final NameCompleter completer = new NameCompleter();
            // first: check for must have parameters; then rest.
            if (_config.getFilename() == null) {
                completer.addName("from");
            } else if (_config.getTable() == null) {
                completer.addName("into");
            } else if (_config.getColumns() == null) {
                completer.addName("columns");
            } else {
                if (_config.getColDelimiter() == null) {
                    completer.addName("column-delim");
                }
                if (_config.getRowDelimiter() == null) {
                    completer.addName("row-delim");
                }
                if (_config.getEncoding() == null) {
                    completer.addName("encoding");
                }
                if (_config.getStartRow() < 0) {
                    completer.addName("start-row");
                }
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

        public void setFilename(final String filename) {
            _filename = filename;
        }

        public String getFilename() {
            return _filename;
        }

        public void setSchema(final String schema) {
            _schema = schema;
        }

        public String getSchema() {
            return _schema;
        }

        public void setTable(final String table) {
            _table = table;
        }

        public String getTable() {
            return _table;
        }

        public void setColDelimiter(final String colDelimiter) {
            _colDelimiter = colDelimiter;
        }

        public String getColDelimiter() {
            return _colDelimiter;
        }

        public void setRowDelimiter(final String rowDelimiter) {
            _rowDelimiter = rowDelimiter;
        }

        public String getRowDelimiter() {
            return _rowDelimiter;
        }

        public void setEncoding(final String encoding) {
            _charset = Charset.forName(encoding);
        }

        public String getEncoding() {
            if (_charset == null) {
                return null;
            }
            return _charset.name();
        }

        public Charset getCharset() {
            return _charset;
        }

        public void setStartRow(final long startRow) {
            _startRow = startRow;
        }

        public long getStartRow() {
            return _startRow;
        }

        public void setRowCount(final long rowCount) {
            _rowCount = rowCount;
        }

        public long getRowCount() {
            return _rowCount;
        }

        public void setRawColumns(final String commaDelimColumns) {
            if (!commaDelimColumns.startsWith("(")) {
                throw new IllegalArgumentException(
                "columns must start with '('");
            }
            final StringTokenizer tok = new StringTokenizer(commaDelimColumns,
            " \t,()");
            final String result[] = new String[tok.countTokens()];
            for (int i = 0; tok.hasMoreElements(); ++i) {
                final String token = tok.nextToken();
                result[i] = "-".equals(token) ? null : token;
                // System.err.println(result[i]);
            }
            setColumns(result);
            if (!commaDelimColumns.endsWith(")")) {
                throw new IllegalArgumentException("columns must end with ')'");
            }
        }

        public void setColumns(final String[] columns) {
            _columns = columns;
        }

        public String[] getColumns() {
            return _columns;
        }

    }

    private final static class FilenameCompleterFactory implements
    CompleterFactory {
        public Iterator getCompleter(final ConfigurationParser parser,
                final String lastCommand) {
            return new FileCompletionIterator(" " + lastCommand, "");
        }
    }

    private final static class TableCompleterFactory implements
    CompleterFactory {
        public Iterator getCompleter(final ConfigurationParser parser,
                final String partialName) {
            return parser.getTableCompleter().completeTableName(
                    HenPlus.getInstance().getCurrentSession(), partialName);
        }
    }

    private final static class ColumnCompleterFactory implements
    CompleterFactory {
        public Iterator getCompleter(final ConfigurationParser parser,
                final String lastCommand) {
            if ("".equals(lastCommand)) {
                final List paren = new ArrayList();
                paren.add("(");
                return paren.iterator();
            }
            // if (lastCommand.endsWith(" ")) {
            // //...
            // }
            // String tab = parser.getConfig().getTable();
            // TODO: read columns from meta-data
            return null;
        }
    }

    private final static class EncodingCompleterFactory implements
    CompleterFactory {
        public Iterator getCompleter(final ConfigurationParser parser,
                final String partialName) {
            final Collection allEncodings = Charset.availableCharsets().keySet();
            final NameCompleter completer = new NameCompleter(allEncodings);
            return completer.getAlternatives(partialName);
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
