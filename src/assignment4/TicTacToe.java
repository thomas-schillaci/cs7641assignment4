package assignment4;

import java.util.Iterator;
import java.util.Scanner;

public class TicTacToe {

	public static void main(String[] args) {
		Agent agentX = new RandomAgent();
		Agent agentO = new Human();
		System.out.println(play(agentX, agentO, true) + " wins!");
	}

	private static String newGame() {
		return "................";
	}

	public static boolean isFull(String grid) {
		return !grid.contains(".");
	}

	public static Result isFinished(String grid) {
		for (int k = 0; k < 4; k++) {
			String row = grid.substring(4 * k, 4 * (k + 1));

			StringBuilder column = new StringBuilder();
			for (int i = 0; i < 4; i++) {
				column.append(grid.charAt(4 * i + k));
			}

			StringBuilder diag1 = new StringBuilder();
			diag1.append(grid.charAt(0));
			diag1.append(grid.charAt(5));
			diag1.append(grid.charAt(10));
			diag1.append(grid.charAt(15));

			StringBuilder diag2 = new StringBuilder();
			diag2.append(grid.charAt(3));
			diag2.append(grid.charAt(6));
			diag2.append(grid.charAt(9));
			diag2.append(grid.charAt(12));

			if (row.equals("xxxx") || column.toString().equals("xxxx") || diag1.toString().equals("xxxx") || diag2.toString().equals("xxxx")) {
				return Result.X;
			}
			if (row.equals("oooo") || column.toString().equals("oooo") || diag1.toString().equals("oooo") || diag2.toString().equals("oooo")) {
				return Result.O;
			}
		}

		return isFull(grid) ? Result.DRAW : Result.UNFINISHED;
	}

	public static int countEmpty(String grid) {
		int count = 0;
		for (int i = 0; i < 16; i++) {
			if (grid.charAt(i) == '.') {
				count++;
			}
		}
		return count;
	}

	public static Iterator<Integer> emptyIterator(String grid) {
		return new Iterator<Integer>() {
			int start = 0;

			@Override
			public boolean hasNext() {
				if (start == -1) return false;
				return grid.substring(start).contains(".");
			}

			@Override
			public Integer next() {
				start = grid.indexOf(".", start);
				if (start != -1) {
					start++;
					return start - 1;
				}
				return -1;
			}
		};
	}

	public static void display(String grid) {
		for (int i = 0; i < 4; i++) {
			System.out.println(grid.substring(4 * i, 4 * (i + 1)));
		}
	}

	public static boolean isFeasible(String grid) {
		int xs = 0;
		int os = 0;

		for (int i = 0; i < 16; i++) {
			if (grid.charAt(i) == 'x') {
				xs++;
			}
			if (grid.charAt(i) == 'o') {
				os++;
			}
		}

		return xs == os || xs == os + 1;
	}

	public static Result play(Agent agentX, Agent agentO) {
		return play(agentX, agentO, false);
	}

	public static Result play(Agent agentX, Agent agentO, boolean display) {
		String grid = newGame();

		if (display) {
			display(grid);
			System.out.println();
		}

		int player = 0;
		Result result;
		while ((result = isFinished(grid)) == Result.UNFINISHED) {
			int action;
			char cell;
			if (player == 0) {
				action = agentX.play(grid);
				cell = 'x';
			} else {
				action = agentO.play(grid);
				cell = 'o';
			}
			if (grid.charAt(action) != '.') {
				throw new Error("Non empty cell player " + player);
			}
			StringBuilder stringBuilder = new StringBuilder(grid);
			stringBuilder.setCharAt(action, cell);
			grid = stringBuilder.toString();
			player = 1 - player;

			if (display) {
				display(grid);
				System.out.println();
			}
		}

		return result;
	}

	public static float evaluate(Agent agent) {
		return evaluate(agent, true);
	}

	public static float evaluate(Agent agent, boolean verbose) {
		int wd = 0;
		float score = 0;
		for (int i = 0; i < 10000; i++) {
			if (play(agent, new RandomAgent()) != Result.O) {
				wd += 1;
			}
			score = ((float) wd / 100);
			if (verbose) {
				System.out.print(score + "% of win/draw\r");
			}
		}
		if (verbose) {
			System.out.println(((float) wd / 100) + "% of win/draw");
		}
		return score;
//		System.out.println(play(agent, new Human(), true));
	}

}

abstract class Agent {

	public abstract int play(String grid);

}

class RandomAgent extends Agent {


	@Override
	public int play(String grid) {
		int index = (int) (Math.random() * TicTacToe.countEmpty(grid));
		int count = 0;
		for (Iterator<Integer> it = TicTacToe.emptyIterator(grid); it.hasNext(); ) {
			int emptyIndex = it.next();
			if (count++ == index) return emptyIndex;
		}
		return -1;
	}

}

class FCAgent extends Agent {

	@Override
	public int play(String grid) {
		return TicTacToe.emptyIterator(grid).next();
	}

}

class NotSoRandomAgent extends Agent {


	@Override
	public int play(String grid) {
		for (Iterator<Integer> it = TicTacToe.emptyIterator(grid); it.hasNext(); ) {
			int index = it.next();
			StringBuilder stringBuilder = new StringBuilder(grid);
			stringBuilder.setCharAt(index, 'o');
			if (TicTacToe.isFinished(stringBuilder.toString()) == Result.O) {
				return index;
			}
		}
		return new RandomAgent().play(grid);
	}
}

class Human extends Agent {

	@Override
	public int play(String grid) {
		Scanner sc = new Scanner(System.in);
		int i = sc.nextInt();
		if (grid.charAt(i) != ' ') {
			return i;
		}
		return play(grid);
	}
}

enum Result {
	DRAW,
	X,
	O,
	UNFINISHED
}