/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: FileCompletionIterator.java,v 1.5 2004-05-31 10:48:22 hzeller Exp $
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
    private String _dirList[];
    private String _matchName;
    private String _nextFileName;
    private String _completePrefix;
    private int _index;

    public FileCompletionIterator(final String partialCommand, final String lastWord) {
        String startFile;
        final int lastPos = partialCommand.lastIndexOf(' ');
        startFile = lastPos >= 0 ? partialCommand.substring(lastPos + 1)
                : "";
        // startFile = prefix + startFile;
        // System.err.println("f: " + startFile);

        try {
            final int lastDirectory = startFile.lastIndexOf(File.separator);
            String dirName = ".";
            _completePrefix = "";
            if (lastDirectory > 0) {
                dirName = startFile.substring(0, lastDirectory);
                startFile = startFile.substring(lastDirectory + 1);
                _completePrefix = dirName + File.separator;
            }
            final File f = new File(dirName).getCanonicalFile();
            final boolean isDir = f.isDirectory();
            _dirList = isDir ? f.list() : f.getParentFile().list();
            _matchName = startFile;
        } catch (final IOException e) {
            _dirList = null;
            _matchName = null;
        }
        _index = 0;
    }

    // this iterator _requires_, that hasNext() is called before next().

    public boolean hasNext() {
        if (_dirList == null) {
            return false;
        }
        while (_index < _dirList.length) {
            _nextFileName = _dirList[_index++];
            if (_nextFileName.startsWith(_matchName)) {
                final File f = new File(_completePrefix + _nextFileName);
                if (f.isDirectory()) {
                    _nextFileName += File.separator;
                }
                return true;
            }
        }
        return false;
    }

    public Object next() {
        return _completePrefix + _nextFileName;
    }

    public void remove() {
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
