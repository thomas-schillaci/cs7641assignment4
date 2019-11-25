package assignment4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static assignment4.BlackJack.*;
import static assignment4.QAgent.mean;
import static assignment4.QAgent.std;

public class ValueIterationBAgent extends BAgent {

	private static float GAMMA = 0.01f;
	private final static float THETA = 0.1f;
	private final static int STATES = 162;

	private float[] lastV;
	private float[] v;
	private boolean[] policy;

	public ValueIterationBAgent() {
		v = new float[STATES];
		for (int state = 0; state < STATES; state++) {
			int[] hands = getHands(state);
			if (hands[1] <= 11) {
				v[state] = 0.63f * hands[1] - 2.3f;
			} else {
				v[state] = 1.25f * hands[1] - 16.2f;
			}
		}
		policy = new boolean[STATES];
	}

	private boolean hasConverged() {
		for (int state = 0; state < STATES; state++) {
			if (Math.abs(v[state] - lastV[state]) >= THETA * (1.0f - GAMMA) / GAMMA) {
				return false;
			}
		}
		return true;
	}

	private int[] enumerateScenarios(int dealer, int player) {
		int[] res = new int[]{0, 0};
		enumerateScenariosUtil(dealer, player, res);
		return res;
	}

	private void enumerateScenariosUtil(int dealer, int player, int[] res) {
		HashSet<Integer> cards = new HashSet<>();
		for (int card : CARDS) cards.add(card);
		for (int card : cards) {
			if (card + dealer < 17) {
				enumerateScenariosUtil(card + dealer, player, res);
			} else if (card + dealer < 22) {
				res[card + dealer > player ? 0 : 1]++;
			} else {
				res[1]++;
			}

		}
	}

	public int train() {
		System.out.println("Training...");
		int iterations = 0;
		do {
			iterations++;
			lastV = Arrays.copyOf(v, v.length);

			for (int state = 0; state < STATES; state++) {
				v[state] = 0.0f;
				int[] hands = getHands(state);

				float sum1 = 0;
				float prob = 1.0f / CARDS.length;
				for (int card : CARDS) {
					if (hands[1] + card < 22) {
						sum1 += prob * (0 + GAMMA * lastV[getState(new int[]{hands[0], hands[1] + card})]);
					} else {
						sum1 += prob * -10;
					}
				}

				int[] outcomes = enumerateScenarios(hands[0], hands[1]);
				float win = (float) outcomes[1] / (outcomes[0] + outcomes[1]);
				float lose = 1.0f - win;
				float sum2 = win * 10 + lose * -10;

				policy[state] = sum1 > sum2;
				v[state] = Math.max(sum1, sum2);
			}
		} while (!hasConverged());
		System.out.println("Done");
		return iterations;
	}

	@Override
	public boolean play(int[] hands) {
		return policy[getState(hands)];
	}

	public static void main(String[] args) {
		float[] gammas = new float[]{0.01f,0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f};
		ArrayList<Integer> iters = new ArrayList<>();
		for (float g : gammas) {
			GAMMA = g;
			ValueIterationBAgent agent = new ValueIterationBAgent();
			iters.add(agent.train());
			evaluate(agent);
			for (int i = 0; i < 100; i++) {
				BlackJack.play(agent);
			}
		}
		System.out.println(iters);
		int trials = 5;
		float[] times = new float[trials];
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			ValueIterationBAgent agent = new ValueIterationBAgent();
			agent.train();
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

}
