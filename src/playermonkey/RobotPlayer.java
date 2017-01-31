package playermonkey;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	
	private static final int ARCHON_NUM_CHANNEL = 0;
	private static final int GARDENER_NUM_CHANNEL = 1;
	private static final int SOLDIER_NUM_CHANNEL = 2;
	private static final int TANK_NUM_CHANNEL = 3;
	private static final int SCOUT_NUM_CHANNEL = 4;
	private static final int LUMBERJACK_NUM_CHANNEL = 5;
	
	
	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")

	public static void run(RobotController rc) throws GameActionException {
		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;


		// Here, we've separated the controls into a different method for each RobotType.
		// You can add the missing ones or rewrite this into your own control structure.
		switch (rc.getType()) {
			case ARCHON:
				runArchon();
				break;
			case GARDENER:
				runGardener();
				break;
			case SOLDIER:
				runSoldier();
				break;
			case SCOUT:
				runScout();
				break;
			case LUMBERJACK:
				runLumberjack();
				break;
		}
	}

	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");
		
		// The code you want your robot to perform every round should be in this loop
		while (true) {
			
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				if(rc.getRoundNum() == 0)
					rc.broadcast(ARCHON_NUM_CHANNEL, rc.getRobotCount());

				// Generate a random direction
				Direction dir = randomDirection();

				// Two Gardeners are built at the start of the game
				while(rc.isBuildReady() && rc.readBroadcast(GARDENER_NUM_CHANNEL) < 2) {
					if(rc.canHireGardener(dir)) {
						rc.hireGardener(dir);
						rc.broadcast(GARDENER_NUM_CHANNEL, rc.readBroadcast(GARDENER_NUM_CHANNEL) + 1);
					}
					else
						dir = randomDirection();
				}
					
//				while(rc.getBuildCooldownTurns() == 0 && rc.senseNearbyRobots(2, rc.getTeam()).length <= 4 /*&& rc.readBroadcast(GARDENER_NUM_CHANNEL) < (4 * rc.readBroadcast(ARCHON_NUM_CHANNEL))*/) {
//					if(rc.canHireGardener(dir))
//						rc.hireGardener(dir);
//				}

				// Move randomly
//				tryMove(randomDirection());


				// Broadcast archon's location for other robots on the team to know
//				MapLocation myLocation = rc.getLocation();
//				rc.broadcast(0,(int)myLocation.x);
//				rc.broadcast(1,(int)myLocation.y);

//				if(rc.getHealth() <= 2) {
//					rc.broadcast(ARCHON_NUM_CHANNEL, rc.readBroadcast(ARCHON_NUM_CHANNEL));
//					rc.disintegrate();
//				}
					
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
					
			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
			}
		}
	}

	static void runGardener() throws GameActionException {
		System.out.println("I'm a gardener!");
		TreeInfo nearestTree = null;
		// The code you want your robot to perform every round should be in this loop
		while(true) {
			
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				// Listen for home archon's location
//				int xPos = rc.readBroadcast(0);
//				int yPos = rc.readBroadcast(1);
//				MapLocation archonLoc = new MapLocation(xPos,yPos);

				// Generate a random direction
				Direction dir = randomDirection();
				
				// Build a Scout in a random direction
				if(rc.hasRobotBuildRequirements(RobotType.SCOUT) && rc.isBuildReady() && rc.readBroadcast(SCOUT_NUM_CHANNEL) < 2) {
					while(!rc.canBuildRobot(RobotType.SCOUT, dir)) dir = randomDirection();
					rc.buildRobot(RobotType.SCOUT, dir);
					rc.broadcast(SCOUT_NUM_CHANNEL, rc.readBroadcast(SCOUT_NUM_CHANNEL) + 1);
				}
				
				
//				if(!rc.isBuildReady() && rc.readBroadcast(SCOUT_NUM_CHANNEL) < 2) {
//					TreeInfo[] treesNeutral = rc.senseNearbyTrees(-1, Team.NEUTRAL);;
//					System.out.println(treesNeutral.length);
//					for(int i = 0; i < treesNeutral.length; i++) {
//						if(treesNeutral[i].getContainedBullets() > 0) {
//							nearestTree = treesNeutral[i];
//							break;
//						}
//						if(treesNeutral.length == i + 1)
//								nearestTree = null;
//					}
//					if(nearestTree != null) {
//						MapLocation nearestTreeLoc = nearestTree.getLocation();
//						if(rc.canInteractWithTree(nearestTreeLoc))
//							rc.shake(nearestTreeLoc);
//						else
//							rc.move(nearestTreeLoc);
//					}
//				}
				
				// Take all objects nearby and put them in an array, dirNearbyObjects
				TreeInfo[] treesRadius3 = rc.senseNearbyTrees(3);
				RobotInfo[] robotRadius3 = rc.senseNearbyRobots(3);
				Direction[] dirNearbyObjects = new Direction[treesRadius3.length + robotRadius3.length];
				int dirLength = 0;
				for(int i = 0; i < treesRadius3.length; i++) {
					dirNearbyObjects[dirLength] = rc.getLocation().directionTo(treesRadius3[i].getLocation());
					dirLength++;
				}
				for(int i = 0; i < robotRadius3.length; i++) {
					dirNearbyObjects[dirLength] = rc.getLocation().directionTo(robotRadius3[i].getLocation());
					dirLength++;
				}
				// Sort directionNearbyObjects
				Direction temp;
				for(int i = 0; i < dirNearbyObjects.length; i++) {
					int minAngleIndex = i;
					float minAngle = dirNearbyObjects[i].getAngleDegrees();
					for(int j = i + 1; j < dirNearbyObjects.length; j++) {
						float testAngle = dirNearbyObjects[j].getAngleDegrees();
						if(testAngle < minAngle) {
							minAngle = testAngle;
							minAngleIndex = j;
						}
					}
					System.out.println("min: " + minAngle);
					temp = dirNearbyObjects[i];
					dirNearbyObjects[i] = dirNearbyObjects[minAngleIndex];
					dirNearbyObjects[minAngleIndex] = temp;
				}
				
				// Get an array of angles between each nearby object
				int remainingSpace = 0;
				Direction dirBuild = null;
				if(dirNearbyObjects.length == 0) {
					remainingSpace = 6;
					dirBuild = Direction.EAST;
				}
				else if(dirNearbyObjects.length == 1) {
					remainingSpace = 5;
					dirBuild = dirNearbyObjects[0].rotateLeftDegrees(60);
				}
					
				else {
					float[] angles = new float[dirNearbyObjects.length];
					for(int i = 0; i < angles.length; i++) {
						if(i < angles.length - 1)
							angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[i + 1]);
						else
							angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]);
					}
					for(int i = 0; i < angles.length; i++) {
						int spaces = (int)((angles[i] - 60)/60);
						remainingSpace += spaces;
					}
					for(int i = 0; i < angles.length; i++) {
						if(angles[i] >= 120) {
							dirBuild = dirNearbyObjects[i].rotateLeftDegrees(60);
							break;
						}
					}
				}
				
				// Build trees around Gardener leaving one space open
				if(rc.isBuildReady() && /*rc.readBroadcast(SCOUT_NUM_CHANNEL) >= 2 &&*/ remainingSpace >= 1) {
					System.out.println("hi");
					if(rc.canPlantTree(dirBuild))
						rc.plantTree(dirBuild);
				}
				
				if(rc.isBuildReady()) {
					if(rc.canBuildRobot(RobotType.SOLDIER, dir)) {
						rc.buildRobot(RobotType.SOLDIER, dir);
						rc.broadcast(SOLDIER_NUM_CHANNEL, rc.readBroadcast(SOLDIER_NUM_CHANNEL) + 1);
					}
					else
						dir = randomDirection();
				}
				
				// Randomly attempt to build a soldier or lumberjack in this direction
//				if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
//					rc.buildRobot(RobotType.SOLDIER, dir);
//				} else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
//					rc.buildRobot(RobotType.LUMBERJACK, dir);
//				}
				
				// Move randomly
//				tryMove(randomDirection()); 
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}
	
	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				MapLocation myLocation = rc.getLocation();
				
				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
				
				// If there are some...
				if (robots.length > 0) {
					// And we have enough bullets, and haven't attacked yet this turn...
					if (rc.canFireSingleShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
					}
				}
				
				// Move randomly
				tryMove(randomDirection());
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}

	
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		TreeInfo nearestTree = null;
		while(true) {
			try {
				TreeInfo[] treesNeutral = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				for(int i = 0; i < treesNeutral.length; i++) {
					if(treesNeutral[i].getContainedBullets() > 0) {
						nearestTree = treesNeutral[i];
						break;
					}
					if(treesNeutral.length == i + 1)
						nearestTree = null;
				}
				
				if(nearestTree != null) {
					MapLocation nearestTreeLoc = nearestTree.getLocation();
					if(rc.canInteractWithTree(nearestTreeLoc))
						rc.shake(nearestTreeLoc);
					else
						tryMove(rc.getLocation().directionTo(nearestTreeLoc), 60, 3);
					}
	    
				// Move randomly
//				tryMove(randomDirection());
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			} catch(Exception e) {
				System.out.println("Scout Exception");
				e.printStackTrace();
			}
		}
	}

	static void runLumberjack() throws GameActionException {
		System.out.println("I'm a lumberjack!");
		Team enemy = rc.getTeam().opponent();

		// The code you want your robot to perform every round should be in this loop
		while (true) {
			
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				
				// See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
				
				if(robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					rc.strike();
				}
				else {
					// No close robots, so search for robots within sight radius
					robots = rc.senseNearbyRobots(-1,enemy);
					
					// If there is a robot, move towards it
					if(robots.length > 0) {
						MapLocation myLocation = rc.getLocation();
						MapLocation enemyLocation = robots[0].getLocation();
						Direction toEnemy = myLocation.directionTo(enemyLocation);
						
						tryMove(toEnemy);
					} else {
						// Move Randomly
						tryMove(randomDirection());
					}
				}
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			} catch (Exception e) {
				System.out.println("Lumberjack Exception");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns a random Direction
	 * @return a random Direction
	 */
	static Direction randomDirection() {
		return new Direction((float)Math.random() * 2 * (float)Math.PI);
	}
	
	/**
	 * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
	 *
	 * @param dir The intended direction of movement
	 * @return true if a move was performed
	 * @throws GameActionException
	 */
	static boolean tryMove(Direction dir) throws GameActionException {
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
	static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
		
		// First, try intended direction
		if (rc.canMove(dir)) {
			rc.move(dir);
			return true;
		}

		// Now try a bunch of similar angles
		boolean moved = false;
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
	static boolean willCollideWithMe(BulletInfo bullet) {
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