package game.controllers.examples;

import game.controllers.GhostController;
import game.core.Game.DM;
import game.core.Game;
import game.core.GameView;
import java.awt.Color;
import game.core.G;
import java.util.Arrays;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getActions() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., game.entries.ghosts.mypackage).
 */

public class MyGhosts implements GhostController
{
	// Public Constant variables
	public static final int PINKY_TILE_AHEAD = 5;	// Number of tiles ahead of pacman used that is targeted by Pinky
	public static final int INKY_TILE_AHEAD = 2;	// Number of tiles ahead of pacman used to calculate vector
	public static final int TIME_FACTOR = 25;       // Time conversion from leveltime to Secs 1000/40 = 25

	// Instance variables:
	private Mode[] ghostMode;    					// Stores the current Ghost Mode
	private int[] targetNode;						// Stores the target Node of each ghost for debugging purposes
	private boolean[] reverse;        				// Used to determine whether the ghosts should reverse direction at their next junction 
	private int[] actions;            				// Stores the action to be returned
	private int[] frightenedTime;					// Stores the frightened time for each ghost over all pills eaten by pacman
	private boolean Debugging = false;  			// Used to draw the Lines from Ghosts to their respective target Tiles
	private int cornerNodes[][];					// Used to store the Scatter Target Nodes for each level and Ghost
	private int cruiseElroy[];						// Used to store the number of Pills for each level when blinky switches to Cruise Elroy
	private int timings[][];						// Stores the timings used for scatter-chase transitions
	private int currentLevel;						// Maintains a copy of the currentlevel to reset frightened tiem when level changes

	// Defines an Enum to Hold the current Mode of the Ghosts
	public enum Mode
	{
		CHASE, SCATTER, FRIGHTENED, NUM_OF_MODES
	}

	// Constructor
	public MyGhosts(boolean debugging)
	{
		// Set debugging Mode
		Debugging = debugging;

		// Create array to hold the Modes of the Ghosts
		// Every Ghost initially begins in scatter Mode
		ghostMode = new Mode[Game.NUM_GHOSTS];
		for (int i = 0; i < ghostMode.length; i++)
			ghostMode[i] = Mode.SCATTER;

		// This array will hold the reverse flag
		// Initally, this flag is set to false for all ghosts
		reverse = new boolean[Game.NUM_GHOSTS];
		for(int i = 0; i < reverse.length; i++)
			reverse[i] = false;

		// For storing the ghost target Tiles
		//Initialise it pacman's Positon
		targetNode = new int[Game.NUM_GHOSTS];
		for(int i = 0; i < targetNode.length; i++)
			targetNode[i] = 0;

		// Create array to hold the actions of the Ghosts and 
		// Initialise it with Left, Since all ghosts turn Left when they come out of the lair 
		actions = new int[Game.NUM_GHOSTS];
		for(int i = 0; i < actions.length; i++)
			actions[i] = Game.LEFT;

		// Create and array to hold the frightened time for each ghost
		// Initialise all the times to zero
		frightenedTime = new int[Game.NUM_GHOSTS];
		for(int i = 0; i < frightenedTime.length; i++)
			frightenedTime[i] = 0;

		// Set the Scatter target Nodes
		cornerNodes = new int[Game.NUM_MAZES][Game.NUM_GHOSTS];
		cornerNodes[0][0] = 78;
		cornerNodes[0][1] = 0;
		cornerNodes[0][2] = 1191;
		cornerNodes[0][3] = 1291;
		cornerNodes[1][0] = 220;
		cornerNodes[1][1] = 131;
		cornerNodes[1][2] = 1217;
		cornerNodes[1][3] = 1317;
		cornerNodes[2][0] = 78;
		cornerNodes[2][1] = 0;
		cornerNodes[2][2] = 1370;
		cornerNodes[2][3] = 1378;
		cornerNodes[3][0] = 100;
		cornerNodes[3][1] = 0;
		cornerNodes[3][2] = 1207;
		cornerNodes[3][3] = 1296;

		//System.out.println(actions.length);

		// Set Cruise Elroy Pill Numbers
		// Values from: http://www.webpacman.com/ghosts.html#red
		cruiseElroy = new int[10];
		cruiseElroy[0] = 20;
		cruiseElroy[1] = 30;
		cruiseElroy[2] = 40;
		cruiseElroy[3] = 40;
		cruiseElroy[4] = 40;
		cruiseElroy[5] = 50;
		cruiseElroy[6] = 50;
		cruiseElroy[7] = 50;
		cruiseElroy[8] = 60;
		cruiseElroy[9] = 60;

		// Initialise Timings according to game-internals document
		timings = new int[3][7];
		timings[0][0] = 7;
		timings[0][1] = 27;
		timings[0][2] = 34;
		timings[0][3] = 54;
		timings[0][4] = 59;
		timings[0][5] = 79;
		timings[0][6] = 84;
		timings[1][0] = 7;
		timings[1][1] = 27;
		timings[1][2] = 34;
		timings[1][3] = 54;
		timings[1][4] = 59;
		timings[1][5] = 1092;
		timings[1][6] = 1093;
		timings[2][0] = 5;
		timings[2][1] = 25;
		timings[2][2] = 30;
		timings[2][3] = 50;
		timings[2][4] = 55;
		timings[2][5] = 1092;
		timings[2][6] = 1093;

		// Set CurrLevel to zero
		currentLevel = 0;
	}

	// Place your game logic here to play the game as the ghosts
	public int[] getActions(Game game,long timeDue)
	{
		// TODOs:
		
		// 1. Lair reverse		
		// IF STILL IN LAIR AND REVERSE[I] = TRUE, TO LAIR REVERSE
		
		// 2. Priority while selecting direction
		// the inbuilt funtion only calculates the distance....does not maintain any order if two or more distances turn out to be equal.
		// Most likely solution. Edit the private method that selected the next move depending upon the current and target tile and type of distance.
		
		// 3. Four intersections at which cannot turn up
		// solution: 1. check if one of the nodes.
		//  		 2. if one of the nodes, use getpossibleDirs and set the upper direction to -1.
		
		// 4. Blinky Speed Increases at 2 times everyLevel
		// In each Level, at two defined points based on the number of dotd remaining, blinky's speed increases by 5%
		// as lvl increase, blinky's speed increases earlier and earlier
		
		// 5. higher Levels, CRUISE ELROY MODE, scatter or chase, target tile - curpacmanloc
		// but direction reversal still occurs.
		// if pcman dies, while blinky is in cruise elroy mode, blinky reverts back to normal modes..
		// i.e in scatte mode its target tile is top right one and not pacman's current tile
		// when everyone is out of the lair, blinky again reverts back to cruise elroy mode

		// 6. Duration of Frightened Mode is reduced as the no. of lvl increases. Completely removed from lvl 19

		// 7. SCATTER MODE: Find the four corners of each Maze

		// 8. FRIGHTENED MODE Timer:
		// Frigtened Mode: timer is paused. length of scatter and chase mode does not increase
		// end of frightened mode timer resumes
		
		// 9. Ghosts always move left after coming out of Ghost house.
		// Except when lair reversal occurs

		// 10. Timings for Different Levels.

		// 11. Reverse Ghost direction when Mode changes

		// Call the decideGhostModes function to set the Modes of all the Ghosts
		decideGhostModes(game);

		// If required, get the action for each Ghost.
		// If not required, return previous action.
		for(int i = 0; i < Game.NUM_GHOSTS; i++)
		{
			// If ghost is in Lair and reverse flag is true, reverse action to be taken.
			if(game.getLairTime(i) > 0 && reverse[i])
			{
				actions[i] = game.getReverse(actions[i]);
				reverse[i] = false;
			}
			else
				if(game.ghostRequiresAction(i))
				{
					// SCATTER MODE
					if(ghostMode[i] == Mode.SCATTER)
					{
						//if we write a function that takes i and returns aprropriate scatter corrner node
						actions[i] = game.getNextGhostDir(i, cornerNodes[game.getCurMaze()][i], true, Game.DM.PATH);
						targetNode[i] = cornerNodes[game.getCurMaze()][i];
						
						// TODO CRUISE ELROY MODE
						int level = (game.getCurLevel() < cruiseElroy.length)? game.getCurLevel(): cruiseElroy.length - 1;
						if(i == 0 && game.getNumActivePills() < cruiseElroy[level])
						{
							actions[i] = game.getNextGhostDir(i, game.getCurPacManLoc(), true, Game.DM.PATH);
							targetNode[i] = game.getCurPacManLoc();
						}			
					}

					//CHASE MODE
					if(ghostMode[i] == Mode.CHASE)
					{
						if(i == 0)
						{
							// BLINKY's CHASE MODE
							// Move in the direction that will move it closer to Pacman's Current Location
							actions[i] = game.getNextGhostDir(i, game.getCurPacManLoc(), true, Game.DM.PATH);
							targetNode[i] = game.getCurPacManLoc();
						}
						else
							if(i == 1)
							{
								// PINKY's CHASE MODE
								// Get pacman's current location and direction
								int pacLocation = game.getCurPacManLoc();
								int pacDirection = game.getCurPacManDir();

								// Get Pacman's X and Y using its Node Index
								int pacX = game.getX(pacLocation);
								int pacY = game.getY(pacLocation);

								//System.out.println("PacmanLocation:" + pacLocation);
								//System.out.println("PacX: " + pacX + " , PacY: " + pacY);

								// Move 4 Steps in Pacman's Direction
								if(pacDirection == Game.UP)
								{
									pacY -= PINKY_TILE_AHEAD;
									//pacX -= PINKY_TILE_AHEAD;
								}
								else
									if(pacDirection == Game.LEFT)
									{
										pacX -= PINKY_TILE_AHEAD;
									}
									else
										if(pacDirection == Game.DOWN)
										{
											pacY += PINKY_TILE_AHEAD;
										}
										else
											pacX += PINKY_TILE_AHEAD;

								// get NodeIndex using X,Y
								int location = game.getNodeUsingXY(pacX, pacY);

								//System.out.println("After 4 Tiles:\nPacX: " + pacX + " , PacY: " + pacY);		
								//System.out.println("Final Target: " + location);		

								// Select action that will take PINKY closer to the target location
								actions[i] = game.getNextGhostDir(i, location, true, Game.DM.PATH);
								targetNode[i] = location;
							}
							else
								if(i == 2)
								{
									// CLYDE's CHASE MODE
									if(game.getManhattenDistance(game.getCurGhostLoc(i), game.getCurPacManLoc()) > 32)
									{
										actions[i] = game.getNextGhostDir(i, game.getCurPacManLoc(), true, Game.DM.PATH);
										targetNode[i] = game.getCurPacManLoc();
									}			
									else
									{
										// TODO:Scatter Mode for each Level
										// Like SCATTER MODE
										actions[i] = game.getNextGhostDir(i, cornerNodes[game.getCurMaze()][i], true, Game.DM.PATH);
										targetNode[i] = cornerNodes[game.getCurMaze()][i];
									}
								}
								else
									if(i == 3)
									{
										// INKY's CHASE MODE
										// Get pacman's current location and direction
										int pacLocation = game.getCurPacManLoc();
										int pacDirection = game.getCurPacManDir();

										// Get Pacman's X and Y using its Node Index
										int pacX = game.getX(pacLocation);
										int pacY = game.getY(pacLocation);

										// Get blinky's current location and direction
										int blinkyLocation = game.getCurGhostLoc(0);

										// Get blinky's X and Y using its Node Index
										int blinkyX = game.getX(blinkyLocation);
										int blinkyY = game.getY(blinkyLocation);

										// Get the Node 2 tiles in front of pacman
										if(pacDirection == Game.UP)
										{
											pacY -= INKY_TILE_AHEAD;
											//pacX -= INKY_TILE_AHEAD;
										}
										else
											if(pacDirection == Game.LEFT)
											{
												pacX -= INKY_TILE_AHEAD;
											}
											else
												if(pacDirection == Game.DOWN)
												{
													pacY += INKY_TILE_AHEAD;
												}
												else
													pacX += INKY_TILE_AHEAD;

										// Get the vector
										int x = 2 * pacX - blinkyX;
										int y = 2 * pacY - blinkyY;

										// get NodeIndex using X,Y
										int location = game.getNodeUsingXY(x, y);

										// Select action that will take PINKY closer to the target location
										actions[i] = game.getNextGhostDir(i, location, true, Game.DM.PATH);
										targetNode[i] = location;
									}
					}

				}
			
			// Set the color for drawing
			if (Debugging) 
			{
				Color color = Color.GRAY;
				if (i == 0) 
					color = Color.RED;
				else if (i == 1) 
					color = Color.PINK;
				else if (i == 2) 
					color = Color.ORANGE;
				else 
					color = Color.BLUE;
				GameView.addLines(game, color, game.getCurGhostLoc(i), targetNode[i]);
				//System.out.println(game.getCurPacManLoc());
			}

		}// end of for
		
		//printDebugInfo();
		System.out.println("TimeDue: " + timeDue);

		return actions;
	}// end of getAction()


	public void decideGhostModes(Game game)
	{
		//setGhostModes(Mode.CHASE);	
		//setGhostModes(Mode.SCATTER);

		//Check if Level has changed
		if(currentLevel != game.getCurLevel())
		{
			// Level has changed , set frightened time to Zero
			for(int j = 0; j < frightenedTime.length; j++)
				frightenedTime[j] = 0;

			// Change the Level Number
			currentLevel = game.getCurLevel();
		}

		// TODO for different levels
		for(int i = 0; i < Game.NUM_GHOSTS; i++)
		{
 
			// If the ghosts are Edible their mode is set to frightened and we return
			if(game.isEdible(i))
			{
				// If the ghosts are edible, set mode to frightened
				ghostMode[i] = Mode.FRIGHTENED;

				// Also set the corresponding reverse flag to false
				reverse[i] = false;

				// Increment Frightened Time if Ghost is Edible
				frightenedTime[i]++;

				System.out.println("Mode Changed to: " + ghostMode[i] + ", Time:" + (game.getLevelTime() - frightenedTime[i]));
			}
			else
				{
					// Get each ghost's time by subtracting the frightenedtime of that ghost from the total Leveltime
					int time = (game.getLevelTime() - frightenedTime[i]) ;//* TIME_FACTOR;

					// Get the current level- to be used for scatter-chase switch
					int level = game.getCurLevel();
					
					// if level is either 0 or 1, do nothing
					// if level is either 2 or 3, set level = 1, since timings are same for 1, 2, 3 
					if(level == 2 || level == 3)
						level = 1;

					// if level is greater than 4, then set level = 2 since timings for higher lvls are stored at index 2 
					if(level >= 4)
						level = 2;

					// If ghost is not frightened, decide between Scatter and Chase Mode
					if(time < timings[level][0] * TIME_FACTOR)
					{
						ghostMode[i] = Mode.SCATTER;
						System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
					}
						
					else
						if(time < timings[level][1] * TIME_FACTOR)
						{
							// If the previous Mode is Scatter, set reverse flag to true
							if(ghostMode[i] == Mode.SCATTER)
								reverse[i] = true;

							// Set the new mode to chase
							ghostMode[i] = Mode.CHASE;

							System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
						}
						else
							if(time < timings[level][2] * TIME_FACTOR)
							{
								// If the previous Mode is CHASE, set reverse flag to true
								if(ghostMode[i] == Mode.CHASE)
									reverse[i] = true;

								// Set the new mode to chase
								ghostMode[i] = Mode.SCATTER;

								System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
							}								
							else
								if(time < timings[level][3] * TIME_FACTOR)
								{
									// If the previous Mode is Scatter, set reverse flag to true
									if(ghostMode[i] == Mode.SCATTER)
										reverse[i] = true;

									// Set the new mode to chase
									ghostMode[i] = Mode.CHASE;

									System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
								}
								else
									if(time < timings[level][4] * TIME_FACTOR)
									{
										// If the previous Mode is CHASE, set reverse flag to true
										if(ghostMode[i] == Mode.CHASE)	
											reverse[i] = true;

										// Set the new mode to chase
										ghostMode[i] = Mode.SCATTER;

										System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
									}
									else
										if(time < timings[level][5] * TIME_FACTOR)
										{
											// If the previous Mode is Scatter, set reverse flag to true
											if(ghostMode[i] == Mode.SCATTER)
												reverse[i] = true;

											// Set the new mode to chase
											ghostMode[i] = Mode.CHASE;

											System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
										}
										else
											if(time < timings[level][6] * TIME_FACTOR)
											{
												// If the previous Mode is CHASE, set reverse flag to true
												if(ghostMode[i] == Mode.CHASE)
													reverse[i] = true;

												// Set the new mode to chase
												ghostMode[i] = Mode.SCATTER;
												
												System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
											}
											else
												{
													if(ghostMode[i] == Mode.SCATTER)
													{
														// If the previous Mode is Scatter, set reverse flag to true
														reverse[i] = true;
													}

													// Set the new mode to chase
													ghostMode[i] = Mode.CHASE;

													System.out.println("Mode Change to: " + ghostMode[i] + ", Time: " + time);
												}
												
				}
		}// end of for

		System.out.println("\n");
	}//end of function
	// Used to set all ghosts modes to the same mode
	public void setGhostModes(Mode mode)
	{
		// Set all the ghosts to the same Mode
		for (int i = 0; i < ghostMode.length; i++)
			ghostMode[i] = mode;
	}

	public void printDebugInfo()
	{

		System.out.println("----------------------------------------------");
		
		System.out.println("Ghost Modes:");
		for(int i = 0; i < ghostMode.length; i++)
			System.out.print(ghostMode[i] + " ,");
		System.out.println("");


		System.out.println("TargetNodes:");
		for(int i = 0; i < targetNode.length; i++)
			System.out.print(targetNode[i] + " ,");
		System.out.println("");
		

		System.out.println("reverse:");
		for(int i = 0; i < reverse.length; i++)
			System.out.print(reverse[i] + " ,");
		System.out.println("");
		
		
		System.out.println("Actions:");
		for(int i = 0; i < actions.length; i++)
			System.out.print(actions[i] + " ,");
		System.out.println("");
		
		System.out.println("----------------------------------------------");		
	}
}