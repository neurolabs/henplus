/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: FileCompletionIterator.java,v 1.2 2002-01-27 13:44:06 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.util.Iterator;
import java.io.File;
import java.io.IOException;

/**
 * fixme. first simple implementation..
 */
public class FileCompletionIterator implements Iterator {
    private String dirList[];
    private String matchName;
    private String nextFileName;
    private int index;

    public FileCompletionIterator(String startFile) {
	try {
	    File f = (new File(startFile)).getCanonicalFile();
	    boolean isDir = f.isDirectory();
	    dirList = (isDir)
		? f.list()
		: f.getParentFile().list();
	    matchName = startFile;
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
