package AEP.nodeUtilities;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Contains methods useful to setup the system and maintain its consistency
 */
public class Utilities {

    /**
     * Checks if a certain port is available
     *
     * @param port the port to be checked
     * @return true if the port is available, false otherwise
     */
    private static boolean available(int port) {
//        System.out.println("--------------Testing port " + port);
        Socket s = null;
        try {
            s = new Socket("localhost", port);

            // If the code makes it this far without an exception it means
            // something is using the port and has responded.
//            System.out.println("--------------Port " + port + " is not available");
            return false;
        } catch (IOException e) {
//            System.out.println("--------------Port " + port + " is available");
            return true;
        } finally {
            if( s != null){
                try {
                    s.close();
                } catch (IOException e) {
                    throw new RuntimeException("You should handle this error." , e);
                }
            }
        }
    }

    /**
     * Gets an available port between two thresholds
     *
     * @param min the minimum threshold
     * @param max the maximum threshold
     *
     * @return a random available port between the thresholds
     */
    public static Integer getAvailablePort(Integer min, Integer max){
        Integer randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
        while (!available(randomNum)){
            randomNum = ThreadLocalRandom.current().nextInt(min, max + 1);
        }
        return randomNum;
    }

}
