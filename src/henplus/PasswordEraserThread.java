/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PasswordEraserThread.java,v 1.3 2003-01-28 05:55:26 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 * inspired by hack provided in 
 *  <http://java.sun.com/features/2002/09/pword_mask.html>
 * Thanks to alec <acgnacgn@yahoo.co.uk> to point me to this.
 */
package henplus;

import java.io.*;

/**
 * Erase password as it is typed. Since we do not have access to 
 * the tty in a way to switch off echoing, we constantly override
 * any stuff written with spaces. This is a hack, since it is kinda
 * busy process doing this in a loop and it may still expose letters
 * from the password for the fraction of a second. However, this is
 * better than nothing, right ?
 */
class PasswordEraserThread extends Thread {
    private final String eraser;
    private volatile boolean running;
    
    /**
     *@param prompt The prompt displayed to the user
     */
    public PasswordEraserThread(String prompt) {
        /*
         * we are erasing by writing the prompt followed by spaces
         * from the beginning of the line
         */
        eraser = "\r" + prompt + "                \r" + prompt;
        running = true;
    }

    
    /**
     * Begin masking until asked to stop.
     */
    public void run() {
        while(running) {
            try {
                this.sleep(1); // yield.
            }
            catch (InterruptedException iex) {
                iex.printStackTrace();
            }
            if (running) {
                System.out.print(eraser);
            }
            System.out.flush();
        }
    }

   public void done() {
       running = false;
   }
}
