import java.util.Scanner;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Arrays;
import java.util.Random;


public class Game{
    // The direction that the snake is moving in
    public enum Direction {
        UP, DOWN, LEFT, RIGHT, NO_DIRECTION
    }

    private Deque<int[]> snakeCells;         // A double sides queue containing all the co-ordinates of the cells containing part of the snake
    private char[][] gameGrid;               // A grid that we use to display the current position of all of the game elements to the player
    private Random random = new Random();    // A random number generator, used to generate the position of the cherry
    private int[] cherryPosition;            // An array containing the x and y co-ordinates of the cherry
    private boolean justAteCherry;           // Set to true if the snake eats the cherry. If true, grow the snake by 1 cell


    private int colsInGrid;           
    private int rowsInGrid;

    private volatile Direction currentSnakeDirection; // Marked volatile so changes are visible across threads

    // Constructor to initalise all variables
    public Game(){
        // The snakes body is stored in a list of co-ords of snake cells
        // The head is always the first
        // Initalise head to be at the 5th column of the 5th row
        snakeCells = new ArrayDeque<>();
        snakeCells.addFirst(new int[]{5,5});


        rowsInGrid = 25;
        colsInGrid = 50;
        gameGrid = new char[rowsInGrid][colsInGrid];
        currentSnakeDirection = Direction.NO_DIRECTION;

        
        // Initial position of the cherry
        cherryPosition = new int[]{10, 10};
        
        // Boolean to determine if we shrink from the tail or not when we next move
        justAteCherry = false;


        // Fill gameGrid with blank space
        for (int i = 0; i < rowsInGrid; i++){
            for (int j = 0; j < colsInGrid; j++){
                gameGrid[i][j] = '-';
            }
        }
    }

    // For testing
    public void printDiagnosticInfo(){;
        int[] currentHead = snakeCells.peekFirst();
        System.out.println("Current snake direction is: " + currentSnakeDirection);
        System.out.println("Head position is: [" + currentHead[0] + ", " + currentHead[1] + "]");
    }
    

    // If snakes head is at the same position as cherry,
    // eat the cherry, and allow a new cherry to respawn
    public void handleCollisionWithCherry(){
        int[] currentHead = snakeCells.peekFirst();

        if (Arrays.equals(cherryPosition, currentHead)){
            justAteCherry = true;
            cherryPosition = new int[]{random.nextInt(25), random.nextInt(50)};
        }
    }

    // If the snakes head is at the same position as one of the
    // cells of its body, then the game is over
    public void handleCollisionBetweenSnakesBodyAndHead(){
        int[] currentHead = snakeCells.peekFirst();        for (int[] cell : snakeCells){
            // "If the head is in the same place as a snake cell, and that snake cell is not the same memory location as the head ... "
            if (Arrays.equals(cell, currentHead) && cell != currentHead){
                System.out.println("GAME OVER");    // "... then print game over and exit the program"
                System.exit(0);
            }
        }
    }

    // Delay for a certain period between iterations of the game loop to control the speed of the snake
    public void delayGameloop(){
        // We delay for less time if we are moving horizontally
        // This is a workaround for the fact that we have twice as many cols
        // as rows, to ensure a square shaped game grid. We halve the frame rate
        // so that it appears the snake is going the same speed no matter the direction
        // We also factor in the size of the snake so that we move faster as the game progresses
        try {
            if (currentSnakeDirection == Direction.LEFT || currentSnakeDirection == Direction.RIGHT){
                Thread.sleep(Math.max(30, 75 - 5*snakeCells.size()));
            }else{
                Thread.sleep(Math.max(60, 150 - 10*snakeCells.size()));
            }
            
        } catch (InterruptedException e) {
            // Do nothing
        }

    }

    // Grows the head in current movement direction, and removes the tail (unless the cherry has just been eaten!!)
    public void handleSnakeMovement(){
        int[] currentHead = snakeCells.peekFirst();

        // Determine the offset of the next head postion compared to current position using the current direction
        int[] offSet;
        switch (currentSnakeDirection){
            default : offSet = new int[]{0,0};
            case UP : offSet = new int[]{-1, 0}; break;
            case DOWN : offSet = new int[]{1, 0}; break;
            case LEFT : offSet = new int[]{0, -1}; break;
            case RIGHT : offSet = new int[]{0, 1}; break;
            case NO_DIRECTION : offSet = new int[]{0,0}; break;
        }

        // Apply this offset to determine the new head position
        int[] newHeadPosition = {currentHead[0]+offSet[0], currentHead[1]+offSet[1]};

        // If the snake has gone out bounds, have him pop out the other side
        if (newHeadPosition[0] >= rowsInGrid){
            newHeadPosition[0] = 0;
        }
        else if (newHeadPosition[0] < 0){
            newHeadPosition[0] = rowsInGrid - 1;
        }
        else if (newHeadPosition[1] >= colsInGrid){
            newHeadPosition[1] = 0;
        }
        else if (newHeadPosition[1] < 0){
            newHeadPosition[1] = colsInGrid - 1;
        }

        // Add the new head to snakeCells
        snakeCells.addFirst(newHeadPosition); // The head is always the "first" element in the double sided queue snakeCells


        // We only shrink the tail if the snake has not just eaten a cherry
        // This has the effect of growing the snake by 1 every time it eats a cherry
        if (!justAteCherry){
            snakeCells.removeLast(); // The tail is always the "last" element in the double sided queue snakeCells
        }

        justAteCherry = false; // Reset after we move

    }

    // Prints the gameGrid (that represents the current game state) to the console
    public void printTheScreen(){
        // To begin with, make all grid cells blank
        for (int i = 0; i < rowsInGrid; i++){
            for (int j = 0; j < colsInGrid; j++){
                gameGrid[i][j] = '-';
            }
        }   

        // Mark all cells that have part of the snake in them
        for (int[] snakeCell : snakeCells ){
            gameGrid[snakeCell[0]][snakeCell[1]] = '1';
        }

        // Mark the position of the cherry
        gameGrid[cherryPosition[0]][cherryPosition[1]] = '2';

        // Make a big gap between this frame and the current frame
        System.out.println("\n".repeat(30));

        // Print the game grid
        for (int i = 0; i < rowsInGrid; i++){
            for (int j = 0; j < colsInGrid; j++){
                System.out.print(gameGrid[i][j]);
            }
            System.out.println(); // Go on to the next row
        }   
    }

    public void startInputThread(){
        Thread userInputThread = new Thread(this::handleUserInput);
        userInputThread.start();
    }

    // Take in user input and set the snake direction variable accordingly
    public void handleUserInput(){
        Scanner userInputScanner = new Scanner(System.in);
    
        while (true){
            try {
                if (userInputScanner.hasNextLine()){
                    String input = userInputScanner.nextLine();
    
                    switch (input.trim().toLowerCase()){
                        case "w" : currentSnakeDirection = Direction.UP; break;
                        case "a" : currentSnakeDirection = Direction.LEFT; break;
                        case "s" : currentSnakeDirection = Direction.DOWN; break;
                        case "d" : currentSnakeDirection = Direction.RIGHT; break;
                    }
                }
            } catch (Exception e) {
            }
        }
    }
    

    public static void main(String args[]){
        Game game = new Game();

        // Start the thread that handles user button presses in the CLI
        game.startInputThread();
 

        // The main game loop
        while(true){
            
            // Game logic
            game.handleCollisionWithCherry();
            game.handleSnakeMovement();
            game.handleCollisionBetweenSnakesBodyAndHead();

            // Print the screen based on the current game state
            game.printTheScreen();

            // Delay between iterations to control the speed of the snake
            game.delayGameloop();
        }
    }
}