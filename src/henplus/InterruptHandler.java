/*
 * $Id: InterruptHandler.java,v 1.1 2005-12-14 10:53:03 hzeller Exp $
 * (c) Copyright 2005 freiheit.com technologies GmbH
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */
package henplus;

public interface InterruptHandler {

    void pushInterruptable(Interruptable t);

    void popInterruptable();

    void reset();

}