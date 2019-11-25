package assignment4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import static assignment4.BlackJack.*;
import static assignment4.QAgent.mean;
import static assignment4.QAgent.std;

public class PolicyIterationBAgent extends BAgent {

	private static float GAMMA = 0.01f;
	private final static float THETA = 1;
	private final static int MAX_ITER = 10;
	private final static int STATES = 162;

	private float[] lastV;
	private float[] v;
	private boolean[] policy;

	public PolicyIterationBAgent() {
		v = new float[STATES];
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

	public ArrayList<Integer> train() {
		ArrayList<Integer> iters = new ArrayList<>();

		System.out.println("Training...");

		for (int k = 0; k < MAX_ITER; k++) {

			// POLICY EVALUATION

			int trial = 0;
			do {
				trial++;
				lastV = Arrays.copyOf(v, v.length);

				for (int state = 0; state < STATES; state++) {
					v[state] = 0.0f;

					int[] hands = getHands(state);
					boolean action = policy[state];

					if (action) {
						float prob = 1.0f / CARDS.length;
						for (int card : CARDS) {
							if (hands[1] + card < 22) {
								v[state] += prob * (0 + GAMMA * lastV[getState(new int[]{hands[0], hands[1] + card})]);
							} else {
								v[state] += prob * -10;
							}
						}
					} else {
						int[] outcomes = enumerateScenarios(hands[0], hands[1]);
						float win = (float) outcomes[1] / (outcomes[0] + outcomes[1]);
						float lose = 1.0f - win;
						v[state] = win * 10 + lose * -10;
					}
				}
			} while (!hasConverged());
			iters.add(trial);

			// POLICY IMPROVEMENT

			boolean[] lastPolicy = Arrays.copyOf(policy, policy.length);
			boolean policyStable = true;

			for (int state = 0; state < STATES; state++) {
				float sum1 = 0;

				int[] hands = getHands(state);
				float prob = 1.0f / CARDS.length;
				for (int card : CARDS) {
					if (hands[1] + card < 22) {
						sum1 += prob * (0 + GAMMA * v[getState(new int[]{hands[0], hands[1] + card})]);
					} else {
						sum1 += prob * -10;
					}
				}

				int[] outcomes = enumerateScenarios(hands[0], hands[1]);
				float win = (float) outcomes[1] / (outcomes[0] + outcomes[1]);
				float lose = 1.0f - win;
				float sum2 = win * 10 + lose * -10;

				policy[state] = sum1 >= sum2;
				if (policy[state] != lastPolicy[state]) {
					policyStable = false;
				}
			}

			if (policyStable) {
				break;
			}
		}
		System.out.println("Done");

		return iters;
	}

	@Override
	public boolean play(int[] hands) {
		return policy[getState(hands)];
	}

	public static void main(String[] args) {
		float[] gammas = new float[]{0.01f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 0.99f};
		for (float g : gammas) {
			GAMMA = g;
			PolicyIterationBAgent agent = new PolicyIterationBAgent();
			System.out.println(agent.train());
			evaluate(agent);
		}
		int trials = 5;
		float[] times = new float[trials];
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			PolicyIterationBAgent agent = new PolicyIterationBAgent();
			agent.train();
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

}
