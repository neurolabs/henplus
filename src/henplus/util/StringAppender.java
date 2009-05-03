///*
// * This is free software, licensed under the Gnu Public License (GPL)
// * get a copy from <http://www.gnu.org/licenses/gpl.html>
// */
//package henplus.util;
//
///**
// * FIXME: removeme.
// * 
// * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
// * @version $Id: StringAppender.java,v 1.3 2004-05-31 10:48:22 hzeller Exp $
// */
//public final class StringAppender {
//
//    private static StringBuilder sb;
//    private static StringAppender instance;
//
//    private StringAppender() {
//        sb = new StringBuilder();
//    }
//
//    public static final StringAppender getInstance() {
//        if (instance == null) {
//            instance = new StringAppender();
//        }
//        return instance;
//    }
//
//    public static StringAppender start(final String value) {
//        if (instance == null) {
//            instance = new StringAppender();
//        }
//        sb.append(value);
//        return instance;
//    }
//
//    public StringAppender append(final String value) {
//        sb.append(value);
//        return this;
//    }
//
//    public StringAppender append(final int value) {
//        sb.append(value);
//        return this;
//    }
//
//    @Override
//    public String toString() {
//        final String result = sb.toString();
//        sb.delete(0, sb.length());
//        return result;
//    }
//
//}
