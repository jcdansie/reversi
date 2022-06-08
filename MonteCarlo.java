import java.util.*;

import javax.naming.spi.DirStateFactory.Result;
import javax.swing.RootPaneContainer;

import java.lang.*;
import java.io.*;
import java.net.*;
import java.lang.Math;

class MonteCarlo {

    public Socket s;
    public BufferedReader sin;
    public PrintWriter sout;
    Random generator = new Random();


    double t1, t2;
    int me;
    public int origMe;
    int boardState;
    int opponent;
    int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;

    int validMoves[] = new int[64];
    int numValidMoves;

    SearchTree root;


    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public MonteCarlo(int _me, String host) {
        me = _me;
        origMe = _me;
        if (origMe == 1) {
            opponent = 2;
        } else {
            opponent = 1;
        }
        initClient(host);

        int myMove;

        while (true) {
            System.out.println("Read");
            readMessage();

            if (turn == me) {
                System.out.println("Move");
                getValidMoves(round, state);

                myMove = move();
                me = origMe;
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


    private int move() {
        //initial state
        root = new SearchTree(copyState(state), me);
        getValidMoves(round, root.state);
        root.numValidMoves = numValidMoves;
        root.validMoves = validMoves.clone();
        root.children = new Vector<>();
        for( int i = 0; i < numValidMoves; i++ ) {
            SearchTree child = new SearchTree(makeMoveOnState(copyState(state), validMoves[i]), opponent);
            child.parent = root;
            root.children.add(child);
        }

        long endTime = System.currentTimeMillis() + 4000;
        while (System.currentTimeMillis() < endTime) {
            SearchTree leaf = traverse(root);

            int result = rollout(leaf);
    
            backPropagate(leaf, result);

            me = origMe;
        }

        for(int i = 0; i < root.children.size(); i++){
            System.out.println("Visits: " + root.children.get(i).visits);
            System.out.println("Wins: " + root.children.get(i).wins);
        }


        return bestChild(root.children);
    }

    private SearchTree traverse(SearchTree tree) {
        double constant = 100;
        double maxValue = -10000;
        int maxChild = 0;
        if(tree.children.size() <= 0){
            return tree;
        }
        for( int i = 0; i < tree.children.size(); i++ ){
            SearchTree child = tree.children.get(i);
            double value;
            if(child.visits == 0){
                value = 100000;
            } else {
                value = (double)child.wins / (double)child.visits + constant * (Math.sqrt( Math.log( (double) tree.visits) / (double) child.visits ) );
            }

            if( value > maxValue ) {
                maxChild = i;
                maxValue = value;
            }
        }

        SearchTree chosenChild = tree.children.get(maxChild);

        if( chosenChild.visits == 0 ) {
            return chosenChild;
        } else {
            switchPlayers();
            expandLeaf(chosenChild);
            return traverse(chosenChild);
        }

    }

    private void expandLeaf(SearchTree tree) {
        tree.children = new Vector<>();
        getValidMoves(round+1, tree.state);
        tree.numValidMoves = numValidMoves;
        tree.validMoves = validMoves.clone();

        for( int i = 0; i < tree.numValidMoves; i++ ) {
            SearchTree child = new SearchTree(makeMoveOnState(copyState(tree.state), tree.validMoves[i]), me);
            child.parent = tree;
            tree.children.add(child);
        }
    }

    private int rollout(SearchTree tree) {
        int[][] copyState = copyState(tree.state);
        while ( !isTerminalState(copyState) ) {
            switchPlayers();
            getValidMoves(round, copyState);

            if(numValidMoves == 0) {
                continue;
            }

            copyState = rollout_policy(copyState);
        }
        //return calcWinner(copyState);
        return calcScore(copyState);
    }

    private void backPropagate(SearchTree state, int win) {
        if (state.parent == null) {
            state.setWins(win);
            state.setVisits();
            return;
        }
        state.setWins(win);
        state.setVisits();
        backPropagate(state.parent, win);
    }

    private int[][] rollout_policy(int[][] state) {
        int[][] newState = copyState(state);
        // select one randomly
        Random rand = new Random();
        int myMove = validMoves[ rand.nextInt(numValidMoves) ];
        // assign newState as state + the random move
        return makeMoveOnState(newState, myMove);
    }

    private boolean isTerminalState(int[][] state) {
        getValidMoves(round, state);
        int moveCount = numValidMoves;
        if (moveCount <= 0) {
            switchPlayers();
            getValidMoves(round, state);
            moveCount = numValidMoves;
            switchPlayers();
            if (moveCount <= 0) {
                return true;
            }
        }
        return false;
    }
    

    private int bestChild(Vector<SearchTree> moves) {
        //return child with the most visits
        int move = 0;
        double percentage = moves.get(0).wins / (double)moves.get(0).visits;
        if (moves.get(0).visits == 0) {
            percentage = 0;
        }
        for (int i = 1; i < moves.size(); i++) {
            if (moves.get(i).wins / (double)moves.get(i).visits >= percentage) {
                //move = moves.get(i).move;
                move = i;
                percentage = moves.get(i).wins / (double)moves.get(i).visits;
            }
        }
        return move;
    }

    private void switchPlayers(){
        if(me == 1){
            me = 2;
        }else {
            me = 1;
        }
    }

    private int calcScore(int[][] state) {
        int myCount = 0;
        int opponentCount = 0;
        for(int j = 0; j < state.length; j++){
            for(int k = 0; k < state[j].length; k++){
                if (state[j][k] == origMe) {
                    myCount++;
                } else if (state[j][k] == opponent) {
                    opponentCount++;
                }
            }
        }
        if (myCount > opponentCount) {
            return myCount-opponentCount + 150;
        } else {
            return myCount-opponentCount;
        }
    }

    private int calcWinner(int[][] state) {
        int myCount = 0;
        int opponentCount = 0;
        for(int j = 0; j < state.length; j++){
            for(int k = 0; k < state[j].length; k++){
                if (state[j][k] == origMe) {
                    myCount++;
                } else if (state[j][k] == opponent) {
                    opponentCount++;
                }
            }
        }
        if (myCount > opponentCount) {
            return 1;
        } else {
            return 0;
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

    private void printState(int state[][]) {
        for(int j = 0; j < state.length; j++){
            for(int k = 0; k < state[j].length; k++){
                System.out.print(state[j][k]);
            }
            System.out.println();
        }
    }

    private int[][] makeMoveOnState(int[][] state, int move){
        state[move/8][move%8] = me;
        changeColors(state, move/8, move%8, me);
        return state;
    }

    public static void changeColors(int[][] givenState, int row, int col, int turn) {
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
            if (turn == 1) {
                if (sequence[i] == 2)
                    count ++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        count = 20;
                    break;
                }
            }
            else if (turn == 2){
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
            if (turn == 1) {
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
            else if (turn == 2) {
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
        new MonteCarlo(Integer.parseInt(args[1]), args[0]);
    }

}
