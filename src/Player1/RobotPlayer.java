package Player1;
import battlecode.common.*;
import java.util.*;
import static java.lang.Math.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int rows, cols;
    static boolean[][] vis;
    static int row, col;
    // static int turnCount;
    static MapLocation goRefinery;
    static MapLocation hq;
    static boolean rush = false;
    static MapLocation rushLoc;
    static Direction randDir;
    static int randStepsTaken = 0;

    static int minersBuilt = 0;

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
        // turnCount = 0;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2);
        for (RobotInfo robot : nearbyRobots) {
            if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                hq = robot.location;
                break;
            }
        }

        while (true) {
            // ++turnCount;
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

        if (50 < rc.getRoundNum() && rc.getRoundNum() < 75) {
            int[] message = new int[7];
            message[0] = rc.getRoundNum() * 65557;
            message[2] = rc.getLocation().x * 64 + rc.getLocation().y;
            blockchain(message, rc.getRoundNum() - 40);
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
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
                    if (rc.canBuildRobot(RobotType.MINER, d) && minersBuilt < 10 && (rc.getRoundNum() < 100 || rc.getTeamSoup() > 200 + 70)) {
                        rc.buildRobot(RobotType.MINER, d);
                        ++minersBuilt;
                        break;
                    }
                }
            }
        }
    }

    // returns array of visible locations with soup sorted by proximity to current location
    static MapLocation[] soupVision() throws GameActionException {
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

        return ret;
    }

    static void runMiner() throws GameActionException {
        System.out.println(1);

        boolean printed = false;

        unblock(); // reset vis if no available moves left

        if (rc.getRoundNum() < 150 || rc.getRoundNum() > 200) { // leave the 150-200 range to let miners spread out
            // if (200 < rc.getRoundNum() && rc.getRoundNum() < 250) { // build design schools during 200-250 range
            //     for (Direction d : dir) {
            //         if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
            //             rc.buildRobot(RobotType.DESIGN_SCHOOL, d);
            //             return;
            //         }
            //     }
            // }

            // check for soup sites in blockchain
            Transaction[] blockchain = rc.getBlock(rc.getRoundNum() - 1);
            ArrayList<Transaction> oursList = new ArrayList<>();
            for (Transaction t : blockchain) {
                if (t.getMessage()[0] % 65557 == 0) {
                    oursList.add(t);
                }
            }
            Transaction[] ours = oursList.toArray(new Transaction[0]);
            Arrays.sort(ours, new Comparator<Transaction>() {
                public int compare(Transaction a, Transaction b) {
                    MapLocation aLoc = new MapLocation(a.getMessage()[1] / 64, a.getMessage()[1] % 64);
                    MapLocation bLoc = new MapLocation(b.getMessage()[1] / 64, b.getMessage()[1] % 64);

                    return rc.getLocation().distanceSquaredTo(aLoc) - rc.getLocation().distanceSquaredTo(bLoc);
                }
            });
            if (ours.length > 0) {
                rush = true;
                rushLoc = new MapLocation(ours[0].getMessage()[1] / 64, ours[0].getMessage()[1] % 64);
                vis = new boolean[rows][cols];
            }

            Clock.yield();

            // look for visible refineries
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam());
            ArrayList<MapLocation> refineriesList = new ArrayList<>();
            for (RobotInfo robot : nearbyRobots) {
                if (robot.type == RobotType.REFINERY || robot.type == RobotType.HQ) {
                    refineriesList.add(robot.location);
                }
            }
            MapLocation[] refineries = refineriesList.toArray(new MapLocation[0]);
            Arrays.sort(refineries, new Comparator<MapLocation>() {
                public int compare(MapLocation a, MapLocation b) {
                    return rc.getLocation().distanceSquaredTo(a) - rc.getLocation().distanceSquaredTo(b);
                }
            });

            MapLocation[] soupVision = soupVision();


            // if soup is visible
            if (soupVision.length > 0) {
                // put soup location on blockchain if you're the first to find it
                if (!rush) {
                    int[] message = new int[7];
                    message[0] = rc.getRoundNum() * 65557;
                    message[1] = soupVision[0].x * 64 + soupVision[0].y;
                    blockchain(message, 10);
                }

                // go to refinery if full soup capacity
                if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                    // go to closest visible refinery
                    if (refineries.length > 0) {
                        goRefinery = refineries[0];
                    } else { // build refinery if none visible
                        if (randStepsTaken < 10) { // take 10 random steps before building refinery
                            randStepsTaken++;

                            ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
                            Collections.shuffle(shuffle);

                            for (Integer d : shuffle) {
                                MapLocation go = rc.adjacentLocation(dir[d]);
                                if (!valid(go)) continue;

                                if (rc.canMove(dir[d]) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                                    rc.move(dir[d]);
                                    vis[rows - 1 - go.y][go.x] = true;
                                    System.out.println("moved");
                                    printed = true;

                                    break;
                                }
                            }
                        } else {
                            for (Direction d : dir) {
                                if (rc.canBuildRobot(RobotType.REFINERY, d)) {
                                    rc.buildRobot(RobotType.REFINERY, d);
                                    goRefinery = rc.adjacentLocation(d);

                                    randStepsTaken = 0;
                                }
                            }

                            // refine at hq if can't build refinery
                            if (goRefinery == null) goRefinery = hq;
                        }
                    }
                } else {
                    MapLocation soupLocation = soupVision[0];

                    Direction[] dirs = sortDirs(soupLocation);

                    for (Direction d : dirs) {
                        MapLocation go = rc.adjacentLocation(d);
                        if (!valid(go)) continue;

                        // mine soup if you can
                        if (soupLocation.distanceSquaredTo(go) <= 2 && rc.canMineSoup(d)) {
                            rc.mineSoup(d);
                            System.out.println("mined");
                            printed = true;
                            break;
                        }

                        // move towards soup
                        if (rc.canMove(d) && !rc.senseFlooding(go)) {
                            rc.move(d);
                            System.out.println("moved");
                            printed = true;
                            break;
                        }
                    }
                }
            } else if (rush) { // stop rushing if you're at the rush location but no soup left
                if (rc.getLocation().distanceSquaredTo(rushLoc) <= 2 && soupVision.length == 0) {
                    rush = false;
                    rushLoc = null;
                } else { // move towards rush location
                    Direction[] dirs = sortDirs(rushLoc);

                    for (Direction d : dirs) {
                        MapLocation go = rc.adjacentLocation(d);
                        if (!valid(go)) continue;

                        if (rc.canMove(d) && !rc.senseFlooding(go)) {
                            rc.move(d);
                            System.out.println("moved");
                            printed = true;
                            break;
                        }
                    }
                }
            } else {
                // go refine if you have soup but can't see soup
                if (false) { // actually let's only go refine if we're full
                    if (refineries.length > 0) {
                        goRefinery = refineries[0];
                    } else {
                        for (Direction d : dir) {
                            if (rc.canBuildRobot(RobotType.REFINERY, d)) {
                                rc.buildRobot(RobotType.REFINERY, d);
                                goRefinery = rc.adjacentLocation(d);
                            }
                        }
                        
                        if (goRefinery == null) goRefinery = hq;
                    }
                } else { // if no soup visible or on blockchain, go in a straight line in a random direction; repeat when reach wall
                    MapLocation go = rc.adjacentLocation(randDir);
                    ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
                    Collections.shuffle(shuffle);
                    for (Integer d : shuffle) {
                        if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go)) break;

                        randDir = dir[d];
                        go = rc.adjacentLocation(dir[d]);
                    }

                    if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go)) {
                        rc.move(randDir);
                        System.out.println("moved");
                        printed = true;
                    }
                }
            }

            if (goRefinery != null) { // move toward the nearest refinery if you need to refine
                vis = new boolean[rows][cols];

                Direction[] dirs = sortDirs(goRefinery);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (goRefinery.distanceSquaredTo(rc.getLocation()) <= 2 && rc.canDepositSoup(d)) {
                        rc.depositSoup(d, rc.getSoupCarrying());
                        goRefinery = null;
                        break;
                    }

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        rc.move(d);
                        System.out.println("moved");
                        printed = true;
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }
        } else if (150 < rc.getRoundNum() && rc.getRoundNum() < 200) { // let miners spread out
            ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
            Collections.shuffle(shuffle);

            for (Integer d : shuffle) {
                MapLocation go = rc.adjacentLocation(dir[d]);
                if (!valid(go)) continue;

                if (rc.canMove(dir[d]) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                    rc.move(dir[d]);
                    System.out.println("moved");
                    printed = true;
                    vis[rows - 1 - go.y][go.x] = true;

                    break;
                }
            }
        }

        if (!printed) System.out.println("--------------------");
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        for (Direction d : dir) {
            if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
                rc.buildRobot(RobotType.LANDSCAPER, d);
                break;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        for (Direction dir : dir)
            tryBuild(RobotType.DELIVERY_DRONE, dir);
    }

    static void runLandscaper() throws GameActionException {
        
    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }

    static Direction randomDirection() {
        return dir[(int) (Math.random() * dir.length)];
    }

    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    static boolean tryMove() throws GameActionException {
        for (Direction dir : dir)
            if (tryMove(dir))
                return true;
        return false;
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
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

*/