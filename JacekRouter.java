/*
 * Made by Jacek Mikołajczak
 */
package io.github.akiranen.routing;

import io.github.akiranen.core.*;

import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

public class JacekRouter extends ActiveRouter {

    public JacekRouter(Settings s) { super(s); }

    protected JacekRouter(JacekRouter r) { super(r); }

    @Override
    public void update() {
        super.update();
        if (isTransferring() || !canStartTransfer()) {
            return; // transferring, don't try other connections yet
        }

        // Try first the messages that can be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return; // started a transfer, don't try others (yet)
        }

        // Then create a list where to send msg.
        // List is build with hosts what have at least two connections and have enough good ratio.
        // RATIO is calculated by distance and load of the host.

        List<Message> messages = new ArrayList<>(this.getMessageCollection());       // get the list of all messages in this host
        List<Connection> nearbyHosts = new ArrayList<>(this.getConnections());       // get list of host's connections
        List<Connection> potentiallyHostsList = new ArrayList<>();                   // list of potentially new hosts
        List<Double> potentiallyHostsRatios = new ArrayList<>();                     // list of potentially new hosts ratios

        for(Message msg : messages) {

            double nearbyHostRatio;             // init variable for nearby host ratio
            double minRatio = 0;                // minimum ratio
            potentiallyHostsList.clear();       // clear potentiallyHostList for the next msg
            potentiallyHostsRatios.clear();     // clear potentiallyHostsRatios for the next msg

            // forEach nearbyHost
            for (Connection nearbyHost : nearbyHosts) {
                // get number of connections of nearbyHost
                if(nearbyHost.getOtherNode(this.getHost()).getConnections().size() > 1) {
                    // get the number of messages in nearbyHost
                    int numberOfMessages = nearbyHost.getOtherNode(this.getHost()).getMessageCollection().size();
                    // get the distance to nearbyHost
                    double distance = calculateDistance(this.getHost(), nearbyHost.getOtherNode(this.getHost()));
                    // calculate the ratio
                    nearbyHostRatio = calculateRatio(distance, numberOfMessages);
                    // if connection with nearbyHost is good and it is not connection back
                    if(nearbyHostRatio >= minRatio) { //&& nearbyHost.getOtherNode(this.getHost()) != msg.getFrom()) {
                        // then add this connection to potentiallyHost list - size of the list is not greater than 3
                        if(potentiallyHostsList.size() > 2) {
                            potentiallyHostsList.remove(0);     // remove first connection
                            potentiallyHostsRatios.remove(0);   // remove the worst ratio
                        }
                        potentiallyHostsList.add(nearbyHost);         // add this connection
                        potentiallyHostsRatios.add(nearbyHostRatio);  // add this ratio
                        minRatio = potentiallyHostsRatios.get(0);     // set new level of minRatio
                    }
                }
            }

            // send msg to selected connections
            for(Connection con : potentiallyHostsList) {
                startTransfer(msg, con);
            }
        }
    }

    private double calculateDistance(DTNHost host1, DTNHost host2) {

        Coord msgCoords = host1.getLocation();
        double distance = Double.MAX_VALUE;

        if(host2.getPath()!=null) {
            for (Coord pathCoord : host2.getPath().getCoords()) {
                // the distance is calculated by distance of two points in coordinates axis
                double part1 = abs(msgCoords.getX() - pathCoord.getX());
                double part2 = abs(msgCoords.getY() - pathCoord.getY());
                double pathDist = sqrt(pow(part1, 2) + pow(part2, 2));
                if (pathDist < distance) distance = pathDist;
            }
        }
        return distance;
    }

    private double calculateRatio(double distance, int numberOfMessages) {
        // the higher ratio, the better
        return (1/(numberOfMessages+1) + (1/(distance+1))); // magic ratio model
    }

    @Override
    public JacekRouter replicate() {
        return new JacekRouter(this);
    }
}
