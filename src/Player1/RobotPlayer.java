package Player1;
import battlecode.common.*;
import java.util.*;
import static java.lang.Math.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int rows, cols;
    static boolean[][] vis;
    static int row, col;
    static int turnCount;
    static boolean gotoHQ = false;
    static MapLocation hq;
    static boolean rush = false;
    static MapLocation rushLoc;
    static boolean foundRush = false;

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
        turnCount = 0;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2);
        for (RobotInfo robot : nearbyRobots) {
            if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                hq = robot.location;
                break;
            }
        }

        while (true) {
            ++turnCount;
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

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        if (!rc.isReady()) return;

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
        if (nearbyRobots.length > 0) {
            Arrays.sort(nearbyRobots, new Comparator<RobotInfo>() {
                public int compare(RobotInfo a, RobotInfo b) {
                    return rc.getLocation().distanceSquaredTo(a.location)
                         - rc.getLocation().distanceSquaredTo(b.location);
                }
            });

            rc.shootUnit(nearbyRobots[0].ID);
        } else {
            for (Direction d : dir) {
                if (tryBuild(RobotType.MINER, d)) break;
            }
        }
    }

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
        if (!rc.isReady()) return;

        unblock();

        MapLocation[] soupVision = soupVision();

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
                MapLocation aLoc = new MapLocation(a.getMessage()[1] / (cols + 64), a.getMessage()[1] % (cols + 64));
                MapLocation bLoc = new MapLocation(b.getMessage()[1] / (cols + 64), b.getMessage()[1] % (cols + 64));

                return rc.getLocation().distanceSquaredTo(aLoc) - rc.getLocation().distanceSquaredTo(bLoc);
            }
        });
        if (ours.length > 0) {
            rush = true;
            rushLoc = new MapLocation(ours[0].getMessage()[1] / (cols + 64), ours[0].getMessage()[1] % (cols + 64));
            vis = new boolean[rows][cols];
        }

        System.out.println(rush);

        Clock.yield();

        if (soupVision.length > 0) {
            if (rush) {
                rush = false;
                rushLoc = null;
            } else {
                blockchain(soupVision[0]);
            }

            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                // System.out.println(1);

                gotoHQ = true;
            } else {
                // System.out.println(2);
                
                MapLocation soupLocation = soupVision[0];

                Direction[] dirs = sortDirs(soupLocation);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (soupLocation.distanceSquaredTo(go) <= 2 && rc.canMineSoup(d)) {
                        rc.mineSoup(d);
                        break;
                    }

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }
        } else if (rush) {
            if (rc.getLocation().distanceSquaredTo(rushLoc) <= 2 && soupVision.length == 0) {
                rush = false;
                rushLoc = null;
            } else {

                Direction[] dirs = sortDirs(rushLoc);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }
        } else {
            if (rc.getSoupCarrying() > 0) {
                gotoHQ = true;
            } else {
                ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
                Collections.shuffle(shuffle);

                for (Integer d : shuffle) {
                    MapLocation go = rc.adjacentLocation(dir[d]);
                    if (!valid(go)) continue;

                    if (rc.canMove(dir[d]) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                        rc.move(dir[d]);
                        vis[rows - 1 - go.y][go.x] = true;

                        break;
                    }
                }
            }
        }

        if (gotoHQ) {
            Direction[] dirs = sortDirs(hq);

            for (Direction d : dirs) {
                MapLocation go = rc.adjacentLocation(d);
                if (!valid(go)) continue;

                if (hq.distanceSquaredTo(go) <= 2 && rc.canDepositSoup(d)) {
                    rc.depositSoup(d, rc.getSoupCarrying());
                    gotoHQ = false;
                    break;
                }
                
                if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) {
                    rc.move(d);
                    vis[rows - 1 - go.y][go.x] = true;
                    break;
                }
            }
        }
    }

    static void runRefinery() throws GameActionException {

    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {

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
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
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


    static void blockchain(MapLocation soup) throws GameActionException {
        int[] message = new int[7];
        message[0] = rc.getRoundNum() * 65557;
        message[1] = soup.x * (cols + 64) + soup.y;

        // if (rc.getTeamSoup() > 0) rc.submitTransaction(message, min(rc.getTeamSoup(), 15));
        if (rc.getTeamSoup() > 0) {
            rc.submitTransaction(message, 1);
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
*/