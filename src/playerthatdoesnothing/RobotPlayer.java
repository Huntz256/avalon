package playerthatdoesnothing;
import battlecode.common.*;

/**
 * Created by Hunter on 1/13/2017.
 *
 * THIS PLAYER DOES ABSOLUTELY NOTHING. DO NOT USE.
 * THIS PLAYER DOES ABSOLUTELY NOTHING. DO NOT USE.
 * THIS PLAYER DOES ABSOLUTELY NOTHING. DO NOT USE.
 * THIS PLAYER DOES ABSOLUTELY NOTHING. DO NOT USE.
 */
public strictfp class RobotPlayer
{
    private static RobotController rc;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException
    {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
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
                runLumberjack();
                break;
        }
    }

    private static void runArchon() throws GameActionException
    {
        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // Do nothing

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

    private static void runGardener() throws GameActionException
    {
        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                //Do nothing

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

    private static void runSoldier() throws GameActionException
    {
        System.out.println("I'm an soldier!");

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // Do nothing

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

    private static void runLumberjack() throws GameActionException
    {
        System.out.println("I'm a lumberjack!");

        // The code you want your robot to perform every round should be in this loop
        while (true)
        {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try
            {
                // Do nothing

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
}
