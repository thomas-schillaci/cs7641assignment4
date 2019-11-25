package assignment4;

import java.util.Random;
import java.util.Scanner;

public class BlackJack {

	private static Random random = new Random();
	public final static int[] CARDS = new int[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10, 10};

	public static void main(String[] args) {
		play(new BHuman(), true);
	}

	private static int[] newGame() {
		return new int[]{pick(), pick() + pick()};
	}

	public static int pick() {
		return CARDS[random.nextInt(CARDS.length)];
	}

	public static int getState(int[] hands) {
		return (hands[0] - 2) + (hands[1] - 4) * 9;
	}

	public static int[] getHands(int state) {
		return new int[]{state % 9 + 2, state / 9 + 4};
	}

	public static void display(int[] hands) {
		display(hands, false);
	}

	public static void display(int[] hands, boolean newline) {
		String dealer = (hands[0] < 10 ? " " + hands[0] : hands[0] + "");
		String player = (hands[1] < 10 ? " " + hands[1] : hands[1] + "");
		System.out.print("\rDealer: " + dealer + " - player: " + player + (newline ? "\t\n" : "\t"));
	}

	public static BResult play(BAgent agent) {
		return play(agent, false);
	}

	public static BResult play(BAgent agent, boolean verbose) {
		int[] hands = newGame();

		if (verbose) {
			display(hands);
		}

		while (agent.play(hands)) {
			hands[1] += pick();
			if (hands[1] > 21) {
				if (verbose) {
					display(hands, true);
					System.out.println("Dealer wins!");
				}
				return BResult.DEALER;
			}
			if (verbose) {
				display(hands);
			}
		}

		while (hands[0] < 17) {
			hands[0] += pick();
			if (hands[0] > 21) {
				if (verbose) {
					display(hands, true);
					System.out.println("Player wins!");
				}
				return BResult.PLAYER;
			}
		}
		if (verbose) {
			display(hands, true);
		}

		if (hands[0] == hands[1]) {
			if (verbose) {
				System.out.println("Draw");
			}
			return BResult.DRAW;
		}

		if (hands[0] > hands[1]) {
			if (verbose) {
				System.out.println("Dealer wins!");
			}
			return BResult.DEALER;
		}
		if (verbose) {
			System.out.println("Player wins!");
		}
		return BResult.PLAYER;
	}

	public static float evaluate(BAgent agent) {
		return evaluate(agent);
	}

	public static float evaluate(BAgent agent, boolean verbose) {
		int wd = 0;
		int games = 10000000;
		float score = 0;
		for (int i = 0; i < games; i++) {
			if (play(agent) != BResult.DEALER) {
				wd += 1;
			}
			score = ((float) wd * 100) / (i + 1);
			if (i % (games / 10) == 0 && verbose) {
				System.out.print("Ratio of #win/#draw / #games: " + score + "%\r");
			}
		}
		if (verbose) {
			System.out.println("Ratio of #win/#draw / #games: " + score + "%");
		}
		return score;
	}

}

abstract class BAgent {

	public abstract boolean play(int[] hands);

}

class BHuman extends BAgent {

	private Scanner sc = new Scanner(System.in);

	@Override
	public boolean play(int[] hands) {
		String str = sc.next().toLowerCase();
		return str.equals("true") || str.equals("t");
	}

}

enum BResult {
	DEALER,
	PLAYER,
	DRAW
}
