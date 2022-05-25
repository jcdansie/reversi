import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.text.PlainDocument;

import java.math.*;
import java.text.*;

class RandomGuy {

    public Socket s;
    public BufferedReader sin;
    public PrintWriter sout;
    Random generator = new Random();

    double t1, t2;
    int me;
    int boardState;
    int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;

    int validMoves[] = new int[64];
    int numValidMoves;


    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public RandomGuy(int _me, String host) {
        me = _me;
        initClient(host);

        int myMove;

        while (true) {
            System.out.println("Read");
            readMessage();

            if (turn == me) {
                System.out.println("Move");
                getValidMoves(round, state);

                myMove = move();
                getValidMoves(round, state);

                String sel = validMoves[myMove] / 8 + "\n" + validMoves[myMove] % 8;

                System.out.println("Selection: " + validMoves[myMove] / 8 + ", " + validMoves[myMove] % 8);

                sout.println(sel);
            }
        }
        //while (turn == me) {
        //    System.out.println("My turn");

        //readMessage();
        //}
    }

    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int move() {
        int depthToExplore = 5;
        int move = exploreState(state, depthToExplore, 10000, true);
        return move;
    }

    private int exploreState(int[][] givenState, int depthToExplore, int previousMax, boolean initialCall){
        int initialState[][] = copyState(givenState);
        // base case
        if(depthToExplore == 0){
            return evaluateState(givenState);
        }

        getValidMoves(round, givenState);
        int numValidMovesThisState = numValidMoves;
        int validMovesThisState[] = validMoves.clone();
        int moveValues[] = new int[numValidMovesThisState];
        int maxSoFar = -10000;
        // for each possible move
        for( int i = 0; i < numValidMovesThisState; i++ ){
            // make copy of state
            int copyState[][] = copyState(givenState);

            //prune
            if(i > 0){
                if(moveValues[i-1] > maxSoFar){
                    maxSoFar = moveValues[i-1];
                }
            }
            if(maxSoFar > previousMax){
                return -1*maxSoFar;
            }

            // make move
            givenState = makeMoveOnState(givenState, validMovesThisState[i]);

            // evaluate value of move using heuristic function
            switchPlayers();
            moveValues[i] = exploreState(givenState, depthToExplore-1, -1*maxSoFar, false);
            switchPlayers();

            // revert state to original
            givenState = copyState;
        }

        state = initialState;

        // get max of moves
        int maxMove = 0;
        int maxMoveValue = 0;
        for (int i = 0; i < moveValues.length; i++){
            if( moveValues[i] > maxMoveValue) {
                maxMove = i;
                maxMoveValue = moveValues[i];
            }
        }

        if(initialCall){
            return maxMove;
        }else {
            return -1*maxMoveValue;
        }
    }

    private void switchPlayers(){
        if(me == 1){
            me = 2;
        }else {
            me = 1;
        }
    }

    //TODO: Beginning play in center 16
    // mid game claim corners, A6,A3,C8,C1,G8,G1,H6,H3 and center 16
    // corners and spots connected to corners we control

    //make more conditional: want to play majority in the center until we can take a corner 2 spots from corner also good
    // once we have the corner play in a way that builds out from that area. Still take corners when possible
    private int evaluateState(int givenState[][]) {
        int stateValue = 0;

        //if we control a corner
        if (givenState[0][0] == me || givenState[0][7] == me || givenState[7][0] == me || givenState[7][7] == me) {
            //Award points for each corner we control and subtract if they control
            if (givenState[0][0] == me) {
                stateValue += 50;
            } else if (givenState[0][0] != 0) {
                stateValue -= 50;
            }
            if (givenState[0][7] == me) {
                stateValue += 50;
            } else if (givenState[0][7] != 0) {
                stateValue -= 50;
            }
            if (givenState[7][0] == me) {
                stateValue += 50;
            } else if (givenState[7][0] != 0) {
                stateValue -= 50;
            }
            if (givenState[7][7] == me) {
                stateValue += 50;
            } else if (givenState[7][7] != 0) {
                stateValue -= 50;
            }
            // play connecting from our corner
            for(int i = 0; i < givenState.length; i++){
                for(int j = 0; j < givenState[i].length; j++){
                    boolean buildCorner = buildsFromCorner(givenState,i,j);
                    if(givenState[i][j] == me && buildCorner){
                        stateValue += 10;
                    }else if (givenState[i][j] == me && !buildCorner) {
                        stateValue += 5;
                    }
                    else if(givenState[i][j] != 0){
                        stateValue -= 7;
                    }
                }
            }
            // center of board(inner 16) when can't grow from corner
            for(int i = 2; i < givenState.length - 2; i++){
                for(int j = 2; j < givenState[i].length - 2; j++){
                    if(givenState[i][j] == me){
                        stateValue += 5;
                    }else if(givenState[i][j] != 0){
                        stateValue -= 5;
                    }
                }
            }
        }

        //if We don't control any corners award more points for
        // play in center of board
        //away from spots touching corner
        // spots 2 spaces away from corner good
        // want to claim corner
        else {
            //subtract for each corner they control
            if (givenState[0][0] != 0) {
                stateValue -= 50;
            }
            if (givenState[0][7] != 0) {
                stateValue -= 50;
            }
            if (givenState[7][0] != 0) {
                stateValue -= 50;
            }
            if (givenState[7][7] != 0) {
                stateValue -= 50;
            }
            //avoid spots directly connected to corner and force opponent to play in those spaces if they don't have the corner
            if (givenState[0][1] == me) {
                stateValue -= 3;
            }else if (givenState[0][1] != 0 && givenState[0][0] == 0) {
                stateValue += 3;
            }
            if (givenState[1][0] == me) {
                stateValue -=3;
            } else if (givenState[1][0] != 0 && givenState[0][0] == 0) {
                stateValue += 3;
            }
            if (givenState[1][1] == me) {
                stateValue -= 25;
            } else if (givenState[1][1] != 0 && givenState[0][0] == 0) {
                stateValue += 20;
            }
            if (givenState[0][6] == me) {
                stateValue -= 3;
            }else if (givenState[0][6] != 0 && givenState[0][7] == 0) {
                stateValue += 3;
            }
            if (givenState[1][7] == me) {
                stateValue -= 3;
            } else if ( givenState[1][7] != 0 && givenState[0][7] == 0) {
                stateValue += 3;
            }
            if (givenState[1][6] == me ) {
                stateValue -= 25;
            } else if (givenState[1][6] != 0 && givenState[0][7] == 0) {
                stateValue += 20;
            }
            if (givenState[7][6] == me) {
                stateValue -= 3;
            }else if (givenState[7][6] != 0 && givenState[7][7] == 0) {
                stateValue += 3;
            }
            if (givenState[6][7] == me) {
                stateValue -= 3;
            } else if (givenState[6][7] != 0 && givenState[7][7] == 0) {
                stateValue += 3;
            }
            if (givenState[6][6] == me) {
                stateValue -= 25;
            } else if (givenState[6][6] != 0 && givenState[7][7] == 0) {
                stateValue += 20;
            }
            if(givenState[6][0] == me) {
                stateValue -= 3;
            } else if (givenState[6][0] != 0 && givenState[7][0] == 0) {
                stateValue += 3;
            }
            if (givenState[7][1] == me) {
                stateValue -= 3;
            } else if (givenState[7][1] != 0 && givenState[7][0] == 0) {
                stateValue += 3;
            }
            if (givenState[6][1] == me) {
                stateValue -= 25;
            } else if (givenState[6][1] != 0 && givenState[7][0] == 0) {
                stateValue += 20;
            }
            //Want spots 2 spaces from corner
            if (givenState[0][2] == me ) {
                stateValue += 6;
            } else if (givenState[0][2] != 0) {
                stateValue -= 6;
            }
            if (givenState[2][0] == me) {
                stateValue += 6;
            } else if (givenState[0][2] != 0) {
                stateValue -=6;
            }
            if (givenState[0][5] == me) {
                stateValue += 6;
            } else if (givenState[0][5] != 0) {
                stateValue -=6;
            }
            if (givenState[2][7] == me) {
                stateValue += 6;
            } else if (givenState[2][7] != 0) {
                stateValue -= 6;
            }
            if (givenState[5][7] == me) {
                stateValue += 6;
            } else if (givenState[5][7] != 0) {
                stateValue -= 6;
            }
            if (givenState[7][5] == me) {
                stateValue += 6;
            } else if (givenState[7][5] != 0) {
                stateValue -= 6;
            }
            if (givenState[7][2] == me) {
                stateValue += 6;
            } else if (givenState[7][2] != 0) {
                stateValue -= 6;
            }
            if (givenState[5][0] == me) {
                stateValue += 6;
            } else if (givenState[5][0] != 0) {
                stateValue -= 6;
            }
            // center of board(inner 16)
            for(int i = 2; i < givenState.length - 2; i++){
                for(int j = 2; j < givenState[i].length - 2; j++){
                    if(givenState[i][j] == me){
                        stateValue += 7;
                    }else if(givenState[i][j] != 0){
                        stateValue -= 7;
                    }
                }
            }
        }
        return stateValue;
    }

    private boolean buildsFromCorner(int[][] givenState, int i, int j) { //i and j are index of the piece we are looking at
        // check each direction if it connects to our piece
        //[-1][-1] , [-1][0], [-1][+1], [0][+1], [+1][+1], [+1][0], [+1][-1], [0][-1]
        if (i != 0 && i != 7 && j != 0 && j != 7) { // not touching a wall
            boolean upLDag = buildsFromCornerUpLDag(givenState, i-1, j-1);
            boolean up =buildsFromCornerUp(givenState, i, j-1);
            boolean upRDag = buildsFromCornerUpRDag(givenState, i+1, j-1);
            boolean right = buildsFromCornerRight(givenState, i+1, j);
            boolean downRDag = buildsFromCornerDownRDag(givenState, i+1, j+1);
            boolean down = buildsFromCornerDown(givenState, i, j+1);
            boolean downLDag = buildsFromCornerDownLDag(givenState, i-1, j+1);
            boolean left = buildsFromCornerLeft(givenState, i-1, j);
            if ( upLDag || up || upRDag || right || downRDag || down || downLDag || left) { // builds from at least one corner
                return true;
            } else {
                return false;
            }
        }
        // if corner return true
        if (i == 0 && j == 0 || i == 0 && j == 7 || i == 7 && j == 0 || i == 7 && j == 7) {
            if (givenState[i][j] == me) {
                return true;
            } else {
                return false;
            }
        }
        // if top or bottom wall continue to corner
        else if (j == 0 || j == 7) {
            //go right
            boolean right = rightWall(givenState, i, j -1);
            // go left
            boolean left = leftWall(givenState, i, j+1);
            return right || left;
        }
        else if (i == 0 || i == 7) {
            //go up
            boolean up = upWall(givenState,i -1, j);
            //go down
            boolean down = downWall(givenState, i+1, j);
            return up || down;
        }
        // if opposing piece return false
        return false;
    }
    private boolean leftWall(int[][] givenState, int i, int j) { // go left along top and bottom walls
        if ( i < 0 || i > 7 || j < 0 || j > 7) {
            return false;
        }
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 0 && j == 0 || i == 0 && j == 7) {
            if (givenState[i][j] == me) {
                return true;
            } else {
                return false;
            }
        }
        return leftWall(givenState,i-1, j);
    }
    private boolean rightWall(int[][] givenState, int i, int j) { // go right along top and bottom walls
        if ( i < 0 || i > 7 || j < 0 || j > 7) {
            return false;
        }
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 7 && j == 0 || i == 7 && j == 7) {
            if (givenState[i][j] == me) {
                return true;
            } else {
                return false;
            }
        }
        return rightWall(givenState,i+1, j);
    }
    private boolean upWall(int[][] givenState, int i, int j) { // go up along left and right wall
        if ( i < 0 || i > 7 || j < 0 || j > 7) {
            return false;
        }
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 0 && j == 0 || i == 7 && j == 0) {
            if (givenState[i][j] == me) {
                return true;
            } else {
                return false;
            }
        }

        return upWall(givenState,i, j-1);
    }
    private boolean downWall(int[][] givenState, int i, int j) { // go down along left and right wall
        if ( i < 0 || i > 7 || j < 0 || j > 7) {
            return false;
        }
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 0 && j == 7 || i == 7 && j == 7) {
            if (givenState[i][j] == me) {
                return true;
            } else {
                return false;
            }
        }
        return downWall(givenState,i, j + 1);
    }

    private boolean buildsFromCornerLeft(int[][] givenState, int i, int j) { // goes from current point directly left
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 0) {
            //go up
            boolean up = upWall(givenState,i, j -1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else {
            return buildsFromCornerLeft(givenState, i -1, j);
        }
    }

    private boolean buildsFromCornerDownLDag(int[][] givenState, int i, int j) { // goes diagonally down left
        if (givenState[i][j] != me) {
            return false;
        }
        if(i == 0 && j == 7) { //We are at a corner
            if(givenState[i][j] == me) {
                return true;
            }
            return false;
        }
        if (i == 0) {
            //go up
            boolean up = upWall(givenState,i, j -1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else if (j == 7) {
            //go right
            boolean right = rightWall(givenState, i+ 1, j);
            // go left
            boolean left = leftWall(givenState, i -1, j);
            return right || left;
        } else  {
            return buildsFromCornerDownLDag(givenState, i-1, j+1);
        }
    }

    private boolean buildsFromCornerDown(int[][] givenState, int i, int j) { // goes straight down
        if (givenState[i][j] != me) {
            return false;
        }
        if (j == 7) {
            //go right
            boolean right = rightWall(givenState, i+1, j);
            // go left
            boolean left = leftWall(givenState, i-1, j);
            return right || left;
        } else {
            return buildsFromCornerDown(givenState, i, j+1);
        }
    }

    private boolean buildsFromCornerDownRDag(int[][] givenState, int i, int j) { // goes diagonally down right
        if (givenState[i][j] != me) {
            return false;
        }
        if(i == 7 && j == 7) { //We are at a corner
            if(givenState[i][j] == me) {
                return true;
            }
            return false;
        }
        if (i == 7) {
            //go up
            boolean up = upWall(givenState,i, j -1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else if (j == 7) {
            //go right
            boolean right = rightWall(givenState, i + 1, j);
            // go left
            boolean left = leftWall(givenState, i -1, j);
            return right || left;
        } else  {
            return buildsFromCornerDownRDag(givenState, i+1, j+1);
        }
    }

    private boolean buildsFromCornerRight(int[][] givenState, int i, int j) { // goes straight right
        if (givenState[i][j] != me) {
            return false;
        }
        if (i == 7) {
            //go up
            boolean up = upWall(givenState,i, j - 1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else {
            return buildsFromCornerRight(givenState, i + 1, j);
        }
    }

    private boolean buildsFromCornerUpRDag(int[][] givenState, int i, int j) { // goes diagonally up right
        if (givenState[i][j] != me) {
            return false;
        }
        if(i == 7 && j == 0) { //We are at a corner
            if(givenState[i][j] == me) {
                return true;
            }
            return false;
        }
        if (i == 7) {
            //go up
            boolean up = upWall(givenState,i , j -1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else if (j == 0) {
            //go right
            boolean right = rightWall(givenState, i + 1, j);
            // go left
            boolean left = leftWall(givenState, i - 1, j);
            return right || left;
        } else  {
            return buildsFromCornerUpRDag(givenState, i+1, j-1);
        }
    }

    private boolean buildsFromCornerUp(int[][] givenState, int i, int j) { // goes straight up
        if (givenState[i][j] != me) {
            return false;
        }
        if (j == 0) {
            //go right
            boolean right = rightWall(givenState, i + 1, j);
            // go left
            boolean left = leftWall(givenState, i -1, j);
            return right || left;
        } else {
            return buildsFromCornerUp(givenState, i, j -1);
        }
    }

    private boolean buildsFromCornerUpLDag(int[][] givenState, int i, int j) { //goes diagonally up left
        if (givenState[i][j] != me) {
            return false;
        }
        if(i == 0 && j == 0) { //We are at a corner
            if(givenState[i][j] == me) {
                return true;
            }
            return false;
        }
        if (i == 0) {
            //go up
            boolean up = upWall(givenState,i, j - 1);
            //go down
            boolean down = downWall(givenState, i, j + 1);
            return up || down;
        } else if (j == 0) {
            //go right
            boolean right = rightWall(givenState, i + 1, j);
            // go left
            boolean left = leftWall(givenState, i -1, j);
            return right || left;
        } else  {
            return buildsFromCornerUpLDag(givenState, i-1, j-1);
        }
    }

    private int[][] copyState(int state[][]) {
        int newState[][] = new int[8][8];

        for(int j = 0; j < newState.length; j++){
            for(int k = 0; k < newState[j].length; k++){
                newState[j][k] = state[j][k];
            }
        }
        return newState;
    }

    private int[][] makeMoveOnState(int state[][], int move){
        state[move/8][move%8] = me;
        changeColors(state, move/8, move%8, me);
        return state;
    }

    public static void changeColors(int givenState[][], int row, int col, int turn) {
        int incx, incy;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;
                checkDirectionExt(givenState, row, col, incx, incy, turn);
            }
        }
    }

    public static void checkDirectionExt(int state[][], int row, int col, int incx, int incy, int turn) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;
        
        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;
        
            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;
        
            sequence[seqLen] = state[r][c];
            seqLen++;
        }
        
        int count = 0;
        for (i = 0; i < seqLen; i++) {
            if (turn == 0) {
                if (sequence[i] == 2)
                    count ++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        count = 20;
                    break;
                }
            }
            else {
                if (sequence[i] == 1)
                    count ++;
                else {
                    if ((sequence[i] == 2) && (count > 0))
                        count = 20;
                    break;
                }
            }
        }
        
        if (count > 10) {
            if (turn == 0) {
                i = 1;
                r = row+incy*i;
                c = col+incx*i;
                while (state[r][c] == 2) {
                    state[r][c] = 1;
                    i++;
                    r = row+incy*i;
                    c = col+incx*i;
                }
            }
            else {
                i = 1;
                r = row+incy*i;
                c = col+incx*i;
                while (state[r][c] == 1) {
                    state[r][c] = 2;
                    i++;
                    r = row+incy*i;
                    c = col+incx*i;
                }
            }
        }
    }

    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private void getValidMoves(int round, int state[][]) {
        int i, j;

        numValidMoves = 0;
        if (round < 4) {
            if (state[3][3] == 0) {
                validMoves[numValidMoves] = 3*8 + 3;
                numValidMoves ++;
            }
            if (state[3][4] == 0) {
                validMoves[numValidMoves] = 3*8 + 4;
                numValidMoves ++;
            }
            if (state[4][3] == 0) {
                validMoves[numValidMoves] = 4*8 + 3;
                numValidMoves ++;
            }
            if (state[4][4] == 0) {
                validMoves[numValidMoves] = 4*8 + 4;
                numValidMoves ++;
            }
            //System.out.println("Valid Moves:");
            for (i = 0; i < numValidMoves; i++) {
                //System.out.println(validMoves[i] / 8 + ", " + validMoves[i] % 8);
            }
        }
        else {
            //System.out.println("Valid Moves:");
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(state, i, j)) {
                            validMoves[numValidMoves] = i*8 + j;
                            numValidMoves ++;
                            //System.out.println(i + ", " + j);
                        }
                    }
                }
            }
        }


        //if (round > 3) {
        //    System.out.println("checking out");
        //    System.exit(1);
        //}
    }

    private boolean checkDirection(int state[][], int row, int col, int incx, int incy) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;

        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;

            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;

            sequence[seqLen] = state[r][c];
            seqLen++;
        }

        int count = 0;
        for (i = 0; i < seqLen; i++) {
            if (me == 1) {
                if (sequence[i] == 2)
                    count ++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        return true;
                    break;
                }
            }
            else {
                if (sequence[i] == 1)
                    count ++;
                else {
                    if ((sequence[i] == 2) && (count > 0))
                        return true;
                    break;
                }
            }
        }

        return false;
    }

    private boolean couldBe(int state[][], int row, int col) {
        int incx, incy;

        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;

                if (checkDirection(state, row, col, incx, incy))
                    return true;
            }
        }

        return false;
    }

    public void readMessage() {
        int i, j;
        String status;
        try {
            //System.out.println("Ready to read again");
            turn = Integer.parseInt(sin.readLine());

            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }

                System.exit(1);
            }

            //System.out.println("Turn: " + turn);
            round = Integer.parseInt(sin.readLine());
            t1 = Double.parseDouble(sin.readLine());
            System.out.println(t1);
            t2 = Double.parseDouble(sin.readLine());
            System.out.println(t2);
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }

        System.out.println("Turn: " + turn);
        System.out.println("Round: " + round);
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                System.out.print(state[i][j]);
            }
            System.out.println();
        }
        System.out.println();
    }

    public void initClient(String host) {
        int portNumber = 3333+me;

        try {
            s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
            sin = new BufferedReader(new InputStreamReader(s.getInputStream()));

            String info = sin.readLine();
            System.out.println(info);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }


    // compile on your machine: javac *.java
    // call: java RandomGuy [ipaddress] [player_number]
    //   ipaddress is the ipaddress on the computer the server was launched on.  Enter "localhost" if it is on the same computer
    //   player_number is 1 (for the black player) and 2 (for the white player)
    public static void main(String args[]) {
        new RandomGuy(Integer.parseInt(args[1]), args[0]);
    }

}
