package Player1;
import battlecode.common.*;
import java.util.*;
import static java.lang.Math.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int rows, cols;
    static MapLocation rcLoc;
    static MapLocation hq;
    static boolean[][] vis;
    static int birthday;
    static int round;
    static final int prime = 65557;

    // miner
    static MapLocation goRefinery;
    static boolean rush = false;
    static MapLocation rushLoc;
    static Direction randDir;
    static boolean schoolBuilt = false;
    // static boolean schoolSent = false;
    // static MapLocation schoolLoc;
    // static boolean needRefine = false;
    static int grindRound = 120;
    static boolean centerBuilt = false;
    // static MapLocation centerLoc;
    static int buildingIdx = 0;

    // hq
    static int minersBuilt = 0;
    static int minerLimit = 6;

    // design school
    static int landscapersBuilt = 0;
    static int lastBuildRound = 0;
    static Direction buildIn;

    // landscaper
    static int idx = 0;
    static int movesMade = 0;
    static int parity = 0;
    static int totalNeeded = 0;
    static int stepsToEdge = 0;
    static int numSpots = 0;
    static boolean cw;
    static int idx2 = 0;
    static Direction[][] moves = {{Direction.NORTH, Direction.WEST, Direction.WEST, Direction.SOUTH, Direction.SOUTH,
                                  Direction.EAST, Direction.EAST, Direction.NORTH},
                                  {Direction.SOUTH, Direction.SOUTH, Direction.EAST, Direction.EAST, Direction.NORTH,
                                  Direction.NORTH, Direction.WEST, Direction.WEST}};
    // static Direction[][] dig = {{Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST},
    //                             {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST},
    //                             {Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
    //                             {Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
    //                             {Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
    //                             {Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
    //                             {Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST},
    //                             {Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST}};
    static Direction[][] dig = {{Direction.WEST, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST},
                                {Direction.SOUTHWEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST},
                                {Direction.SOUTH, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
                                {Direction.SOUTHEAST, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST, Direction.NORTH, Direction.NORTHEAST},
                                {Direction.EAST, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
                                {Direction.NORTHEAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST},
                                {Direction.NORTH, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST},
                                {Direction.NORTHWEST, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST, Direction.SOUTH, Direction.SOUTHWEST}};
    static Direction[] digHQ = {Direction.WEST, Direction.SOUTHWEST, Direction.SOUTH, Direction.SOUTHEAST, 
                                Direction.EAST, Direction.NORTHEAST, Direction.NORTH, Direction.NORTHWEST};
    static TreeMap<Direction, Integer> map = new TreeMap<>();
    static {
        map.put(Direction.EAST, 0);
        map.put(Direction.NORTHEAST, 1);
        map.put(Direction.NORTH, 2);
        map.put(Direction.NORTHWEST, 3);
        map.put(Direction.WEST, 4);
        map.put(Direction.SOUTHWEST, 5);
        map.put(Direction.SOUTH, 6);
        map.put(Direction.SOUTHEAST, 7);
    }

    // fulfillment center
    static int dronesBuilt = 0;
    static Direction[] dirsFC;

    static Direction[] dirc = {
        Direction.CENTER,
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
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

    // delivery drone
    static boolean holdingEnemy = false, holdingLandscaper = false;
    static int flyDir = 0;
    static int minDist = 15, maxDist = 30;
    static TreeSet<MapLocation> water = new TreeSet<>();
    static TreeMap<Direction, Direction[][]> normal = new TreeMap<>();
    static {
        Direction[][] a1 = {{Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST}, 
                            {Direction.WEST, Direction.SOUTHWEST, Direction.NORTHWEST}};
        normal.put(Direction.NORTH, a1);

        Direction[][] a2 = {{Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH}, 
                            {Direction.NORTHWEST, Direction.WEST, Direction.NORTH}};
        normal.put(Direction.NORTHEAST, a2);

        Direction[][] a3 = {{Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST}, 
                            {Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST}};
        normal.put(Direction.EAST, a3);

        Direction[][] a4 = {{Direction.SOUTHWEST, Direction.WEST, Direction.SOUTH}, 
                            {Direction.NORTHEAST, Direction.EAST, Direction.NORTH}};
        normal.put(Direction.SOUTHEAST, a4);

        Direction[][] a5 = {{Direction.WEST, Direction.SOUTHWEST, Direction.NORTHWEST}, 
                            {Direction.EAST, Direction.NORTHEAST, Direction.SOUTHEAST}};
        normal.put(Direction.SOUTH, a5);

        Direction[][] a6 = {{Direction.NORTHWEST, Direction.WEST, Direction.NORTH}, 
                            {Direction.SOUTHEAST, Direction.EAST, Direction.SOUTH}};
        normal.put(Direction.SOUTHWEST, a6);

        Direction[][] a7 = {{Direction.NORTH, Direction.NORTHWEST, Direction.NORTHEAST}, 
                            {Direction.SOUTH, Direction.SOUTHEAST, Direction.SOUTHWEST}};
        normal.put(Direction.WEST, a7);

        Direction[][] a8 = {{Direction.NORTHEAST, Direction.NORTH, Direction.EAST}, 
                            {Direction.SOUTHWEST, Direction.SOUTH, Direction.WEST}};
        normal.put(Direction.NORTHWEST, a8);
    }

    // static int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
    // static int[] dy = {1, 1, 0, -1, -1, -1, 0, 1};

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        rows = rc.getMapHeight();
        cols = rc.getMapWidth();
        vis = new boolean[rows][cols];
        randDir = randomDirection();
        rcLoc = rc.getLocation();

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(2);
        for (RobotInfo robot : nearbyRobots) {
            if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                hq = robot.location;
                break;
            }
        }

        birthday = rc.getRoundNum();

        // landscaper
        if (rc.getType() == RobotType.LANDSCAPER) {
            birthday = 1;
            nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (rc.senseRobot(robot.ID).type == RobotType.LANDSCAPER) {
                    ++birthday;
                } else if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                    hq = robot.location;
                }
            }

            idx = map.get(hq.directionTo(rcLoc));

            for (Direction d : dir) {
                if (valid(hq.add(d))) ++numSpots;
            }

            int temp = idx;
            MapLocation cur = rcLoc;
            while (valid(cur) && stepsToEdge < 8) {
                ++stepsToEdge;
                cur = cur.add(moves[0][temp % 8]);
                ++temp;
            }
            --stepsToEdge;

            if (birthday > stepsToEdge) {
                cw = true;
                totalNeeded = numSpots - birthday;
            } else {
                cw = false;
                totalNeeded = stepsToEdge - birthday + 1;
            }

            System.out.println(birthday + " " + totalNeeded + " " + stepsToEdge + " " + movesMade + " " + cw);
        }

        while (true) {
            round = rc.getRoundNum();
            rcLoc = rc.getLocation();

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

        // int side = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared()));
        // int[][] ele = new int[side * 2 + 1][side * 2 + 1];
        // for (int i = 0; i < ele.length; ++i) Arrays.fill(ele[i], -99);

        // String print = "";

        // int soupAmt = 0;

        // for (int i = -side; i <= side; i++) {
        //     int height = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared() - i * i));
        //     for (int j = -height; j <= height; j++) {
        //         MapLocation search = new MapLocation(rc.getLocation().x + i, rc.getLocation().y + j);

        //         ele[i + side][j + side] = rc.senseElevation(search);
        //         // soupAmt += rc.senseSoup(search);
        //         print += ele[i + side][j + side] + " ";
        //     }
        //     print += '\n';
        // }
        // System.out.println(print);
        // // System.out.println(soupAmt);

        // int round = rc.getRoundNum();
        // MapLocation rcLoc = rc.getLocation();

        if (50 < round && round < 55) {
            int[] message = new int[7];
            Arrays.fill(message, -1);
            message[0] = round * prime;
            message[2] = rcLoc.x * 64 + rcLoc.y;
            blockchain(message, 1);
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo nearestDrone = null;

        // find nearest drone
        for (RobotInfo robot : nearbyRobots) {
            if (robot.type == RobotType.DELIVERY_DRONE && (nearestDrone == null ||
                rcLoc.distanceSquaredTo(robot.location) < rcLoc.distanceSquaredTo(nearestDrone.location))) {
                nearestDrone = robot;
            }
        }

        if (nearestDrone != null) { // shoot nearest drone if it exists
            rc.shootUnit(nearestDrone.ID);
        } else { // otherwise make a miner
            Direction bestDir = null;
            int maxPotential = 0;
            for (Direction d : dir) {
                if (minersBuilt < minerLimit + 1 && round >= grindRound) { // make miner that can build design school
                    if (!rc.canBuildRobot(RobotType.MINER, d)) continue;

                    int curPotential = 0;
                    for (Direction e : dig[map.get(d)]) {
                        MapLocation potentialBuilding = rcLoc.add(d).add(e);

                        if (valid(potentialBuilding) && !rc.senseFlooding(potentialBuilding) && abs(rc.senseElevation(potentialBuilding) - rc.senseElevation(rcLoc.add(d))) <= 3
                            && !rc.isLocationOccupied(potentialBuilding)) {
                            ++curPotential;

                            // rc.buildRobot(RobotType.MINER, d);
                            // minersBuilt = minerLimit + 1;
                            // break outer;
                        }

                    }

                    if (curPotential > maxPotential) {
                        maxPotential = curPotential;
                        bestDir = d;
                    }
                }
            }

            if (bestDir != null && rc.canBuildRobot(RobotType.MINER, bestDir) && maxPotential >= 2) {
                rc.buildRobot(RobotType.MINER, bestDir);
                minersBuilt = minerLimit + 1;

                return;
            }

            for (Direction d : dir) {
                if (rc.canBuildRobot(RobotType.MINER, d) && minersBuilt < minerLimit && (round < 100 || rc.getTeamSoup() > 200 + 70)) {
                    rc.buildRobot(RobotType.MINER, d);
                    ++minersBuilt;
                    break;
                }
                
            }
        }
    }

    // returns location of closest reachable/visible soup
    static MapLocation soupVision() throws GameActionException {
        // System.out.println(rc.getCurrentSensorRadiusSquared());

        // MapLocation[] soupArray = rc.senseNearbySoup();

        // if (soupArray.length > 0) {
            // int side = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared()));
        //     int side = 5;
        //     MapLocation rcLoc = rc.getLocation();

        //     int[][] mat = new int[side * 2 + 1][side * 2 + 1];
        //     for (int i = 0; i < mat.length; i++) Arrays.fill(mat[i], -1);
        //     mat[side][side] = rc.senseSoup(rcLoc) > 0 ? 1 : 0;

        //     int[][] elevation = new int[side * 2 + 1][side * 2 + 1];
        //     for (int i = 0; i < elevation.length; i++) Arrays.fill(elevation[i], -50);
        //     elevation[side][side] = rc.senseElevation(rcLoc);

        //     ArrayList<MapLocation> bfs = new ArrayList<>();
        //     bfs.add(rcLoc);

        //     while (!bfs.isEmpty()) {
        //         ArrayList<MapLocation> temp = new ArrayList<>();
        //         for (MapLocation loc : bfs) {
        //             for (Direction d : dir) {
        //                 MapLocation go = loc.add(d);
        //                 if (valid(go) && rcLoc.distanceSquaredTo(go) < rc.getCurrentSensorRadiusSquared()
        //                     && abs(elevation[rcLoc.y - loc.y + side][loc.x - rcLoc.x + side] - rc.senseElevation(go)) <= 3
        //                     && !rc.senseFlooding(go) && mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] == -1
        //                     && !rc.isLocationOccupied(go)) {

        //                     if (rc.senseSoup(go) > 0) {
        //                         mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] = 1;
                                
        //                         vis = new boolean[rows][cols];
        //                         return go;
        //                     } else {
        //                         mat[rcLoc.y - go.y + side][go.x - rcLoc.x + side] = 0;
        //                     }

        //                     elevation[rcLoc.y - go.y + side][go.x - rcLoc.x + side] = rc.senseElevation(go);

        //                     temp.add(go);
        //                 }
        //             }
        //         }

        //         bfs = temp;
        //     }

        //     // System.out.println("returned");

        //     Clock.yield();
        //     return null;
        // }

        // return null;


        MapLocation[] soups = rc.senseNearbySoup();
        MapLocation closestSoup = null;
        for (MapLocation loc : soups) {
            if ((closestSoup == null || rcLoc.distanceSquaredTo(loc) < rcLoc.distanceSquaredTo(closestSoup))
                && !(rc.getRoundNum() >= grindRound - 8 && hq.distanceSquaredTo(loc) <= 8)) { // don't go for soup near hq when we're grinding landscapers
                closestSoup = loc;
            }
        }
        
        // if (closestSoup != null) vis = new boolean[rows][cols];

        return closestSoup;
    }

    static void runMiner() throws GameActionException {
        // int round = rc.getRoundNum();

        if (round == grindRound - 8) vis = new boolean[rows][cols];

        System.out.println(1);

        if (!rc.isReady()) return;

        unblock(); // reset vis if no available moves left

        // MapLocation rcLoc = rc.getLocation();

        // build design school
        if (birthday >= grindRound && !schoolBuilt) {
            if (rc.getTeamSoup() < 500) return;

            int cameFrom = map.get(hq.directionTo(rcLoc));

            // for (Direction d : dig[map.get(cameFrom)]) {
            while (buildingIdx < dig[cameFrom].length) {
                Direction d = dig[cameFrom][buildingIdx++];

                if (rc.canBuildRobot(RobotType.DESIGN_SCHOOL, d)) {
                    rc.buildRobot(RobotType.DESIGN_SCHOOL, d);

                    schoolBuilt = true;
                    ++buildingIdx;
                    // schoolLoc = rcLoc.add(d);

                    break;
                }
            }



            return;
        }

        // build fulfillment center
        if (birthday >= grindRound && !centerBuilt) {
            if (rc.getTeamSoup() < 500) return;

            int cameFrom = map.get(hq.directionTo(rcLoc));

            // for (Direction d : dig[map.get(cameFrom)]) {
            while (buildingIdx < dig[cameFrom].length) {
                Direction d = dig[cameFrom][buildingIdx++];
                if (rc.canBuildRobot(RobotType.FULFILLMENT_CENTER, d)) {
                    rc.buildRobot(RobotType.FULFILLMENT_CENTER, d);

                    centerBuilt = true;
                    ++buildingIdx;
                    // centerLoc = rcLoc.add(d);

                    break;
                }
            }

            return;
        }

        boolean needRefine = rc.getSoupCarrying() == RobotType.MINER.soupLimit;

        MapLocation soupVision = soupVision();

        // check for soup sites in blockchain
        Transaction[][] blockchain = new Transaction[5][];
        for (int i = 0; i < min(5, round); i++) {
            blockchain[i] = rc.getBlock(round - i - 1);
        }
        for (Transaction[] block : blockchain) {
            for (Transaction t : block) {
                int[] msg = t.getMessage();
                if (msg[0] % prime == 0 && msg[0] > 0) {
                    if (msg[1] != -1) {
                        MapLocation soupMsg = new MapLocation(msg[1] / 64, msg[1] % 64);
                        if (rushLoc == null || rcLoc.distanceSquaredTo(soupMsg) < rcLoc.distanceSquaredTo(rushLoc)) {
                            rush = true;
                            rushLoc = soupMsg;
                            // vis = new boolean[rows][cols];
                        }
                    }

                    // if (msg[3] == 1) {
                    //     landscapersDone = true;
                    // }
                }
            }
        }

        if (rush && soupVision == null) vis = new boolean[rows][cols];

        // System.out.println((soupVision == null) + " " + needRefine + " " + rush);
        // if (rush) {
        //     System.out.println(rushLoc.x + " " + rushLoc.y);
        // }

        if (round > grindRound - 8 && rcLoc.distanceSquaredTo(hq) <= 2) { // get away from hq if it's grindround
            for (Direction d : dir) {
                MapLocation go = rcLoc.add(d);
                if (valid(go) && rc.canMove(d) && go.distanceSquaredTo(hq) > rcLoc.distanceSquaredTo(hq)) {
                    rc.move(d);

                    return;
                }
            }
        } else if (needRefine) {
            // look for visible refineries
            goRefinery = null;
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (robot.type == RobotType.REFINERY || (robot.type == RobotType.HQ && round < grindRound - 8)) {
                    if (goRefinery == null || rcLoc.distanceSquaredTo(robot.location) < rcLoc.distanceSquaredTo(goRefinery)) {
                        goRefinery = robot.location;
                    }
                }
            }

            if (goRefinery == null) { // build refinery if can't see one
                for (Direction d : dir) {
                    if (rc.canBuildRobot(RobotType.REFINERY, d) && rcLoc.add(d).distanceSquaredTo(hq) > 8) {
                        rc.buildRobot(RobotType.REFINERY, d);
                        goRefinery = rc.adjacentLocation(d);

                        return;
                    }
                }
            } else { // move toward refinery, refine when you reach it
                Direction[] dirs = sortDirs(goRefinery);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (goRefinery.equals(go) && rc.canDepositSoup(d)) {
                        rc.depositSoup(d, rc.getSoupCarrying());
                        goRefinery = null;
                        
                        return;
                    }

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) { // && !vis[rows - 1 - go.y][go.x]
                        if (round > grindRound - 8 && rcLoc.distanceSquaredTo(hq) > 2 && go.distanceSquaredTo(hq) <= 2) continue;

                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        
                        return;
                    }
                }
            }
        } else if (soupVision != null) { // if soup is visible
            // put soup location on blockchain if you're the first to find it
            if (!rush) {
                int[] message = new int[7];
                Arrays.fill(message, -1);
                message[0] = round * prime;
                message[1] = soupVision.x * 64 + soupVision.y;
                blockchain(message, 1);
            }

            Direction[] dirs = sortDircs(soupVision);

            for (Direction d : dirs) {
                MapLocation go = rc.adjacentLocation(d);
                if (!valid(go)) continue;

                // mine soup if you can
                if (soupVision.equals(go) && rc.canMineSoup(d)) {
                    rc.mineSoup(d);
                    
                    return;
                }

                // move towards soup
                if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) { // && !vis[rows - 1 - go.y][go.x]
                    if (round > grindRound - 8 && rcLoc.distanceSquaredTo(hq) > 2 && go.distanceSquaredTo(hq) <= 2) continue;

                    vis[rows - 1 - go.y][go.x] = true;
                    rc.move(d);
                    
                    return;
                }
            }
        } else if (rush) {
            if (rcLoc.distanceSquaredTo(rushLoc) < 10 && soupVision == null || round > grindRound - 8 && rushLoc.distanceSquaredTo(hq) <= 2) { // stop rushing if you're at the rush location but no soup left or if it's grindround and the soup is close to hq
                rush = false;
                rushLoc = null;
            } else { // move towards rush location
                Direction[] dirs = sortDirs(rushLoc);

                for (Direction d : dirs) {
                    MapLocation go = rc.adjacentLocation(d);
                    if (!valid(go)) continue;

                    if (rc.canMove(d) && !rc.senseFlooding(go) && !vis[rows - 1 - go.y][go.x]) { // && !vis[rows - 1 - go.y][go.x]
                        if (round > grindRound - 8 && rcLoc.distanceSquaredTo(hq) > 2 && go.distanceSquaredTo(hq) <= 2) continue;

                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        
                        return;
                    }
                }
            }
        } else {
            // if no soup visible or on blockchain, go in a straight line in a random direction; repeat when reach wall

            MapLocation go = rc.adjacentLocation(randDir);
            ArrayList<Integer> shuffle = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7));
            Collections.shuffle(shuffle);
            for (Integer d : shuffle) {
                if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go) && (round <= grindRound - 8 || go.distanceSquaredTo(hq) > 8
                    || go.distanceSquaredTo(hq) > rcLoc.distanceSquaredTo(hq))) break;

                randDir = dir[d];
                go = rc.adjacentLocation(dir[d]);
            }

            if (valid(go) && rc.canMove(randDir) && !rc.senseFlooding(go)) {
                if (!(round > grindRound - 8 && rcLoc.distanceSquaredTo(hq) > 2 && go.distanceSquaredTo(hq) <= 2)) {
                    rc.move(randDir);

                    return;
                }
            }
        }

    }

    static void runDesignSchool() throws GameActionException {
        if (hq == null) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(8, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                    hq = robot.location;
                    break;
                }
            }

            for (Direction d : dir) {
                MapLocation go = rcLoc.add(d);

                if (valid(go) && !rc.isLocationOccupied(go) && !rc.senseFlooding(go) && abs(rc.senseElevation(rcLoc) - rc.senseElevation(go)) <= 3 && go.distanceSquaredTo(hq) <= 2) {
                    buildIn = d;
                    // break;
                }

                if (valid(hq.add(d))) ++numSpots;
            }
        }

        System.out.println(numSpots);
        
        if (landscapersBuilt < numSpots) {// && round > lastBuildRound + 10
            if (rc.canBuildRobot(RobotType.LANDSCAPER, buildIn)) {
                rc.buildRobot(RobotType.LANDSCAPER, buildIn);
                ++landscapersBuilt;
                lastBuildRound = round;

                return;
            }

            for (Direction d : dir) {
                if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
                    rc.buildRobot(RobotType.LANDSCAPER, d);
                    ++landscapersBuilt;
                    lastBuildRound = round;

                    return;
                }
            }
        }
        // } else if (numSpots == 8) {
        //     for (Direction d : dir) {
        //         if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
        //             rc.buildRobot(RobotType.LANDSCAPER, d);
        //         }
        //     }

        // if (landscapersBuilt < 8) {
        //     for (Direction d : dir) {
        //         if (rc.canBuildRobot(RobotType.LANDSCAPER, d)) {
        //             rc.buildRobot(RobotType.LANDSCAPER, d);
        //             ++landscapersBuilt;

        //             return;
        //         }
        //     }
        // }
    }

    static void runLandscaper() throws GameActionException {
        System.out.println('a');
        if (rcLoc.distanceSquaredTo(hq) > 2) return;

        if (numSpots == 8) {
            System.out.println(idx2);
            idx = map.get(hq.directionTo(rcLoc));

            // System.out.println(idx);

            if (idx2 % 3 == 2) {
                if (rc.canMove(moves[0][idx % 8])) {
                    rc.move(moves[0][idx % 8]);
                    ++idx2;

                    return;
                } else if (rc.isLocationOccupied(rcLoc.add(moves[0][idx % 8]))) {
                    ++idx2;
                } else if (rc.senseElevation(rcLoc.add(moves[0][idx % 8])) < rc.senseElevation(rcLoc) - 3) {
                    if (rc.getDirtCarrying() > 0) {
                        if (rc.canDepositDirt(moves[0][idx % 8])) {
                            rc.depositDirt(moves[0][idx % 8]);

                            return;
                        }
                    } else {
                        for (Direction d : dig[idx % 8]) {
                            if (rc.canDigDirt(d)) {
                                rc.digDirt(d);
                                
                                return;
                            }
                        }
                    }
                } else if (rc.senseElevation(rcLoc.add(moves[0][idx % 8])) > rc.senseElevation(rcLoc) + 3) {
                    if (rc.getDirtCarrying() > 0) {
                        for (Direction d : dig[idx % 8]) {
                            if (rc.canDepositDirt(d)) {
                                rc.depositDirt(d);

                                return;
                            }
                        }
                    } else {
                        if (rc.canDigDirt(moves[0][idx % 8])) {
                            rc.digDirt(moves[0][idx % 8]);

                            return;
                        }
                    }
                }
            }

            if (idx2 % 3 == 0) { // dig
                for (Direction d : dig[idx % 8]) {
                    if (rc.canDigDirt(d)) {
                        rc.digDirt(d);
                        ++idx2;

                        return;
                    }
                }

                if (rc.canMove(moves[0][idx % 8])) {
                    rc.move(moves[0][idx % 8]);
                    idx2 = 0;
                }
            } else if (idx2 % 3 == 1) { // deposit
                if (rc.canDepositDirt(Direction.CENTER)) {
                    rc.depositDirt(Direction.CENTER);
                    ++idx2;

                    return;
                } else if (rc.canMove(moves[0][idx % 8])) {
                    rc.move(moves[0][idx % 8]);
                    idx2 = 0;
                }
            }

            return;
        }

        if (rc.canDigDirt(digHQ[idx % 8])) {
            rc.digDirt(digHQ[idx % 8]);

            return;
        } else if (rc.getDirtCarrying() == 25) {
            for (Direction d : dig[idx % 8]) {
                if (rc.canDepositDirt(d)) {
                    rc.depositDirt(d);

                    return;
                }
            }
        }

        int rot = cw ? 1 : 0;

        if (movesMade < totalNeeded && abs(rc.senseElevation(rcLoc.add(moves[rot][idx % 8])) - rc.senseElevation(rcLoc)) > 3 && !rc.isLocationOccupied(rcLoc.add(moves[rot][idx % 8]))) {
            if (rc.senseElevation(rcLoc.add(moves[rot][idx % 8])) < rc.senseElevation(rcLoc)) {
                if (rc.getDirtCarrying() > 0) {
                    if (rc.canDepositDirt(moves[rot][idx % 8])) {
                        rc.depositDirt(moves[rot][idx % 8]);
                    }
                } else {
                    for (Direction d : dig[idx % 8]) {
                        if (rc.canDigDirt(d)) {
                            rc.digDirt(d);
                            break;
                        }
                    }
                }
            } else {
                if (rc.getDirtCarrying() > 0) {
                    for (Direction d : dig[idx % 8]) {
                        if (rc.canDepositDirt(d)) {
                            rc.depositDirt(d);
                        }
                    }
                } else {
                    if (rc.canDigDirt(moves[rot][idx % 8])) {
                        rc.digDirt(moves[rot][idx % 8]);
                    }
                }
            }
        } else if (movesMade == totalNeeded) {
            if (rc.getDirtCarrying() > 0) {
                if (rc.canDepositDirt(Direction.CENTER)) {
                    rc.depositDirt(Direction.CENTER);
                }
            } else {
                for (Direction d : dig[idx % 8]) {
                    if (rc.canDigDirt(d)) {
                        rc.digDirt(d);
                        break;
                    }
                }
            }
        } else {
            if (rc.canMove(moves[rot][idx % 8])) {
                rc.move(moves[rot][idx % 8]);

                idx += cw ? 7 : 1;

                ++movesMade;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        if (hq == null) {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(8, rc.getTeam());
            for (RobotInfo robot : nearbyRobots) {
                if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
                    hq = robot.location;
                    break;
                }
            }

            dirsFC = sortDirs(hq);
        }

        if (dronesBuilt < 2 || dronesBuilt < 20 && rc.getTeamSoup() > 1100) {
            for (int dd = 7; dd > -1; --dd) {
                Direction d = dirsFC[dd];

                if (rc.canBuildRobot(RobotType.DELIVERY_DRONE, d)) {
                    rc.buildRobot(RobotType.DELIVERY_DRONE, d);

                    ++dronesBuilt;
                }
            }
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        if (hq == null) {
            // RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, rc.getTeam());
            // for (RobotInfo robot : nearbyRobots) {
            //     if (rc.senseRobot(robot.ID).type == RobotType.HQ) {
            //         hq = robot.location;
            //         break;
            //     }
            // }
        }

        int hqIdx = 51;
        while (hq == null && hqIdx < 55) {
            Transaction[] blockchain = rc.getBlock(hqIdx);
            for (Transaction t : blockchain) {
                int[] message = t.getMessage();

                if (message[0] % prime == 0 && message[0] > 0 && message[2] != -1) {
                    hq = new MapLocation(message[2] / 64, message[2] % 64);
                    break;
                }
                
            }


            ++hqIdx;
        }

        System.out.println(holdingEnemy + " " + holdingLandscaper);

        int dist = (rcLoc.x - hq.x) * (rcLoc.x - hq.x) + (rcLoc.y - hq.y) * (rcLoc.y - hq.y);

        int side = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared()));
        for (int i = -side; i <= side; i++) {
            int height = (int) floor(sqrt(rc.getCurrentSensorRadiusSquared() - i * i));
            for (int j = -height; j <= height; j++) {
                MapLocation search = new MapLocation(rcLoc.x + i, rcLoc.y + j);
                if (valid(search) && rc.senseFlooding(search)) {
                    water.add(search);
                }
            }
        }

        if (!rc.isReady()) return;

        if (rc.getTeamSoup() > 1000) {
            minDist = 4;
            maxDist = 15;
        }

        if (minDist == 4) {

            droneUnblock();
            if (rcLoc.distanceSquaredTo(hq) == 8) return;
            if (rcLoc.distanceSquaredTo(hq) > 5) {
                Direction[] dirs = sortDircs(hq);
                for (Direction d : dirs) {
                    MapLocation go = rcLoc.add(d);
                    if (rc.canMove(d) && !vis[rows - 1 - go.y][go.x]) {
                        rc.move(d);
                        vis[rows - 1 - go.y][go.x] = true;
                        break;
                    }
                }
            }

            return;
        }

        if (holdingEnemy) {
            if (water.size() > 0) {
                MapLocation closestWater = null;
                for (MapLocation loc : water) {
                    if (closestWater == null || rcLoc.distanceSquaredTo(loc) < rcLoc.distanceSquaredTo(closestWater)) {
                        closestWater = loc;
                    }
                }

                Direction[] dirs = sortDirs(closestWater);
                for (Direction d : dirs) {
                    if (rc.canDropUnit(d)) {
                        rc.dropUnit(d);
                        holdingEnemy = false;

                        return;
                    }

                    if (rc.canMove(d)) {
                        rc.move(d);

                        return;
                    }
                }
            }
            // do case where no water visible
        } else if (holdingLandscaper) {
            Direction[] dirs = sortDirs(hq);
            for (Direction d : dirs) {
                MapLocation go = rcLoc.add(d);
                if (!valid(go)) continue;

                if (go.distanceSquaredTo(hq) <= 2 && rc.canDropUnit(d)) {
                    rc.dropUnit(d);
                    holdingLandscaper = false;

                    return;
                }

                if (rc.canMove(d)) {
                    rc.move(d);

                    return;
                }
            }
        }

        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        RobotInfo closestEnemy = null, closestLandscaper = null;
        for (RobotInfo robot : nearbyRobots) {
            if (rc.senseRobot(robot.ID).team == rc.getTeam().opponent()) {
                if (closestEnemy == null || rcLoc.distanceSquaredTo(robot.location) < rcLoc.distanceSquaredTo(closestEnemy.location)) {
                    closestEnemy = robot;
                }
            } else if (rc.senseRobot(robot.ID).type == RobotType.LANDSCAPER) {
                if (robot.location.distanceSquaredTo(hq) > 2 && (closestLandscaper == null || rcLoc.distanceSquaredTo(robot.location) < rcLoc.distanceSquaredTo(closestLandscaper.location))) {
                    closestLandscaper = robot;
                }
            }
        }

        if (closestEnemy != null) {
            if (rc.canPickUpUnit(closestEnemy.ID)) {
                rc.pickUpUnit(closestEnemy.ID);
                holdingEnemy = true;
            } else {
                Direction[] dirs = sortDirs(closestEnemy.location);
                for (Direction d : dirs) {
                    if (rc.canMove(d)) {
                        rc.move(d);

                        break;
                    }
                }
            }

            return;
        } else if (closestLandscaper != null) {
            if (rc.canPickUpUnit(closestLandscaper.ID)) {
                rc.pickUpUnit(closestLandscaper.ID);
                holdingLandscaper = true;

                return;
            } else {
                Direction[] dirs = sortDirs(closestLandscaper.location);
                for (Direction d : dirs) {
                    if (rc.canMove(d)) {
                        rc.move(d);

                        return;
                    }
                }
            }
        } else if (dist <= minDist || dist >= maxDist) {
            for (Direction d : dir) {
                MapLocation go = rcLoc.add(d);
                int goDist = go.distanceSquaredTo(hq);

                if (valid(go) && (min(abs(goDist - minDist), abs(goDist - maxDist)) < min(abs(dist - minDist), abs(dist - maxDist)) || minDist < goDist && goDist < maxDist)  && rc.canMove(d)) {
                    rc.move(d);

                    return;
                }
            }
        } else {
            Direction cameFrom = hq.directionTo(rcLoc);
            for (Direction d : normal.get(cameFrom)[flyDir % 2]) {
                MapLocation go = rcLoc.add(d);
                int goDist = go.distanceSquaredTo(hq);

                if (valid(go) && rc.canMove(d) && minDist < goDist && goDist < maxDist) {
                    rc.move(d);

                    return;
                }
            }

            ++flyDir;
            for (Direction d : normal.get(cameFrom)[flyDir % 2]) {
                MapLocation go = rcLoc.add(d);
                int goDist = go.distanceSquaredTo(hq);

                if (valid(go) && rc.canMove(d) && minDist < goDist && goDist < maxDist) {
                    rc.move(d);

                    return;
                }
            }

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
    static void droneUnblock() throws GameActionException {
        for (Direction d : dir) {
            MapLocation go = rcLoc.add(d);
            if (valid(go) && !vis[rows - 1 - go.y][go.x] && rc.canMove(d)) return;
        }

        System.out.println("reset");
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

    static Direction[] sortDircs(MapLocation source) throws GameActionException {
        Direction[] dirs = new Direction[9];
        for (int d = 0; d < 9; ++d) dirs[d] = dirc[d];

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
we know it's our transaction if it's divisible by prime
things stored by transaction: rc.getRoundNum() * prime, location of soup found, location of a built refinery,
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

next step
figure out why hq isn't shooting drones
start building as soon as you have the soup ?
use the grind miner to build a fulfillment center to build a drone
have the drone patrol around the hq looking for enemy troops
if you see an enemy troop put it in the water
have the hq tell the drone through the blockchain where the enemy troop is (hq has more vision)

dealing with edge/corner hq's
dealing with elevation around hq

build army of drones and attack enemy hq

landscaper:
upon spawn, count how many valid tiles surround the hq, assign this value to totalNeeded
count how many counterclockwise steps would be required to reach the edge (this would be infinite if you're not
at an edge/corner)
if the number of spawned landscapers is greater than or equal to that number of steps, go clockwise instead
and set movesMade to totalNeeded - 1 - ccwSteps

*/
