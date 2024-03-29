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
    private static boolean sentGoingToDieSignal = false;

    // These channels keep track of our archon's location
    private static final int ARCHON_CHANNEL_ONE = 0;
    private static final int ARCHON_CHANNEL_TWO = 1;

    // This channel keeps track of how many scouts we've built
    private static final int SCOUT_NUM_CHANNEL = 2;

    // This channel keeps track of how many lumberjacks we've built
    private static final int LUMBERJACK_NUM_CHANNEL = 7;

    // These channels keep track of the enemy archon's location
    private static final int ENEMY_ARCHON_X_CHANNEL = 3;
    private static final int ENEMY_ARCHON_Y_CHANNEL = 4;
    private static final int LOST_ENEMY_ARCHON_CHANNEL = 5;

    private static final int GAME_STATE_CHANNEL = 6;

    // Used by lumberjacks.
    // Is the "state" of a lumberjack, used for basic pathing to enemy archon
    // A "state diagram":                                      __
    //                                                       /   |
    //   start -> 0 --> 1 --> 2 --> 3 --> 4 --> 5 --> 6 --> 7 <-/
    //                  |           |           |           |
    //                  v           v           v           v
    //                  0           0           0           0
    private static int lumberjackState = 0;

    // The enemy archon's location
    private static MapLocation enemyArchonLocation;

    // A unit's previous location
    private static int previousX = 0, previousY = 0;

    // A unit's current location
    private static MapLocation myLocation;

    // The two directions pointing perpendicular to the direction to the enemy archon
    private static Direction enemyArchonRightPerp;
    private static Direction enemyArchonLeftPerp;

    private static int initialNumberOfEnemyArchons;

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
        initialNumberOfEnemyArchons = initialEnemyArchonLocation.length;
        System.out.println("yo:" + initialNumberOfEnemyArchons);

        // Broadcast the initial enemy archon's location at the start of the game
        if (rc.readBroadcast(GAME_STATE_CHANNEL) == 0)
        {
            rc.broadcast(ENEMY_ARCHON_X_CHANNEL, (int) initialEnemyArchonLocation[0].x);
            rc.broadcast(ENEMY_ARCHON_Y_CHANNEL, (int) initialEnemyArchonLocation[0].y);
            rc.broadcast(GAME_STATE_CHANNEL, 1);
        }

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
                // Sense nearby bullets
                BulletInfo[] bullets = rc.senseNearbyBullets();

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                // Build more gardeners if archon is in danger
                if (bullets.length > 0 && rc.canHireGardener(dir))
                {
                    rc.hireGardener(dir);
                }
                else if (rc.canHireGardener(dir) && Math.random() < .12)
                {
                    rc.hireGardener(dir);
                }

                // Try to dodge bullets if there are any sensed nearby
                if (bullets.length > 0)
                {
                    for (BulletInfo bi : bullets)
                    {
                        if (willCollideWithMe(bi))
                        {
                            trySidestep(bi);
                        }
                    }
                }

                // Otherwise, move randomly
                else
                {
                    tryMove(randomDirection());
                }

                // Broadcast archon's location for other robots on the team to know
                myLocation = rc.getLocation();
                rc.broadcast(0, (int) myLocation.x);
                rc.broadcast(1, (int) myLocation.y);

                // Gain victory points (10 bullets = 1 victory point)
                //  If we have enough victory points to win, then win
                if (rc.getTeamBullets() >= 10000 - rc.getTeamVictoryPoints() * 10)
                {
                    rc.donate(10000 - rc.getTeamVictoryPoints() * 10);
                }

                //  If we have a decent amount of bullets, buy one victory point every twenty turns
                else if (rc.getTeamBullets() >= 250 && Math.random() < .05)
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

    // Updates the enemy archon location and the enemy archon location status
    private static void updateEnemyArchonLocation() throws GameActionException
    {
        MapLocation prevArchonLocation = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));
        RobotInfo[] robots = rc.senseNearbyRobots(10,rc.getTeam().opponent());

        // Updates the enemy archon location stored in the team-shared array
        boolean foundEnemyArchon = false;
        enemyArchonLocation = prevArchonLocation;
        for (RobotInfo robot : robots)
        {
            if (robot.getType() == RobotType.ARCHON)
            {
                foundEnemyArchon = true;
                enemyArchonLocation = robot.getLocation();
                rc.broadcast(ENEMY_ARCHON_X_CHANNEL, (int) enemyArchonLocation.x);
                rc.broadcast(ENEMY_ARCHON_Y_CHANNEL, (int) enemyArchonLocation.y);
            }
        }

        // Update whether we have lost the enemy archon's location or not
        //  This should only occur if the enemy archon is dead, in which case we need to find
        //  the other enemy archon(s) if they exist
        if (!foundEnemyArchon)
        {
            rc.broadcast(LOST_ENEMY_ARCHON_CHANNEL, 1);
            //System.out.println("Lost!");
            //rc.setIndicatorDot(enemyArchonLocation, 255, 0, 0);
            rc.setIndicatorLine(myLocation, enemyArchonLocation, 255, 0, 0);
        }
        else
        {
            rc.broadcast(LOST_ENEMY_ARCHON_CHANNEL, 0);
            //rc.setIndicatorDot(enemyArchonLocation, 0, 255, 0);
            rc.setIndicatorLine(myLocation, enemyArchonLocation, 0, 255, 0);
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
                int xPos = rc.readBroadcast(ARCHON_CHANNEL_ONE);
                int yPos = rc.readBroadcast(ARCHON_CHANNEL_TWO);
                MapLocation archonLoc = new MapLocation(xPos, yPos);

                //Dodge bullets
                BulletInfo[] bullets = rc.senseNearbyBullets();
                for (BulletInfo bi : bullets)
                {
                    if (willCollideWithMe(bi))
                    {
                        trySidestep(bi);
                    }
                }

                // Generate a random direction
                Direction dir = randomDirection();

                TreeInfo[] trees = rc.senseNearbyTrees(7, rc.getTeam());
                myLocation = rc.getLocation();


                int numScouts = rc.readBroadcast(SCOUT_NUM_CHANNEL);
                int numLumberjack = rc.readBroadcast(LUMBERJACK_NUM_CHANNEL);

                // Build one scout if we have less than one scout
                if (numScouts < 1 && rc.canBuildRobot(RobotType.SCOUT, dir) && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.SCOUT, dir);
                    rc.broadcast(SCOUT_NUM_CHANNEL, numScouts + 1);
                }

                // Build a lumberjack if we have less than two lumberjacks
                else if (numLumberjack < 2 && rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                    rc.broadcast(LUMBERJACK_NUM_CHANNEL, numLumberjack + 1);
                }

                // Build a lumberjack if we have less than bullets / 100 lumberjacks
                else if (numLumberjack < rc.getTeamBullets() * 0.01 && rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                    rc.broadcast(LUMBERJACK_NUM_CHANNEL, numLumberjack + 1);
                }

                // Randomly attempt to build a soldier or lumberjack in this direction
                else if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .04 && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                }
                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .05 && rc.isBuildReady())
                {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }

                // Build tree
                else if (rc.getTeamBullets() > GameConstants.BULLET_TREE_COST && Math.random() < 0.15)
                {
                    if (rc.canPlantTree(dir))
                    {
                        rc.plantTree(dir);
                    }
                }

                Direction toArchon = myLocation.directionTo(archonLoc);
                Direction awayFromArchon = toArchon.rotateRightDegrees(180);
                TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 1, Team.NEUTRAL);

                // Water adjacent team trees
                if (trees.length > 0 && rc.canWater(trees[0].getID()) && Math.random() < 0.6)
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

                else if (Math.random() < 0.25)
                {
                    tryMove(awayFromArchon);
                }
                else if (Math.random() < 0.8)
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
        MapLocation enemyArchon;

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                //Dodge bullets
                BulletInfo[] bullets = rc.senseNearbyBullets();

                for (BulletInfo bi : bullets)
                {
                    if (willCollideWithMe(bi))
                    {
                        if (!rc.hasMoved())
                        {
                            trySidestep(bi);
                        }
                    }
                }

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

                        rc.setIndicatorLine(rc.getLocation(), robots[0].location, 0,0,255);
                    }
                }

                myLocation = rc.getLocation();

                if (!rc.hasMoved())
                {
                    if (Math.random() < 0.8)
                    {
                        enemyArchon = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));

                        // Move toward an enemy archon
                        tryMove(myLocation.directionTo(enemyArchon));
                    }
                    else
                    {
                        // Move randomly
                        tryMove(randomDirection());
                    }
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

    /*
     * Lumberjack code
     */
    private static void AttackLumberjackMoveTowardEnemyArchon()  throws GameActionException
    {
        //System.out.println("Moving Toward Enemy Archon!");
        //System.out.println("lumberjackState:" + lumberjackState);

        TreeInfo[] neutralTrees = rc.senseNearbyTrees(2, Team.NEUTRAL);

        //System.out.println("neutralTrees.length:" + neutralTrees.length);
        //System.out.println("previouslocation: x=" + previousX + " y=" + previousY);
        //System.out.println("currentLocation: x=" + myLocation.x + " y=" + myLocation.y);

        // If there are nearby neutral trees and this lumberjack is in state 7, then chop them down
        if (neutralTrees.length > 0 && lumberjackState == 7)
        {
            rc.chop(neutralTrees[0].getID());
        }
        else if (lumberjackState == 7)
        {
            lumberjackState = 0;
        }
        else
        {
            enemyArchonLocation = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));

            if (lumberjackState == 6)
            {
                // Try to move toward an enemy archon
                tryMove(myLocation.directionTo(enemyArchonLocation));

                // If we didn't move, then change our state
                if (previousX == ((int) myLocation.x) && previousY == ((int) myLocation.y))
                {
                    lumberjackState++;
                }
                else
                {
                    lumberjackState = 0;
                }
            }
            else if (lumberjackState == 5)
            {
                enemyArchonRightPerp = myLocation.directionTo(enemyArchonLocation).rotateRightRads((float) (Math.PI * 0.5));
                tryMove(enemyArchonRightPerp);
                lumberjackState++;
            }
            else if (lumberjackState == 4)
            {
                // Try to move toward an enemy archon
                tryMove(myLocation.directionTo(enemyArchonLocation));

                // If we didn't move, then change our state
                if (previousX == ((int) myLocation.x) && previousY == ((int) myLocation.y))
                {
                    lumberjackState++;
                }
                else
                {
                    lumberjackState = 0;
                }
            }
            else if (lumberjackState == 3)
            {
                enemyArchonRightPerp = myLocation.directionTo(enemyArchonLocation).rotateRightRads((float) (Math.PI * 0.5));
                tryMove(enemyArchonRightPerp);
                lumberjackState++;
            }
            else if (lumberjackState == 2)
            {
                // Try to move toward an enemy archon
                tryMove(myLocation.directionTo(enemyArchonLocation));

                // If we didn't move, then change our state
                if (previousX == ((int) myLocation.x) && previousY == ((int) myLocation.y))
                {
                    lumberjackState++;
                }
                else
                {
                    lumberjackState = 0;
                }
            }
            else if (lumberjackState == 1)
            {
                enemyArchonLeftPerp = myLocation.directionTo(enemyArchonLocation).rotateLeftRads((float) (Math.PI * 0.5));
                tryMove(enemyArchonLeftPerp);
                lumberjackState++;
            }
            else
            {
                // Try to move toward an enemy archon
                tryMove(myLocation.directionTo(enemyArchonLocation));

                // If we didn't move, then change our state
                if (previousX == ((int)myLocation.x) && previousY == ((int)myLocation.y))
                {
                    lumberjackState++;
                }
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
        myLocation = rc.getLocation();

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if (robots.length > 0 && !rc.hasAttacked())
                {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                    System.out.println("Strike()!");
                }
                else
                {
                    //Dodge bullets
                    BulletInfo[] bullets = rc.senseNearbyBullets();
                    if (bullets.length > 0)
                    {
                        for (BulletInfo bi : bullets)
                        {

                            if (willCollideWithMe(bi))
                            {
                                System.out.println("Lumberjack tried to dodge a bullet");
                                if (!rc.hasMoved())
                                {
                                    trySidestep(bi);
                                }
                            }
                        }
                    }
                    else if (!rc.hasMoved())
                    {
                        // Search for robots within fraction of sight radius
                        robots = rc.senseNearbyRobots(5, enemy);

                        // If there is a robot, move towards it
                        if (robots.length > 0)
                        {
                            MapLocation enemyLocation = robots[0].getLocation();
                            Direction toEnemy = myLocation.directionTo(enemyLocation);

                            tryMove(toEnemy);
                            System.out.println("Moved toward enemy robot");
                        }
                        else
                        {
                            AttackLumberjackMoveTowardEnemyArchon();
                        }
                    }
                }

                // Update previous location
                previousX = (int) myLocation.x;
                previousY = (int) myLocation.y;

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
        MapLocation enemyArchon;

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
               myLocation = rc.getLocation();

                // Move toward an enemy archon
                if (Math.random() < 0.9)
                {
                    enemyArchon = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));

                    // Move toward an enemy archon
                    tryMove(myLocation.directionTo(enemyArchon));
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
        MapLocation enemyArchon;

        int scoutState = 0;

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                myLocation = rc.getLocation();

                // Update enemy archon location
                updateEnemyArchonLocation();

                // See if there are any enemy robots within sight radius
                RobotInfo[] robots = rc.senseNearbyRobots(10, enemy);

                // If we have lost the location of the enemy archon and a scout has
                //   found an enemy archon, broadcast its location to all robots
                if (rc.readBroadcast(LOST_ENEMY_ARCHON_CHANNEL) == 1)
                {
                    boolean foundArchon = false;

                    for (RobotInfo robot : robots)
                    {
                        if (robot.getType() == RobotType.ARCHON)
                        {
                            foundArchon = true;
                            MapLocation enemyArchonLocation = robot.getLocation();
                            rc.broadcast(ENEMY_ARCHON_X_CHANNEL, (int) enemyArchonLocation.x);
                            rc.broadcast(ENEMY_ARCHON_Y_CHANNEL, (int) enemyArchonLocation.y);
                            rc.setIndicatorDot(myLocation, 0, 0, 255);
                        }
                    }

                    if (!foundArchon)
                    {
                        rc.setIndicatorDot(myLocation, 255, 0, 0);
                    }
                }
                else
                {
                    rc.setIndicatorDot(myLocation, 0, 255, 0);
                }

                // Shake nearby trees to gain bullets (note: 2.5 is the scout's stride radius)
                TreeInfo[] trees = rc.senseNearbyTrees((float) 2.5, Team.NEUTRAL);

                if (trees.length > 0)
                {
                    rc.shake(trees[0].getLocation());
                }

                // Scout Movement
                if (scoutState < 16 || (Math.random() < 0.05))
                {
                    enemyArchon = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));
                    tryMove(myLocation.directionTo(enemyArchon));
                    scoutState++;
                }
                else
                {
                    // Move randomly
                    tryMove(randomDirection());
                }

                // Signal for another scout to be created if this one is going to die
                if (rc.getHealth() < 6 && !sentGoingToDieSignal)
                {
                    sentGoingToDieSignal = true;
                    rc.broadcast(SCOUT_NUM_CHANNEL, rc.readBroadcast(SCOUT_NUM_CHANNEL) - 1);
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
        return (number % spacing) < spacing * fraction;
    }

    private static void initDirList()
    {
        for (int i = 0; i < 4; i++)
        {
            float radians = (float) (-Math.PI + 2 * Math.PI * ((float) i) / 4);
            dirList[i] = new Direction(radians);
        }
    }

    /**
     * Returns a random Direction
     *
     * @return a random Direction
     */
    private static Direction randomDirection()
    {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
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
        return tryMove(dir, 20, 3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir           The intended direction of movement
     * @param degreeOffset  Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    private static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException
    {

        // First, try intended direction
        if (rc.canMove(dir))
        {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while (currentCheck <= checksPerSide)
        {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck)))
            {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck)))
            {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
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
    private static boolean willCollideWithMe(BulletInfo bullet)
    {
        myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2)
        {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    private static void trySidestep(BulletInfo bullet) throws GameActionException
    {
        Direction towards = bullet.getDir();
        tryMove(towards.rotateRightDegrees(90));
    }

}

