/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: FileCompletionIterator.java,v 1.4 2002-07-23 06:36:09 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

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
    private String completePrefix;
    private int index;

    public FileCompletionIterator(String partialCommand, String startFile) {
	try {
	    int lastDirectory = startFile.lastIndexOf(File.separator);
	    String dirName = ".";
	    completePrefix = "";
	    if (lastDirectory > 0) {
		dirName = startFile.substring(0, lastDirectory);
		startFile = startFile.substring(lastDirectory + 1 );
		completePrefix = dirName + File.separator;
	    }
	    File f = (new File(dirName)).getCanonicalFile();
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
	    if (nextFileName.startsWith(matchName)) {
		File f = new File(completePrefix + nextFileName);
		if (f.isDirectory()) {
		    nextFileName += File.separator;
		}
		return true;
	    }
	}
	return false;
    }
    public Object  next() { return completePrefix + nextFileName; }
    public void remove() {}
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
