/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: FileCompletionIterator.java,v 1.1.1.1 2002-01-19 21:10:45 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;

/**
 * fixme.
 */
public class FileCompletionIterator implements Iterator {
    private String dirList[];
    private String matchName;
    private String nextFileName;
    private int index;

    public FileCompletionIterator(String startFile) {
	try {
	    File f = (new File(startFile)).getCanonicalFile();
	    matchName = f.getName(); // last element..
	    dirList = f.getParentFile().list();
	}
	catch (IOException e) {
	    dirList = null;
	    matchName = null;
	}
	index = 0;
    }

    // this iterator _requires_, that hasNext() is called before next().

    public boolean hasNext() {
	if (dirList == null) return false;
	while (index < dirList.length) {
	    nextFileName = dirList[index++];
	    if (nextFileName.startsWith(matchName))
		return true;
	}
	return false;
    }
    public Object  next() { return nextFileName; }
    public void remove() {}
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
