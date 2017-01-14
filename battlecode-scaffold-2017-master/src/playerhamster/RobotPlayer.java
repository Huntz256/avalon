package playerhamster;
import battlecode.common.*;

/**
 * Player Hamster
 **/
public strictfp class RobotPlayer 
{
    private static RobotController rc;
    private static final Direction[] dirList = new Direction[4];
    private static MapLocation[] initialEnemyArchonLocation;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException 
    {
        // The RobotController object. Used to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        initDirList();

        initialEnemyArchonLocation = rc.getInitialArchonLocations(rc.getTeam().opponent());

        // Separate controls into different methods for each RobotType.
        switch (rc.getType()) 
        {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                if (Math.random() < 0.5)
                    runDefenseLumberjack();
                else
                    runAttackLumberjack();
                break;
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
        }
	}

    /**
     * Archon code executed every round.
     */
    private static void runArchon() throws GameActionException
    {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .15)
                {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);
                
                // Gain victory points (10 bullets = 1 victory point)
                //  If we have enough victory points to win, then win
                if (rc.getTeamBullets() >= 10000 - rc.getTeamVictoryPoints() * 10)
                {
                    rc.donate(10000 - rc.getTeamVictoryPoints() * 10);
                }

                //  If we have a decent amount of bullets, buy one victory point every two turns
                else if (rc.getTeamBullets() >= 250 && Math.random() < .5)
                {
                	rc.donate(10);
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } 
            catch (Exception e) 
            {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Gardener code executed every round.
     */
	private static void runGardener() throws GameActionException
	{
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);

                // Generate a random direction
                Direction dir = randomDirection();

                TreeInfo[] trees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 1,  rc.getTeam());
                MapLocation myLocation = rc.getLocation();

                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .008)
                {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                }
                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .04 && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }

                // Build tree
                else if (rc.getTeamBullets() > GameConstants.BULLET_TREE_COST)
                {
                    //This is supposed to plant trees on a grid but the trees do not appear to be on a grid
                    for (int i = 0; i < 4; i++)
                    {
                        MapLocation p = rc.getLocation().add(dirList[i], GameConstants.GENERAL_SPAWN_OFFSET + GameConstants.BULLET_TREE_RADIUS + rc.getType().bodyRadius);
                        if (modGood(p.x, 6, 0.2f) && modGood(p.y, 6, 0.2f) && rc.canPlantTree(dir) && Math.random() < .4)
                        {
                            rc.plantTree(dir);
                            break;
                        }
                    }
                }


                Direction toArchon = myLocation.directionTo(archonLoc);
                Direction awayFromArchon = toArchon.rotateRightDegrees(180);
                TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 1, Team.NEUTRAL);

                // Water adjacent team trees
                if (trees.length > 0 && Math.random() < 0.6)
                {
                    rc.water(trees[0].getID());
                }

                // Shake neutral trees if we can
                else if (neutralTrees.length > 0 && Math.random() < 0.01)
                {
                    int neutralTreeID = neutralTrees[0].getID();
                    if (rc.canShake(neutralTreeID))
                    {
                        rc.shake(neutralTreeID);
                    }
                }

                // Randomly move such that they tend to move away from our Arch
                else if (Math.random() < 0.1)
                {
                	tryMove(awayFromArchon);
                }
                else
                {
                	tryMove(randomDirection());
                }


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            }
            catch (Exception e)
            {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Solider code executed every round.
     */
    private static void runSoldier() throws GameActionException
    {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(7, enemy);

                // If there are some...
                if (robots.length > 0) 
                {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) 
                    {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                MapLocation myLocation = rc.getLocation();

                if (Math.random() < 0.1)
                {
                    // Move toward an enemy archon
                    tryMove(myLocation.directionTo(initialEnemyArchonLocation[0]));
                }
                else
                {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } 
            catch (Exception e) 
            {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Lumberjack code executed every round.
     */
    private static void runDefenseLumberjack() throws GameActionException
    {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) 
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try 
            {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) 
                {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } 
                else
                {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(7, enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) 
                    {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    }
                    else
                    {
                        TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, Team.NEUTRAL);

                        if (neutralTrees.length > 0 && Math.random() < 0.99)
                        {
                            rc.chop(neutralTrees[0].getID());
                        }
                        else
                        {
                            int xPos = rc.readBroadcast(0);
                            int yPos = rc.readBroadcast(1);
                            MapLocation archonLoc = new MapLocation(xPos,yPos);
                            MapLocation myLocation = rc.getLocation();
                            Direction toArchon = myLocation.directionTo(archonLoc);

                            // Move toward our archon
                            if (Math.random() < 0.2)
                            {
                                tryMove(toArchon);
                            }
                            else
                            {
                                tryMove(randomDirection());
                            }
                        }
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } 
            catch (Exception e) 
            {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Lumberjack code executed every round.
     */
    private static void runAttackLumberjack() throws GameActionException
    {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked())
                {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                }
                else
                {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(7, enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0)
                    {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    }
                    else
                    {
                        TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, Team.NEUTRAL);

                        if (neutralTrees.length > 0 && Math.random() < 0.99)
                        {
                            rc.chop(neutralTrees[0].getID());
                        }
                        else
                        {
                            MapLocation myLocation = rc.getLocation();

                            /*
                            if (Math.random() < 0.5)
                            {
                                MapLocation[] broadcastingLocations = rc.senseBroadcastingRobotLocations();
                                float distance;
                                float maxDistance = 0;
                                int maxIndex = 0;
                                for (int i = 0; i < broadcastingLocations.length; i++) {
                                    distance = myLocation.distanceTo(broadcastingLocations[i]);

                                    if (distance > maxDistance) {
                                        maxDistance = distance;
                                        maxIndex = i;
                                    }
                                }

                                tryMove(myLocation.directionTo(broadcastingLocations[maxIndex]));
                            }*/

                            // Move toward an enemy archon!
                            //if (Math.random() < 0.4)
                            //{
                                // Move toward an enemy archon
                                tryMove(myLocation.directionTo(initialEnemyArchonLocation[0]));
                            //}
                            //else
                            //{
                                // Move randomly
                            //    tryMove(randomDirection());
                            //}
                        }
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            }
            catch (Exception e)
            {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Tank code executed every round.
     */
    private static void runTank() throws GameActionException
    {
        System.out.println("I'm a tank!");

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                MapLocation myLocation = rc.getLocation();

                // Move toward an enemy archon
                if (Math.random() < 0.1)
                {
                    // Move toward an enemy archon
                    tryMove(myLocation.directionTo(initialEnemyArchonLocation[0]));
                }
                else
                {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            }
            catch (Exception e)
            {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Scout code executed every round.
     */
    private static void runScout() throws GameActionException
    {
        System.out.println("I'm a scout!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                MapLocation myLocation = rc.getLocation();

                // See if there are any enemy robots within striking range (distance 1 from scout's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.SCOUT.bodyRadius + 1, enemy);

                if (rc.canFireSingleShot())
                {
                    // Fire a bullet in the direction of the enemy.
                    rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                }

                // Move toward an enemy archon
                if (Math.random() < 0.05)
                {
                    // Move toward an enemy archon
                    tryMove(myLocation.directionTo(initialEnemyArchonLocation[0]));
                }
                else
                {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            }
            catch (Exception e)
            {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }
    }

    private static boolean modGood(float number, float spacing, float fraction)
    {
        return (number%spacing) < spacing*fraction;
    }

    private static void initDirList()
    {
        for(int i = 0; i < 4; i++)
        {
            float radians = (float)(-Math.PI + 2 * Math.PI *((float)i)/4);
            dirList[i] = new Direction(radians);
        }
    }
    
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    private static Direction randomDirection()
    {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    private static boolean tryMove(Direction dir) throws GameActionException
    {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    private static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException
    {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) 
    {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
