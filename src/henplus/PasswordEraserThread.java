/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PasswordEraserThread.java,v 1.1 2003-01-26 21:15:10 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 * inspired by hack provided in 
 *  <http://java.sun.com/features/2002/09/pword_mask.html>
 * Thanks to alec <acgnacgn@yahoo.co.uk> to point me to this.
 */
package henplus;

import java.io.*;

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
        eraser = "\r" + prompt + "    \r" + prompt;
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
