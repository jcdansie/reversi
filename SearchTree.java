import java.util.Random;
import java.util.Vector;

public class SearchTree {
    int wins = 0;
    int visits = 0;
    int me;
    int move;
    int[][] state;
    int[] validMoves = new int[64];
    Vector<SearchTree> children;
    int numValidMoves;
    Random generator = new Random();
    SearchTree parent = null;

    public SearchTree(int[][] state, int me) {
        this.state = state;
        this.me = me;
    }

    public SearchTree(int[][] state, int me, SearchTree parent, int move) {
        this.state = state;
        this.me = me;
        this.parent = parent;
        this.move = move;
    }

    public int explore() {
        // select one randomly
        int myMove = validMoves[generator.nextInt(numValidMoves+1)];
        // assign newState as state + the random move
        return myMove;
    }

    public int getNumValidMoves() {
        return numValidMoves;
    }

    public void setNumValidMoves(int numValidMoves) {
        this.numValidMoves = numValidMoves;
    }

    public void setWins(int win) {
        wins += win;
    }

    public void setVisits() {
        visits++;
    }
}
