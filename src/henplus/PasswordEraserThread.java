/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PasswordEraserThread.java,v 1.5 2004-03-05 23:34:38 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 * inspired by hack provided in
 *  <http://java.sun.com/features/2002/09/pword_mask.html>
 * Thanks to alec <acgnacgn@yahoo.co.uk> to point me to this.
 */
package henplus;

/**
 * Erase password as it is typed. Since we do not have access to the tty in a
 * way to switch off echoing, we constantly override any stuff written with
 * spaces. This is a hack, since it is kinda busy process doing this in a loop
 * and it may still expose letters from the password for the fraction of a
 * second. However, this is better than nothing, right ?
 */
class PasswordEraserThread extends Thread {
    private final String eraser;
    private boolean running;
    private boolean onHold;

    /**
     *@param prompt
     *            The prompt displayed to the user
     */
    public PasswordEraserThread(final String prompt) {
        /*
         * we are erasing by writing the prompt followed by spaces from the
         * beginning of the line
         */
        eraser = "\r" + prompt + "                \r" + prompt;
        running = true;
        onHold = true;
    }

    /**
     * Begin masking until asked to stop.
     */
    @Override
    public void run() {
        while (running) {
            if (onHold) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (final InterruptedException iex) {
                    // ignore.
                }
                if (onHold) {
                    continue;
                }
            }

            try {
                Thread.sleep(1); // yield.
            } catch (final InterruptedException iex) {
                // ignore
            }
            if (running && !onHold) {
                System.out.print(eraser);
            }
            System.out.flush();
        }
    }

    public synchronized void holdOn() {
        onHold = true;
        notify();
    }

    public synchronized void goOn() {
        onHold = false;
        notify();
    }

    public synchronized void done() {
        running = false;
        notify();
    }
}
