/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TableRenderer.java,v 1.4 2002-02-26 17:58:19 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.util;

import java.util.Vector;
import java.util.Iterator;
import java.io.PrintStream;

/**
 * document me.
 */
public class TableRenderer {
    private static final int MAX_CACHE_ELEMENTS = 500;

    private final ColumnMetaData meta[];
    private final PrintStream    out;
    private final Vector         cacheRows;
    private boolean              alreadyFlushed;
    private int                  writtenRows;

    public TableRenderer(ColumnMetaData[] meta, PrintStream out) {
	this.meta = meta;
	this.out = out;
	/*
	 * we cache the rows in order to dynamically determine the
	 * output width of each column.
	 */
	this.cacheRows   = new Vector();
	this.alreadyFlushed = false;
	this.writtenRows = 0;
    }
    
    public void addRow(Column[] row) {
	for (int i = 0 ; i < meta.length ; ++i) {
	    meta[i].updateWidth(row[i].getWidth());
	}
	cacheRows.add(row);
	if (cacheRows.size() > MAX_CACHE_ELEMENTS) {
	    flush();
	    cacheRows.clear();
	}
    }
    
    public void closeTable() {
	flush();
	if (writtenRows > 0) {
	    printHorizontalLine();
	}
    }

    /**
     * flush the cached rows.
     */
    public void flush() {
	if (!alreadyFlushed) {
	    printTableHeader();
	    alreadyFlushed = true;
	}
	Iterator rowIterator = cacheRows.iterator();
	while (rowIterator.hasNext()) {
	    Column[] currentRow = (Column[]) rowIterator.next();
	    boolean hasMoreLines;
	    do {
		hasMoreLines = false;
		for (int i = 0 ; i < meta.length ; ++i) {
		    if (!meta[i].doDisplay()) continue;
		    String txt;
		    out.print(" ");
		    txt = formatString (currentRow[i].getNextLine(), ' ',
					meta[i].getWidth(),
					meta[i].getAlignment());
		    hasMoreLines |= currentRow[i].hasNextLine();
		    if (currentRow[i].isNull()) Terminal.grey(out);
		    out.print(txt);
		    if (currentRow[i].isNull()) Terminal.reset(out);
		    out.print(" |");
		}
		out.println();
	    }
	    while (hasMoreLines);
	    ++writtenRows;
	}
    }
    
    private void printHorizontalLine() {
	for (int i = 0 ; i < meta.length ; ++i) {
	    if (!meta[i].doDisplay()) continue;
	    String txt;
	    txt = formatString ("", '-', meta[i].getWidth()+2,
				ColumnMetaData.ALIGN_LEFT);
	    out.print(txt);
	    out.print('+');
	}
	out.println();
    }

    private void printTableHeader() {
	printHorizontalLine();
	for (int i=0; i < meta.length ; ++i) {
	    if (!meta[i].doDisplay()) continue;
	    String txt;
	    txt = formatString (meta[i].getLabel(), ' ',
				meta[i].getWidth()+1,
				ColumnMetaData.ALIGN_CENTER);
	    Terminal.boldface(out);
	    out.print(txt);
	    Terminal.reset(out);
	    out.print(" |");
	}
	out.println();
	printHorizontalLine();
    }

    private String formatString (String text,
				 char fillchar, int len, int alignment) {
	StringBuffer fillstr = new StringBuffer();
	
	if (len > 4000) {
	    len = 4000;
	}
	
	if (text == null) {
		text = "[NULL]";
	}
	int slen = text.length();
	
	if (alignment == ColumnMetaData.ALIGN_LEFT) {
	    fillstr.append(text);
	}
	int fillNumber = len - slen;
	int boundary = 0;
	if (alignment == ColumnMetaData.ALIGN_CENTER) {
	    boundary = fillNumber / 2;
	}
	while (fillNumber > boundary) {
	    fillstr.append (fillchar);
	    --fillNumber;
	}
	if (alignment != ColumnMetaData.ALIGN_LEFT) {
	    fillstr.append(text);
	}
	while (fillNumber > 0) {
	    fillstr.append (fillchar);
	    --fillNumber;
	}
	return fillstr.toString();
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
