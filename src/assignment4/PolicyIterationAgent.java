package assignment4;

import java.util.*;

import static assignment4.QAgent.mean;
import static assignment4.QAgent.std;
import static assignment4.TicTacToe.evaluate;
import static assignment4.ValueIterationAgent.statesIterator;

public class PolicyIterationAgent extends Agent {

	private static float GAMMA = 0.001f;
	private final static float THETA = 0.1f;
	private final static int MAX_ITER = 5;

	private HashMap<State, Float> lastV = new HashMap<>();
	private HashMap<State, Float> v = new HashMap<>();
	private HashMap<State, Integer> policy = new HashMap<>();
	private Random random = new Random();

	public PolicyIterationAgent() {
		loadAndLink();
	}

	private boolean hasConverged() {
		for (Map.Entry<State, Float> entry : v.entrySet()) {
			State state = entry.getKey();
			float value = entry.getValue();
			float delta = Math.abs(value - lastV.get(state));
			if (delta >= THETA * (1.0f - GAMMA) / GAMMA) {
				return false;
			}
		}
		return true;
	}

	private void loadAndLink() {
		// LOAD TABLES

		HashMap<String, State> states = new HashMap<>();

		int count = 0;
		for (Iterator<State> it = statesIterator(); it.hasNext(); ) {
			if (count % 100000 == 0) {
				System.out.print("Loading tables - " + count / 51966 + "%\r");
			}
			State state = it.next();
			if (state.getPlayer() == 'x') {
				v.put(state, 0.0f);
				count++;
			}
			states.put(state.getStr(), state);
		}
		System.out.println("Loading tables - done");

		// LINK STATES

		count = 0;
		for (State state : states.values()) {
			if (count++ % 100000 == 0) {
				System.out.print("Linking states - " + count / 51966 / 2 + "%\r");
			}
			State[] nextStates = new State[state.getActions().length];
			int[] actions = state.getActions();
			for (int i = 0; i < actions.length; i++) {
				int action = actions[i];
				StringBuilder stringBuilder = new StringBuilder(state.getStr());
				stringBuilder.setCharAt(action, state.getPlayer());
				nextStates[i] = states.get(stringBuilder.toString());
			}
			state.setNextStates(nextStates);
		}
		System.out.println("Linking states - done");
	}

	private ArrayList<Integer> train() {
		ArrayList<Integer> iters = new ArrayList<>();

		for (State state : v.keySet()) {
			int empty = 0;
			for (int i = 0; i < 16; i++) {
				if (state.getStr().charAt(i) == '.') {
					empty++;
				}
			}
			int full = 16 - empty;
			v.put(state, (float) Math.exp(-1.05f * full + 16.5f));
			policy.put(state, random.nextInt(16));
		}


		for (int k = 0; k < MAX_ITER; k++) {
			System.out.println("Iteration " + (k + 1) + "/" + MAX_ITER);

			// POLICY EVALUATION

			int trial = 1;
			while (true) {
				lastV = new HashMap<>(v);

				int iterations = 0;
				for (State state : v.keySet()) {
					iterations += 1;
					int percentage = (int) ((float) iterations / v.size() * 100);
					if (random.nextFloat() < 100.0f / v.size()) {
						System.out.print("Policy evaluation, trial #" + trial + " - " + percentage + "%\r");
					}

					float sum = 0;
					for (State semiState : state.getNextStates()) {
						float prob = 1.0f / semiState.getNextStates().length; // FIXME
						if (prob == 1.0f) {
							sum = semiState.getNextStates()[0].getReward();
						} else {
							for (State nextState : semiState.getNextStates()) {
								if (nextState.getReward() == -10) {
									sum = -10;
									break;
								}
								sum += prob * (nextState.getReward() + GAMMA * lastV.get(nextState));
							}
						}
					}

					v.put(state, sum);
				}

				if (hasConverged()) {
					break;
				}
				System.out.println("Policy evaluation, trial #" + trial + " - diverged");
				trial++;
			}

			iters.add(trial);

			System.out.println("Policy evaluation, trial #" + trial + " - converged");

			// POLICY IMPROVEMENT

			HashMap<State, Integer> lastPolicy = new HashMap<>(policy);

			int iterations = 0;
			for (State state : v.keySet()) {
				iterations += 1;
				int percentage = (int) ((float) iterations / v.size() * 100);
				if (random.nextFloat() < 100.0f / v.size()) {
					System.out.print("Policy improvement - " + percentage + "%\r");
				}

				float maximum = -1000;
				int argument = -1;
				for (int i = 0; i < state.getActions().length; i++) {
					State semiState = state.getNextStates()[i];
					float sum = 0;
					float prob = 1.0f / semiState.getNextStates().length;
					if (prob == 1.0f) {
						sum = semiState.getNextStates()[0].getReward();
					} else {
						for (State nextState : semiState.getNextStates()) {
							sum += prob * (nextState.getReward() + GAMMA * v.get(nextState));
						}
					}
					if (sum > maximum) {
						maximum = sum;
						argument = state.getActions()[i];
					}
				}
				policy.put(state, argument);
			}
			System.out.println("Policy improvement - done");
			if (lastPolicy.equals(policy)) {
				System.out.println("Policy stable");
				break;
			}
		}
		System.out.println("Training done");
		return iters;
	}

	@Override
	public int play(String str) {
		State buffer = new State(str);
		return policy.get(buffer);
	}

	public static void main(String[] args) {
		float[] gammas = new float[]{0.00001f,0.0001f,0.001f, 0.01f, 0.1f, 0.5f, 0.75f, 0.99f};
		for (float g : gammas) {
			GAMMA = g;
			PolicyIterationAgent agent = new PolicyIterationAgent();
			System.out.println(agent.train());
			evaluate(agent, false);
		}
		int trials = 2;
		float[] times = new float[trials];
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			PolicyIterationAgent agent = new PolicyIterationAgent();
			agent.train();
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

}
