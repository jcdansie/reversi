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
        //IMPORTANT!! depth must be a multiple of 2
        int depthToExplore = 5;
        int move = exploreState(state, depthToExplore, true);
        System.out.println("Selected Move: " + move);
        return move;
    }

    private int exploreState(int[][] givenState, int depthToExplore, boolean initialCall){
        int initialState[][] = copyState(givenState);
        // base case
        if(depthToExplore == 0){
            return evaluateState(givenState);
        }

        getValidMoves(round, givenState);
        int numValidMovesThisState = numValidMoves;
        int validMovesThisState[] = validMoves.clone();
        int moveValues[] = new int[numValidMovesThisState];
        // for each possible move
        for( int i = 0; i < numValidMovesThisState; i++ ){
        
            // make copy of state
            int copyState[][] = copyState(givenState);
            
            // make move
            givenState = makeMoveOnState(givenState, validMovesThisState[i]);
            
            // evaluate value of move using heuristic function
            switchPlayers();
            moveValues[i] = exploreState(givenState, depthToExplore-1, false);
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
                stateValue += 15;
            } else if (givenState[0][0] != 0) {
                stateValue -= 15;
            }
            if (givenState[0][7] == me) {
                stateValue += 15;
            } else if (givenState[0][7] != 0) {
                stateValue -= 15;
            }
            if (givenState[7][0] == me) {
                stateValue += 15;
            } else if (givenState[7][0] != 0) {
                stateValue -= 15;
            }
            if (givenState[7][7] == me) {
                stateValue += 15;
            } else if (givenState[7][7] != 0) {
                stateValue -= 15;
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
                stateValue -= 15;
            }
            if (givenState[0][7] != 0) {
                stateValue -= 15;
            }
            if (givenState[7][0] != 0) {
                stateValue -= 15;
            }
            if (givenState[7][7] != 0) {
                stateValue -= 15;
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
                stateValue -= 6;
            } else if (givenState[1][1] != 0 && givenState[0][0] == 0) {
                stateValue += 7;
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
                stateValue -= 6;
            } else if (givenState[1][6] != 0 && givenState[0][7] == 0) {
                stateValue += 7;
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
                stateValue -= 6;
            } else if (givenState[6][6] != 0 && givenState[7][7] == 0) {
                stateValue += 7;
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
                stateValue -= 6;
            } else if (givenState[6][1] != 0 && givenState[7][0] == 0) {
                stateValue += 7;
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
        // recursively follow that path to either wall, corner, or opposing piece
        // if corner return true
        // if wall continue to corner
        // if opposing piece return false
        return false;
    }
//    private int evaluateState(int givenState[][]) {
//        int stateValue = 0;
//
//        // add 1 for your square, subtract 1 for their square
//        for(int i = 0; i < givenState.length; i++){
//            for(int j = 0; j < givenState[i].length; j++){
//                if(givenState[i][j] == me){
//                    stateValue += 1;
//                }else if(givenState[i][j] != 0){
//                    stateValue -= 1;
//                }
//            }
//        }
//
//        // add 3 for your square on edge not counting directly next to corner, subtract 3 for their square on edge
//        for(int i = 1; i < givenState[0].length - 1; i++){
//            if(givenState[0][i] == me) {
//                stateValue += 3;
//            } else if(givenState[0][i] != 0){
//                stateValue -= 3;
//            }
//        }
//        int maxIndex = givenState.length-1;
//        for(int i = 1; i < givenState[maxIndex].length -1; i++){
//            if(givenState[maxIndex][i] == me) {
//                stateValue += 3;
//            } else if(givenState[maxIndex][i] != 0){
//                stateValue -= 3;
//            }
//        }
//        for(int i = 1; i < givenState.length -1; i++){
//            if(givenState[i][0] == me) {
//                stateValue += 3;
//            } else if(givenState[i][0] != 0){
//                stateValue -= 3;
//            }
//        }
//        for(int i = 1; i < givenState.length -1; i++){
//            if(givenState[i][maxIndex] == me) {
//                stateValue += 3;
//            } else if(givenState[i][maxIndex] != 0){
//                stateValue -= 3;
//            }
//        }
//
//        //want spots 2 away from corner
//        if (givenState[0][2] == me) {
//            stateValue += 3;
//        } else if (givenState[0][2] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[2][0] == me) {
//            stateValue +=3;
//        } else if (givenState[2][0] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[maxIndex][2] == me) {
//            stateValue += 3;
//        } else if (givenState[maxIndex][2] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[2][maxIndex] == me) {
//            stateValue +=3;
//        } else if (givenState[2][maxIndex] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[0][maxIndex-2] == me) {
//            stateValue += 3;
//        } else if (givenState[0][maxIndex -2] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[maxIndex-2][0] == me) {
//            stateValue +=3;
//        } else if (givenState[maxIndex-2][0] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[maxIndex-2][maxIndex] == me) {
//            stateValue +=3;
//        } else if (givenState[maxIndex-2][maxIndex] != 0) {
//            stateValue -= 5;
//        }
//        if (givenState[maxIndex][maxIndex-2] == me) {
//            stateValue +=3;
//        } else if (givenState[maxIndex][maxIndex-2] != 0) {
//            stateValue -= 5;
//        }
//
//        //don't want diagonal unless we control corner
//        if (givenState[0][0] != 0 && givenState[1][1] == me) {
//            stateValue -= 100;
//        } else if (givenState[0][0] == me && givenState[1][1] == me) {
//            stateValue += 10;
//        }
//        if (givenState[maxIndex][0] != 0 && givenState[maxIndex -1][1] == me) {
//            stateValue -= 100;
//        } else if (givenState[maxIndex][0] == me && givenState[maxIndex-1][1] == me) {
//            stateValue += 10;
//        }
//        if (givenState[0][maxIndex] != 0 && givenState[1][maxIndex-1] == me) {
//            stateValue -= 100;
//        } else if (givenState[0][maxIndex] == me && givenState[1][maxIndex-1] == me) {
//            stateValue += 10;
//        }
//        if (givenState[maxIndex][maxIndex] != 0 && givenState[maxIndex-1][maxIndex-1] == me) {
//            stateValue -= 100;
//        } else if (givenState[maxIndex][maxIndex] == me && givenState[maxIndex-1][maxIndex-1] == me) {
//            stateValue += 10;
//        }
//
//
//        // add 15 for your square in corner, subtract 15 for their square in corner
//        if(givenState[0][0] == me) {
//            stateValue += 20;
//        } else if(givenState[0][0] != 0){
//            stateValue -= 25;
//        }
//        if(givenState[0][maxIndex] == me) {
//            stateValue += 20;
//        } else if(givenState[0][maxIndex] != 0){
//            stateValue -= 25;
//        }
//        if(givenState[maxIndex][0] == me) {
//            stateValue += 20;
//        } else if(givenState[maxIndex][0] != 0){
//            stateValue -= 25;
//        }
//        if(givenState[maxIndex][maxIndex] == me) {
//            stateValue += 20;
//        } else if(givenState[maxIndex][maxIndex] != 0){
//            stateValue -= 25;
//        }
//
//        return stateValue;
//    }

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
        changeColors(state, move/8, move%8);
        return state;
    }

    public void changeColors(int givenState[][], int row, int col) {
        int incx, incy;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;
            
                checkDirectionExt(givenState, row, col, incx, incy, me);
            }
        }
    }

    public void checkDirectionExt(int state[][], int row, int col, int incx, int incy, int turn) {
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
