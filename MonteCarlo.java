import java.util.*;
import java.lang.*;
import java.io.*;
import java.net.*;

class MonteCarlo {

    public Socket s;
    public BufferedReader sin;
    public PrintWriter sout;
    Random generator = new Random();

    double t1, t2;
    int me;
    int origMe;
    int boardState;
    int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;

    int validMoves[] = new int[64];
    int numValidMoves;

    SearchTree root = new SearchTree(state, me);


    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public MonteCarlo(int _me, String host) {
        me = _me;
        origMe = _me;
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

    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int move() {
        //while time remains for move
        root = new SearchTree(copyState(state), me);
        Vector<SearchTree> possibleMoves = new Vector<>();
        int myMove = 0;
        long endTime = System.currentTimeMillis() + 5200;
        while (System.currentTimeMillis() < endTime) {
            //myMove = generator.nextInt(numValidMoves);
            SearchTree nextLeaf =  traverse(root);
            possibleMoves.add(nextLeaf);
            int currentResult = rollout(nextLeaf);
            me = origMe;
            backPropagate(nextLeaf, currentResult);
        }
        //after time take move that had best win percentages
        myMove = bestChild(possibleMoves);
       return myMove;
    }

    private SearchTree traverse (SearchTree root) {
        //TODO: use an upper confidence bound to explore and exploit
        getValidMovesForTree(round, root);
        int move = root.explore();
        SearchTree nextState = new SearchTree(makeMoveOnState(root.state, move), me, root, move);
        return nextState;

    }

    private int rollout(SearchTree state) {
        int[][] copyState = copyState(state.state);
        while (isTerminalState(copyState) != true) { //TODO: Check that isTerminalState accurately returns when a game is done
            copyState = rollout_policy(copyState);
        }
        return calcWinner(copyState);
    }

    private int[][] rollout_policy(int[][] state) {
        switchPlayers();
        int[][] newState = copyState(state);
        // get possible moves
        getValidMoves(round, newState);
        int numValidMovesThisState = numValidMoves;
        int[] validMovesThisState = validMoves.clone();
        // select one randomly
        int myMove = validMovesThisState[generator.nextInt(numValidMovesThisState+1)];
        // assign newState as state + the random move
        makeMoveOnState(newState, myMove);
        return newState;
    }

    private void backPropagate(SearchTree state, int win) {
        if (state.parent == null) {
            return;
        }
        state.setVisits();
        state.setWins(win);
        backPropagate(state.parent, win);
    }

    private int bestChild(Vector<SearchTree> moves) {
        //return child with the most visits
        int move = 0;
        double percentage = 0;
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i).wins / (double)moves.get(i).visits > percentage) {
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

    private int calcWinner(int[][] state) {
        int myCount = 0;
        int opponentCount = 0;
        for(int j = 0; j < state.length; j++){
            for(int k = 0; k < state[j].length; k++){
                if (state[j][k] == me) {
                    myCount++;
                } else if (state[j][k] != me && state[j][k] != 0) {
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

    private boolean isTerminalState(int[][] state) {
        getValidMoves(round, state);
        int moveCount = numValidMoves;
        if (moveCount <= 0) {
            return true;
        }
        else {
            return false;
        }
    }

    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private int getValidMoveCount(int round, int state[][]) {
        int moveCount = 0;
        int i, j;

        if (round < 4) {
            if (state[3][3] == 0) {
                moveCount ++;
            }
            if (state[3][4] == 0) {
                moveCount ++;
            }
            if (state[4][3] == 0) {
                moveCount ++;
            }
            if (state[4][4] == 0) {
                moveCount ++;
            }
        }
        else {
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(state, i, j)) {
                            moveCount ++;
                        }
                    }
                }
            }
        }

        return moveCount;
    }

    private void getValidMovesForTree(int round, SearchTree node) {
        int i, j;

        node.numValidMoves = 0;
        if (round < 4) {
            if (node.state[3][3] == 0) {
                node.validMoves[node.numValidMoves] = 3 * 8 + 3;
                node.numValidMoves++;
            }
            if (node.state[3][4] == 0) {
                node.validMoves[node.numValidMoves] = 3 * 8 + 4;
                node.numValidMoves++;
            }
            if (node.state[4][3] == 0) {
                node.validMoves[node.numValidMoves] = 4 * 8 + 3;
                node.numValidMoves++;
            }
            if (node.state[4][4] == 0) {
                node.validMoves[node.numValidMoves] = 4 * 8 + 4;
                node.numValidMoves++;
            }
            for (i = 0; i < node.numValidMoves; i++) {
            }
        } else {
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (node.state[i][j] == 0) {
                        if (couldBe(node.state, i, j)) {
                            node.validMoves[node.numValidMoves] = i * 8 + j;
                            node.numValidMoves++;
                        }
                    }
                }
            }
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
        new MonteCarlo(Integer.parseInt(args[1]), args[0]);
    }

}
