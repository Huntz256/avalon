package playermaster;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;
	
	// These channels keep track of our archon's location
	private static final int ARCHON_CHANNEL_ONE = 0;
	private static final int ARCHON_CHANNEL_TWO = 1;
	
	// These channels keep track of the enemy archon's location
	private static final int ENEMY_ARCHON_X_CHANNEL = 3;
	private static final int ENEMY_ARCHON_Y_CHANNEL = 4;
	private static final int LOST_ENEMY_ARCHON_CHANNEL = 5;
	
	// These channels keep track of the amount of robots on our team
	private static final int ARCHON_NUM_CHANNEL = 6;
	private static final int GARDENER_NUM_CHANNEL = 7;
	private static final int SOLDIER_NUM_CHANNEL = 8;
	private static final int TANK_NUM_CHANNEL = 9;
	private static final int SCOUT_NUM_CHANNEL = 10;
	private static final int LUMBERJACK_NUM_CHANNEL = 11;
	
	private static final int ARCHON_SURROUNDED = 12;
	private static final int BUILD_GARDENER = 13;
	
	private static final int GAME_STATE_CHANNEL = 14;
	
	// The enemy archon's location
	private static MapLocation enemyArchonLocation;
	
	
	private static MapLocation[] initialEnemyArchonLocation;
	
    
	
	
	/**
	 * run() is the method that is called when a robot is instantiated in the Battlecode world.
	 * If this method returns, the robot dies!
	 **/
	@SuppressWarnings("unused")

	public static void run(RobotController rc) throws GameActionException {
		// This is the RobotController object. You use it to perform actions from this robot,
		// and to get information on its current status.
		RobotPlayer.rc = rc;

		initialEnemyArchonLocation = rc.getInitialArchonLocations(rc.getTeam().opponent());
		
		// Broadcast the initial enemy archon's location at the start of the game
		if (rc.readBroadcast(GAME_STATE_CHANNEL) == 0) {
            rc.broadcastFloat(ENEMY_ARCHON_X_CHANNEL, initialEnemyArchonLocation[0].x);
            rc.broadcastFloat(ENEMY_ARCHON_Y_CHANNEL, initialEnemyArchonLocation[0].y);
            rc.broadcast(GAME_STATE_CHANNEL, 1);
        }
		

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
			case TANK:
				runTank();
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
				
				
				Direction dir = randomDirection();
				while(!rc.canHireGardener(dir))
					dir = randomDirection();
				// Two Gardeners are built at the start of the game
				if(rc.readBroadcast(GARDENER_NUM_CHANNEL) < 2 && rc.isBuildReady()) {
					rc.hireGardener(dir);
					rc.broadcast(GARDENER_NUM_CHANNEL, rc.readBroadcast(GARDENER_NUM_CHANNEL) + 1);
				}
				
				
				Direction[] dirNearbyObjects = getDirectionOfNearbyObjects();
				
				// Get remaining space Archon can build around it and
				// the next direction it can build to
				int remainingSpace = 0;
				Direction dirBuild = null;
				float[] angles = new float[dirNearbyObjects.length];
				for(int i = 0; i < angles.length; i++) { // Get an array of angles between each nearby object
					if(i < angles.length - 1)
						angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[i + 1]);
					else {
						if(dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]) < 0)
							angles[i] = 360 + dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]);
						else
							angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]);
					}
					System.out.println("Archon angle: " + angles[i]);
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
				
				// Sense nearby bullets
				BulletInfo[] bullets = rc.senseNearbyBullets();
				
				// Randomly attempt to build a gardener in this direction
				// Build more gardeners if archon is in danger
				if(bullets.length > 0 && rc.canHireGardener(dirBuild))
					rc.hireGardener(dirBuild);
				else if(rc.canHireGardener(dirBuild) && Math.random() < .12)
					rc.hireGardener(dirBuild);
				
//				if(remainingSpace == 0)
//					rc.broadcast(ARCHON_SURROUNDED, rc.getID());
//				
//				if(rc.readBroadcast(BUILD_GARDENER) == 1 && rc.canHireGardener(dirBuild)) {
//					rc.hireGardener(dirBuild);
//					rc.broadcast(GARDENER_NUM_CHANNEL, rc.readBroadcast(GARDENER_NUM_CHANNEL) + 1);
//					rc.broadcast(BUILD_GARDENER, 0);
//				}
				
				// Move randomly
//				tryMove(randomDirection());
				
				
					
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
				
				
				// Determine if our team Archon is nearby
				RobotInfo[] myRobots = rc.senseNearbyRobots(2, rc.getTeam());
				RobotInfo archonNearby = null;
				boolean isArchonNearby = false;
				Direction toArchon = null;
				Direction awayFromArchon = null;
				for(RobotInfo robot : myRobots) {
					if(robot.getType() == RobotType.ARCHON) {
						isArchonNearby = true;
						archonNearby = robot;
						toArchon = rc.getLocation().directionTo(archonNearby.getLocation());
		                awayFromArchon = toArchon.rotateRightDegrees(180);
					}
				}
				
				if(isArchonNearby && rc.readBroadcast(SCOUT_NUM_CHANNEL) >= 2) {
					// Water our team lowest health tree
					TreeInfo[] myTrees = rc.senseNearbyTrees(2, rc.getTeam());
					if(myTrees.length > 0) {
						TreeInfo lowestHealthTree = myTrees[0];
						for(TreeInfo tree : myTrees) {
							if(tree.getHealth() < lowestHealthTree.getHealth())
								lowestHealthTree = tree;
						}
						
						if(rc.canWater(lowestHealthTree.getID()))
							rc.water(lowestHealthTree.getID());
					}
					
					Direction[] dirNearbyObjects = getDirectionOfNearbyObjects();
					
					// Get remaining space Gardener can build around it and
					// the next direction it can build to
					int remainingSpace = 0;
					Direction dirBuild = null;
					if(dirNearbyObjects.length == 1) { // Our Archon is the only nearby object
						remainingSpace = 4;
						dirBuild = dirNearbyObjects[0].rotateLeftDegrees(80);
					}
					else {
						float[] angles = new float[dirNearbyObjects.length];
						for(int i = 0; i < angles.length; i++) { // Get an array of angles between each nearby object
							if(i < angles.length - 1)
								angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[i + 1]);
							else {
								if(dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]) < 0)
									angles[i] = 360 + dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]);
								else
									angles[i] = dirNearbyObjects[i].degreesBetween(dirNearbyObjects[0]);
							}
							System.out.println("Garden angle: " + angles[i]);
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
					if(remainingSpace > 1) {
						rc.broadcast(BUILD_GARDENER, 0);
						if(rc.canPlantTree(dirBuild))
							rc.plantTree(dirBuild);
					}
					else if(remainingSpace == 1) {
						// Build a Lumberjack if we have less than two Lumberjacks
						if(rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < 2 && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
							rc.buildRobot(RobotType.LUMBERJACK, dir);
							rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
						}
						// Build a Lumberjack if we have less than bullets / 100 Lumberjacks
						else if (rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < rc.getTeamBullets() * 0.01 && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
							rc.buildRobot(RobotType.LUMBERJACK, dir);
							rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
						}
						// Randomly attempt to build a soldier or Lumberjack in this direction
						else if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .04) {
							rc.buildRobot(RobotType.SOLDIER, dir);
							rc.broadcast(SOLDIER_NUM_CHANNEL, rc.readBroadcast(SOLDIER_NUM_CHANNEL) + 1);
						}
						else if(rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .05) {
							rc.buildRobot(RobotType.LUMBERJACK, dir);
							rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
						}
						
						// Shake neutral trees if we can
						TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 1, Team.NEUTRAL);
						if(neutralTrees.length > 0 && Math.random() < 0.01) {
							int neutralTreeID = neutralTrees[0].getID();
							if (rc.canShake(neutralTreeID)) {
								rc.shake(neutralTreeID);
							}
						}
						// Randomly move such that they tend to move away from our Arch
						else if(Math.random() < 0.25) {
							tryMove(awayFromArchon);
						}
						else if (Math.random() < 0.8) {
							tryMove(randomDirection());
						}
						
//						rc.broadcast(BUILD_GARDENER, 1);
//						if(archonNearby.getID() == rc.readBroadcast(ARCHON_SURROUNDED)) {
//							if(rc.canMove(dirBuild))
//								rc.move(dirBuild);
//							else {
//								while(!rc.canMove(dir)) {
//									dir = randomDirection();
//								}
//								rc.move(dir);
//							}
//							rc.broadcast(ARCHON_SURROUNDED, 0);
//						}
//						
//						else {
//							// Build a Lumberjack if we have less than two Lumberjacks
//							if(rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < 2 && rc.canBuildRobot(RobotType.LUMBERJACK, dirBuild)) {
//								rc.buildRobot(RobotType.LUMBERJACK, dirBuild);
//								rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
//							}
//							// Build a Lumberjack if we have less than bullets / 100 Lumberjacks
//							else if (rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < rc.getTeamBullets() * 0.01 && rc.canBuildRobot(RobotType.LUMBERJACK, dirBuild)) {
//								rc.buildRobot(RobotType.LUMBERJACK, dirBuild);
//			                    rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
//							}
//							// Randomly attempt to build a soldier or Lumberjack in this direction
//							else if (rc.canBuildRobot(RobotType.SOLDIER, dirBuild) && Math.random() < .04) {
//								rc.buildRobot(RobotType.SOLDIER, dirBuild);
//								rc.broadcast(SOLDIER_NUM_CHANNEL, rc.readBroadcast(SOLDIER_NUM_CHANNEL) + 1);
//							}
//							else if(rc.canBuildRobot(RobotType.LUMBERJACK, dirBuild) && Math.random() < .05) {
//								rc.buildRobot(RobotType.LUMBERJACK, dirBuild);
//								rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
//							}
//						}
					}
				}
				else if(!isArchonNearby) {
					// Build a Lumberjack if we have less than two Lumberjacks
					if(rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < 2 && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
						rc.buildRobot(RobotType.LUMBERJACK, dir);
						rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
					}
					// Build a Lumberjack if we have less than bullets / 100 Lumberjacks
					else if (rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) < rc.getTeamBullets() * 0.01 && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
						rc.buildRobot(RobotType.LUMBERJACK, dir);
						rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
					}
					// Randomly attempt to build a soldier or Lumberjack in this direction
					else if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .04) {
						rc.buildRobot(RobotType.SOLDIER, dir);
						rc.broadcast(SOLDIER_NUM_CHANNEL, rc.readBroadcast(SOLDIER_NUM_CHANNEL) + 1);
					}
					else if(rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .05) {
						rc.buildRobot(RobotType.LUMBERJACK, dir);
						rc.broadcast(LUMBERJACK_NUM_CHANNEL, rc.readBroadcast(LUMBERJACK_NUM_CHANNEL) + 1);
					}
					
					// Shake neutral trees if we can
					TreeInfo[] neutralTrees = rc.senseNearbyTrees(RobotType.GARDENER.bodyRadius + 1, Team.NEUTRAL);
					if(neutralTrees.length > 0 && Math.random() < 0.01) {
						int neutralTreeID = neutralTrees[0].getID();
						if (rc.canShake(neutralTreeID)) {
							rc.shake(neutralTreeID);
						}
					}
					// Randomly move such that they tend to move away from our Arch
					else if(Math.random() < 0.25) {
						tryMove(awayFromArchon);
					}
					else if (Math.random() < 0.8) {
						tryMove(randomDirection());
					}
				}
				
				
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
				
				//Dodge bullets
				BulletInfo[] bullets = rc.senseNearbyBullets();
				
				for (BulletInfo bi : bullets) {
					if (willCollideWithMe(bi)) {
						if (!rc.hasMoved()) {
							trySidestep(bi);
						}
					}
				}
				
				
				// See if there are any nearby enemy robots
				RobotInfo[] robots = rc.senseNearbyRobots(7, enemy);
				
				// If there are some...
				if (robots.length > 0) {
					// And we have enough bullets, and haven't attacked yet this turn...
					if (rc.canFireSingleShot()) {
						// ...Then fire a bullet in the direction of the enemy.
						rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
						
						rc.setIndicatorLine(rc.getLocation(), robots[0].location, 0,0,255);
						}
				}
				
				if (!rc.hasMoved()) {
					if (Math.random() < 0.8) {
						MapLocation enemyArchon = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));
						
						// Move toward an enemy Archon
						tryMove(myLocation.directionTo(enemyArchon));
					}
					else {
						// Move randomly
						tryMove(randomDirection());
					}
				}
				
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			} catch (Exception e) {
				System.out.println("Soldier Exception");
				e.printStackTrace();
			}
		}
	}
	
	
	private static void runTank() throws GameActionException {
		System.out.println("I'm a tank!");
		
		// The code you want your robot to perform every round should be in this loop
		while (true) {
			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				MapLocation myLocation = rc.getLocation();
				
				// Move toward an enemy archon
				if (Math.random() < 0.9) {
					MapLocation enemyArchon = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));
					
					// Move toward an enemy archon
					tryMove(myLocation.directionTo(enemyArchon));
				}
				else {
					// Move randomly
					tryMove(randomDirection());
				}
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				
			}
			catch (Exception e) {
				System.out.println("Tank Exception");
				e.printStackTrace();
			}
		}
	}
	
	
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		TreeInfo nearestTree = null;
		while(true) {
			try {
				
				MapLocation myLocation = rc.getLocation();
				
				// Update enemy archon location
				updateEnemyArchonLocation();
				
				// See if there are any enemy robots within sight radius
				RobotInfo[] robots = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
				
				
				
				// If location of the enemy Archon is lost and a scout has
				// found an enemy Archon, broadcast its location to all robots
				if (rc.readBroadcast(LOST_ENEMY_ARCHON_CHANNEL) == 1) {
					boolean foundArchon = false;
					for(RobotInfo robot : robots) {
						if (robot.getType() == RobotType.ARCHON) {
							foundArchon = true;
							MapLocation enemyArchonLocation = robot.getLocation();
							rc.broadcast(ENEMY_ARCHON_X_CHANNEL, (int) enemyArchonLocation.x);
							rc.broadcast(ENEMY_ARCHON_Y_CHANNEL, (int) enemyArchonLocation.y);
							rc.setIndicatorDot(myLocation, 0, 0, 255);
						}
					}
					
					if (!foundArchon)
						rc.setIndicatorDot(myLocation, 255, 0, 0);
				}
				else {
					rc.setIndicatorDot(myLocation, 0, 255, 0);
				}
				
				
				// Shake nearby trees to gain bullets
				TreeInfo[] neutralTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
				for(int i = 0; i < neutralTrees.length; i++) {
					if(neutralTrees[i].getContainedBullets() > 0) {
						nearestTree = neutralTrees[i];
						break;
					}
					if(neutralTrees.length == i + 1)
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
				
				// Signal for another scout to be created if this one is going to die
				if (rc.getHealth() <= 5) {
					rc.broadcast(SCOUT_NUM_CHANNEL, rc.readBroadcast(SCOUT_NUM_CHANNEL) - 1);
					rc.disintegrate();
				}
				
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
				RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
				
				if(robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					rc.strike();
				}
				else {
					//Dodge bullets
					BulletInfo[] bullets = rc.senseNearbyBullets();
					if (bullets.length > 0) {
						for (BulletInfo bi : bullets) {
							if(willCollideWithMe(bi)) {
								if(!rc.hasMoved()) {
									trySidestep(bi);
								}
							}
						}
					}
					else if(!rc.hasMoved()) {
						// Search for robots within fraction of sight radius
						robots = rc.senseNearbyRobots(5, enemy);
						
						// If there is a robot, move towards it
						if(robots.length > 0) {
							MapLocation myLocation = rc.getLocation();
							MapLocation enemyLocation = robots[0].getLocation();
							Direction toEnemy = myLocation.directionTo(enemyLocation);
							
							if(tryMove(toEnemy)) {}
							else 
								while(!tryMove(randomDirection())) {}
						}
						else {
							// Move Randomly
							while(!tryMove(randomDirection()));
						}
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
	
	private static void trySidestep(BulletInfo bullet) throws GameActionException {
		Direction towards = bullet.getDir();
		tryMove(towards.rotateRightDegrees(90));
	}
	
	private static void updateEnemyArchonLocation() throws GameActionException {
		MapLocation prevArchonLocation = new MapLocation(rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL), rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL));
		RobotInfo[] robots = rc.senseNearbyRobots(10,rc.getTeam().opponent());
		
		// Updates the enemy Archon location stored in the team-shared array
		boolean foundEnemyArchon = false;
		enemyArchonLocation = prevArchonLocation;
		for (RobotInfo robot : robots) {
			if (robot.getType() == RobotType.ARCHON) {
				foundEnemyArchon = true;
				enemyArchonLocation = robot.getLocation();
				rc.broadcast(ENEMY_ARCHON_X_CHANNEL, (int) enemyArchonLocation.x);
				rc.broadcast(ENEMY_ARCHON_Y_CHANNEL, (int) enemyArchonLocation.y);
			}
		}
		
		// Update whether we have lost the enemy archon's location or not
		// This should only occur if the enemy archon is dead, in which case we need to find
		// the other enemy archon(s) if they exist
		if (!foundEnemyArchon) {
			rc.broadcast(LOST_ENEMY_ARCHON_CHANNEL, 1);
			// System.out.println("Lost!");
			// rc.setIndicatorDot(enemyArchonLocation, 255, 0, 0);
			rc.setIndicatorLine(rc.getLocation(), enemyArchonLocation, 255, 0, 0);
		}
		else {
			rc.broadcast(LOST_ENEMY_ARCHON_CHANNEL, 0);
			// rc.setIndicatorDot(enemyArchonLocation, 0, 255, 0);
			rc.setIndicatorLine(rc.getLocation(), enemyArchonLocation, 0, 255, 0);
		}
	}
	
	private static Direction[] getDirectionOfNearbyObjects() throws GameActionException {
		TreeInfo[] treesNearby = rc.senseNearbyTrees(3);
		RobotInfo[] robotsNearby = rc.senseNearbyRobots(3);
		
		// Take all objects nearby and put them in an array, dirNearbyObjects
		Direction[] dirNearbyObjects = new Direction[treesNearby.length + robotsNearby.length];
		int dirLength = 0;
		for(TreeInfo tree : treesNearby) {
			dirNearbyObjects[dirLength] = rc.getLocation().directionTo(tree.getLocation());
			dirLength++;
		}
		for(RobotInfo robot : robotsNearby) {
			dirNearbyObjects[dirLength] = rc.getLocation().directionTo(robot.getLocation());
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
			temp = dirNearbyObjects[i];
			dirNearbyObjects[i] = dirNearbyObjects[minAngleIndex];
			dirNearbyObjects[minAngleIndex] = temp;
		}
		return dirNearbyObjects;
	}
}