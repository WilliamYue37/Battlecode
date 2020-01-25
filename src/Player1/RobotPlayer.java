package Player1;
import battlecode.common.*;
import java.util.*;
import static java.lang.Math.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int rows, cols;
    static boolean[][] vis;
    static MapLocation hq;
    static int birthday;

    // miner
    static MapLocation goRefinery;
    static boolean rush = false;
    static MapLocation rushLoc;
    static Direction randDir;
    static int randStepsTaken = 0;
    static int stepsFromHQ = 0;
    static boolean schoolBuilt = false;
    static boolean schoolSent = false;
    static MapLocation schoolLoc;

    // hq
    static int minersBuilt = 0;

    // landscaper
    static int idx = 0;
    static Direction[] moves = {Direction.NORTH, Direction.WEST, Direction.WEST, Direction.SOUTH, Direction.SOUTH,
                                Direction.EAST, Direction.EAST};
    static Direction[][] dig = {{Direction.NORTHEAST, Direction.SOUTHEAST},
                                {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST},
                                {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
                                {Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
                                {Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
                                {Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
                                {Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST},
                                {Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST}};
    static Direction[] deposit = {Direction.SOUTH, Direction.SOUTH, Direction.EAST, Direction.EAST, Direction.NORTH,
                                  Direction.NORTH, Direction.WEST, Direction.WEST};

    static Direction[] dir = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };

    // static int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
    // static int[] dy = {1, 1, 0, -1, -1, -1, 0, 1};

    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};


    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        rows = rc.getMapHeight();
        cols = rc.getMapWidth();
        vis = new boolean[rows][cols];
        randDir = randomDirection();

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2);
        for (RobotInfo robot : nearbyRobots) {
            if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                hq = robot.location;
                break;
            }
        }

        birthday = rc.getRoundNum();

        while (true) {
            try {
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                }

                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (!rc.isReady()) return;

        if (50 < rc.getRoundNum() && rc.getRoundNum() < 55) {
            int[] message = new int[7];
            Arrays.fill(message, -1);
            message[0] = rc.getRoundNum() * 65557;
            message[2] = rc.getLocation().x * 64 + rc.getLocation().y;
            blockchain(message, 2);
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo nearestDrone = null;

        // find nearest drone
        for (RobotInfo robot : nearbyRobots) {
            if (nearestDrone == null || robot.type == RobotType.DELIVERY_DRONE && 
                rc.getLocation().distanceSquaredTo(robot.location) < rc.getLocation().distanceSquaredTo(nearestDrone.location)) {
                nearestDrone = robot;
            }
        }

        if (nearestDrone != null) { // shoot nearest drone if it exists
            rc.shootUnit(nearestDrone.ID);
        } else { // otherwise generate a miner if round < 150
            if (rc.getRoundNum() < 150) {
                for (Direction d : dir) {
                    if (rc.canBuildRobot(RobotType.MINER, d) && minersBuilt < 5 && (rc.getRoundNum() < 100 || rc.getTeamSoup() > 200 + 70)) {
                        rc.buildRobot(RobotType.MINER, d);
                        ++minersBuilt;
                        break;
                    }
                }
            }
        }
    }

    // returns location of closest reachable soup
    static MapLocation soupVision() throws GameActionException {
        // int side = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared()));
        // MapLocation rcLoc = rc.getLocation();

        // int[][] mat = new int[side * 2 + 1][side * 2 + 1];
        // for (int i = 0; i < mat.length; i++) Arrays.fill(mat[i], -1);
        // mat[side][side] = rc.senseSoup(rcLoc) > 0 ? 1 : 0;

        // ArrayList<MapLocation> bfs = new ArrayList<>();
        // bfs.add(rcLoc);

        // // ArrayList<MapLocation> soup = new ArrayList<>();
        // MapLocation closestSoup = mat[side][side] == 0 ? null : rcLoc;
        // if (mat[side][side] > 0) return rcLoc;

        // while (!bfs.isEmpty()) {
        //     ArrayList<MapLocation> temp = new ArrayList<>();
        //     for (MapLocation loc : bfs) {
        //         for (Direction d : dir) {
        //             MapLocation go = loc.add(d);
        //             if (valid(go) && rcLoc.distanceSquaredTo(go) <= rc.getCurrentSensorRadiusSquared()
        //                 && abs(rc.senseElevation(loc) - rc.senseElevation(go)) <= 3
        //                 && !rc.senseFlooding(go) && mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] == -1
        //                 && !rc.isLocationOccupied(go)) {

        //                 if (rc.senseSoup(go) > 0) {
        //                     mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] = 1;
                            
        //                     if (closestSoup == null) {
        //                         closestSoup = go;
        //                         return closestSoup;
        //                     }
        //                 } else {
        //                     mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] = 0;
        //                 }

        //                 temp.add(go);
        //             }
        //         }
        //     }

        //     bfs = temp;
        // }

        // // System.out.println("returned");

        // return closestSoup;

        ArrayList<MapLocation> soup = new ArrayList<>();

        int base = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared()));
        for (int i = -base; i <= base; i++) {
            int height = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared() - i * i));
            for (int j = -height; j <= height; j++) {
                MapLocation search = new MapLocation(rc.getLocation().x + i, rc.getLocation().y + j);
                if (rc.canSenseLocation(search) && rc.senseSoup(search) > 0) {
                    soup.add(search);
                }
            }
        }

        MapLocation[] ret = soup.toArray(new MapLocation[0]);
        Arrays.sort(ret, new Comparator<MapLocation>() {
            public int compare(MapLocation a, MapLocation b) {
                return rc.getLocation().distanceSquaredTo(a)
                     - rc.getLocation().distanceSquaredTo(b);
            }
        });

        if (ret.length > 0) vis = new boolean[rows][cols];

        return ret.length > 0 ? ret[0] : null;
    }

    static void runMiner() throws GameActionException {

        if (rc.getRoundNum() == 190) vis = new boolean[rows][cols];

        // System.out.println(1);

        if (!rc.isReady()) return;

        unblock(); // reset vis if no available moves left

        MapLocation rcLoc = rc.getLocation();

        // build design school
        if (birthday == 2 && !schoolBuilt && rc.getRoundNum() > 200) {
            System.out.println(true);

            if (rcLoc.equals(hq.add(Direction.EAST))) {
                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, Direction.EAST)) {
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, Direction.EAST);
                    schoolBuilt = true;
                    schoolLoc = hq.translate(2, 0);

                    // Clock.yield();

                    if (rc.canMove(Direction.NORTHEAST)) rc.move(Direction.NORTHEAST);
                }

                return;
            }

            Direction[] dirs = sortDirs(hq.add(Direction.EAST));

            for (Direction d : dirs) {
                MapLocation go = rc.adjacentLocation(d);
                
                if (valid(go) && rc.canMove(d) && !rc.senseFlooding(go)) {
                    rc.move(d);
                    return;
                }
            }
        }

        // check for soup sites in blockchain
        Transaction[] blockchain = rc.getBlock(rc.getRoundNum() - 1);
        for (Transaction t : blockchain) {
            if (t.getMessage()[0] % 65557 == 0 && t.getMessage()[1] != -1) {
                MapLocation soupMsg = new MapLocation(t.getMessage()[1] / 64, t.getMessage()[1] % 64);
                if (rushLoc == null || rcLoc.distanceSquaredTo(soupMsg) < rcLoc.distanceSquaredTo(rushLoc)) {
                    rush = true;
                    rushLoc = soupMsg;
                    vis = new boolean[rows][cols];
                }
            }
        }


        goRefinery = null;

        // look for visible refineries
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type == RobotType.REFINERY || (robot.type == RobotType.HQ && rc.getRoundNum() < 190)) {
                if (goRefinery == null || rcLoc.distanceSquaredTo(robot.location) < rcLoc.distanceSquaredTo(goRefinery)) {
                    goRefinery = robot.location;
                }
            }
        }

        MapLocation soupVision = soupVision();
        Clock.yield();


        // if soup is visible
        if (soupVision != null) {
            // put soup location on blockchain if you're the first to find it
            if (!rush) {
                int[] message = new int[7];
                Arrays.fill(message, -1);
                message[0] = rc.getRoundNum() * 65557;
                message[1] = soupVision.x * 64 + soupVision.y;
                blockchain(message, 3);
            }

            // go to refinery if full soup capacity
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                // build refinery if can't see one
                if (goRefinery == null) {
                    if (randStepsTaken < 10) { // take 10 random steps before building refinery
                        randStepsTaken++;

                        ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
                        Collections.shuffle(shuffle);

                        for (Integer d : shuffle) {
                            MapLocation go = rc.adjacentLocation(dir[d]);
                            if (!valid(go)) continue;

                            if (rc.canMove(dir[d]) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) { // && !vis[rows - 1 - go.y][go.x]
                                if (rc.getRoundNum() > 190) {
                                    if (rcLoc.distanceSquaredTo(hq) > 2) {
                                        if (go.distanceSquaredTo(hq) <= 2) continue;
                                    } else {
                                        if (go.distanceSquaredTo(hq) <= rcLoc.distanceSquaredTo(hq)) continue;
                                    }
                                }

                                rc.move(dir[d]);
                                vis[rows - 1 - go.y][go.x] = true;

                                break;
                            }
                        }
                    } else {
                        boolean built = false;

                        for (Direction d : dir) {
                            if (rc.canBuildRobot(RobotType.REFINERY, d) && rcLoc.add(d).distanceSquaredTo(hq) > 2 && !rcLoc.add(d).equals(hq.add(Direction.EAST))) {
                                rc.buildRobot(RobotType.REFINERY, d);
                                goRefinery = rc.adjacentLocation(d);

                                randStepsTaken = 0;

                                built = true;
                                break;
                            }
                        }

                        if (built) Clock.yield();
                    }
                }
            } else {
                Direction[] dirs = sortDirs(soupVision);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    // mine soup if you can
                    if (soupVision.equals(go) && rc.canMineSoup(d)) {
                        rc.mineSoup(d);
                        break;
                    }

                    // move towards soup
                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        if (rc.getRoundNum() > 190) {
                            if (rcLoc.distanceSquaredTo(hq) > 2) {
                                if (go.distanceSquaredTo(hq) <= 2) continue;
                            } else {
                                if (go.distanceSquaredTo(hq) <= rcLoc.distanceSquaredTo(hq)) continue;
                            }
                        }

                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }
        } else if (rush) {
            // stop rushing if you're at the rush location but no soup left
            if (rcLoc.distanceSquaredTo(rushLoc) <= 2 && soupVision == null) {
                rush = false;
                rushLoc = null;
            } else { // move towards rush location
                Direction[] dirs = sortDirs(rushLoc);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        if (rc.getRoundNum() > 190) {
                            if (rcLoc.distanceSquaredTo(hq) > 2) {
                                if (go.distanceSquaredTo(hq) <= 2) continue;
                            } else {
                                if (go.distanceSquaredTo(hq) <= rcLoc.distanceSquaredTo(hq)) continue;
                            }
                        }

                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }
        } else {
            // if no soup visible or on blockchain, go in a straight line in a random direction; repeat when reach wall
            MapLocation go = rc.adjacentLocation(randDir);
            ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
            Collections.shuffle(shuffle);
            for (Integer d : shuffle) {
                if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go)) break;

                randDir = dir[d];
                go = rc.adjacentLocation(dir[d]);
            }

            if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go)) {
                boolean doMove = true;

                if (rc.getRoundNum() > 190) {
                    if (rcLoc.distanceSquaredTo(hq) > 2) {
                        if (go.distanceSquaredTo(hq) <= 2) doMove = false;
                    } else {
                        if (go.distanceSquaredTo(hq) <= rcLoc.distanceSquaredTo(hq)) doMove = false;
                    }
                }
                
                if (doMove) {
                    rc.move(randDir);
                }
            }
        }

        if (goRefinery != null) { // move toward the nearest refinery if you need to refine
            Direction[] dirs = sortDirs(goRefinery);

            for (Direction d : dirs) {
                MapLocation go = rc.adjacentLocation(d);
                if (!valid(go)) continue;

                if (goRefinery.equals(go) && rc.canDepositSoup(d)) {
                    rc.depositSoup(d, rc.getSoupCarrying());
                    goRefinery = null;
                    break;
                }

                if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) { // && !vis[rows - 1 - go.y][go.x]
                    if (rc.getRoundNum() > 190) {
                        if (rcLoc.distanceSquaredTo(hq) > 2) {
                            if (go.distanceSquaredTo(hq) <= 2) continue;
                        } else {
                            if (go.distanceSquaredTo(hq) <= rcLoc.distanceSquaredTo(hq)) continue;
                        }
                    }

                    rc.move(d);
                    vis[rows - 1 - go.y][go.x] = true;
                    break;
                }
            }
        }
    }

    static void runDesignSchool() throws GameActionException {
        if (rc.canBuildRobot(RobotType.LANDSCAPER, Direction.WEST)) {
            rc.buildRobot(RobotType.LANDSCAPER, Direction.WEST);
        }
    }

    static void runLandscaper() throws GameActionException {
        if (idx >= moves.length || !rc.canMove(moves[idx])) {
            if (rc.getDirtCarrying() > 0 && rc.isLocationOccupied(rc.getLocation().add(deposit[idx]))) {
                if (rc.canDepositDirt(deposit[idx])) {
                    rc.depositDirt(deposit[idx]);
                }
            } else {
                for (Direction d : dig[idx]) {
                    if (rc.canDigDirt(d)) {
                        rc.digDirt(d);
                        break;
                    }
                }
            }
        } else {
            rc.move(moves[idx++]);
        }
    }

    static Direction randomDirection() {
        return dir[(int) (Math.random() * dir.length)];
    }

    static void blockchain(int[] message, int cost) throws GameActionException {
        if (rc.getTeamSoup() > 0) {
            rc.submitTransaction(message, min(rc.getTeamSoup(), cost));
        }
    }

    static void unblock() throws GameActionException {
        for (Direction d : dir) {
            MapLocation go = rc.adjacentLocation(d);
            if (valid(go) && !vis[rows - 1 - go.y][go.x] && rc.canMove(d) && !rc.senseFlooding(go)) return;
        }

        vis = new boolean[rows][cols];
    }

    static boolean valid(MapLocation loc) {
        return loc != null && 0 <= rows - 1 - loc.y && rows - 1 - loc.y < rows && 0 <= loc.x && loc.x < cols;
    }

    static Direction[] sortDirs(MapLocation source) throws GameActionException {
        Direction[] dirs = new Direction[8];
        for (int d = 0; d < 8; ++d) dirs[d] = dir[d];

        Arrays.sort(dirs, new Comparator<Direction> () {
            public int compare(Direction a, Direction b) {
                MapLocation adjA = rc.adjacentLocation(a), adjB = rc.adjacentLocation(b);
                if (!valid(adjA)) {
                    if (!valid(adjB)) return 0;
                    else return 1;
                } else {
                    if (!valid(adjB)) return -1;
                    else return source.distanceSquaredTo(adjA) 
                              - source.distanceSquaredTo(adjB);
                }
            }
        });

        return dirs;
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }
    static void runNetGun() throws GameActionException {

    }
    static void runDeliveryDrone() throws GameActionException {
        
    }
    static void runFulfillmentCenter() throws GameActionException {
        
    }
}


/*
each miner does its own independent random dfs to try to find soup (store vis array)
(one problem is trapping yourself in your own vis boundary)
once a miner finds soup, put it on blockchain for other miners to go to
other miners continue to search while the delivery drone delivers each miner to the soup site until the soup site
is exhausted
at the beginning of a miner's turn, check blockchain. if soup found and there's a drone available, then stop moving
and put location on blockchain for drone to see and come pick up, otherwise continue searching.
at the beginning of a drone's turn, check blockchain. if soup found and there's a miner who wants pickup, go to it
and take it to soup site location.
once the found soup site is exhausted, update blockchain to say that the soup site is exhausted
// multiple blockchain states: empty, soup found, soup exhausted, building design schools instead of looking for 
// soup now
each miner stores distance traveled (to compute if it should build a refinery or go back to the hq to refine
the soup it finds)

after a certain turn, or after a certain amount of soup collected, 3 miners stop searching for soup and instead
start building design schools
the other miners can continue searching for soup

new idea
only make up to 8 miners
each one goes in a different direction and does a thorough bfs search of the area
count how much soup in site
drones have to watch out for enemy hq/net gun
store locations of enemy hq/netguns found

new idea
miners do their own independent dfs, but once there are no available squares left to move to, reset vis
and continue searching

next step
maybe have a second 'true' vis to avoid miners retracing the same paths they've already gone to
building refineries and putting their locations in the blockchain to limit miner travel time
put soup sites on blockchain
sense elevation to avoid being flooded
if miner is trapped, send drone

each miner submits its own transactions
we know it's our transaction if it's divisible by 65557
things stored by transaction: rc.getRoundNum() * 65557, location of soup found, location of a built refinery,
location of trapped miner
idea for many transactions: map id to meaning of location

have each miner go in a straight line in a random direction. switch direction when you can no longer move in 
this direction

current problems
miners running into each other
knowing where and when to build refineries and fulfillment centers


next step
have the first miner built build a design school and have the design school build landscapers
have the landscapers stand around the hq
block the blockchain
*/
