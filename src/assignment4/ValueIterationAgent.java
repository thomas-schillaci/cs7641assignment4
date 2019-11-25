package assignment4;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static assignment4.QAgent.mean;
import static assignment4.QAgent.std;
import static assignment4.TicTacToe.*;

public class ValueIterationAgent extends Agent {

	private static float GAMMA = 0.001f;
	private final static float THETA = 0.1f;

	private HashMap<State, Float> lastV = new HashMap<>();
	private HashMap<State, Float> v = new HashMap<>();
	private HashMap<State, Integer> policy = new HashMap<>();

	public ValueIterationAgent() {
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

	public static int getReward(Result result) {
		switch (result) {
			case UNFINISHED:
				return 0;
			case X:
				return 10;
			case O:
				return -10;
			case DRAW:
				return 5;
			default:
				throw new Error("Unexpected result: " + result);
		}
	}

	public static Iterator<State> statesIterator() {
		return new Iterator<State>() {
			int index = 0;
			String next;

			private String getNext() {
				if (index == Math.pow(3, 16)) return null;
				StringBuilder strBuilder = new StringBuilder();
				for (int i = 0; i < 16; i++) {
					int res = (index / (int) Math.pow(3, i)) % 3;
					if (res == 0) {
						strBuilder.append(".");
					}
					if (res == 1) {
						strBuilder.append("x");
					}
					if (res == 2) {
						strBuilder.append("o");
					}
				}
				index++;
				String grid = strBuilder.toString();
				if (isFeasible(grid)) {
					return grid;
				}
				return getNext();
			}

			@Override
			public boolean hasNext() {
				if (next == null) {
					next = getNext();
				}
				return next != null;
			}

			@Override
			public State next() {
				if (next == null) {
					next = getNext();
				}
				String tmp = next;
				if (tmp == null) {
					return null;
				}
				next = null;

				int empty = countEmpty(tmp);
				int[] actions = new int[empty];

				int i = 0;
				for (Iterator<Integer> it = emptyIterator(tmp); i < empty; i++) {
					actions[i] = it.next();
				}

				int xs = 0;
				int os = 0;

				for (i = 0; i < 16; i++) {
					if (tmp.charAt(i) == 'x') {
						xs++;
					}
					if (tmp.charAt(i) == 'o') {
						os++;
					}
				}

				return new State(tmp, actions, getReward(isFinished(tmp)), xs == os ? 'x' : 'o');
			}
		};
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
				int empty = 0;
				for (int i = 0; i < 16; i++) {
					if (state.getStr().charAt(i) == '.') {
						empty++;
					}
				}
				int full = 16 - empty;
				v.put(state, -2.015f * full + 44.08f);
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

	private int train() {
		for (State state : v.keySet()) {
			int empty = 0;
			for (int i = 0; i < 16; i++) {
				if (state.getStr().charAt(i) == '.') {
					empty++;
				}
			}
			int full = 16 - empty;
			v.put(state, (float) Math.exp(-full + 16));
		}

		int trial = 1;
		while (true) {
			lastV = new HashMap<>(v);

			int iterations = 0;
			for (State state : v.keySet()) {
				if (iterations++ % 100000 == 0) {
					System.out.print("Trial #" + trial + " - " + iterations / 51966 + "%\r");
				}


				State[] semiStates = state.getNextStates();
				float maximum = -1000;
				int bestArgument = 0;
				for (int i = 0; i < semiStates.length; i++) {
					State semiState = semiStates[i];
					float sum = 0;
					float prob = 1.0f / semiState.getNextStates().length;

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

					if (sum > maximum) {
						maximum = sum;
						bestArgument = state.getActions()[i];
					}
				}
				v.put(state, maximum);
				policy.put(state, bestArgument);
			}
			if (hasConverged()) {
				break;
			} else {
				System.out.println("Trial #" + trial + " - diverged");
			}
			trial++;
		}
		System.out.println("Trial #" + trial + " - converged");
		System.out.println("Training done");

		return trial;
	}

	@Override
	public int play(String str) {
		State buffer = new State(str);
		int index = policy.get(buffer);
		if (str.charAt(index) == '.') {
			return index;
		}
		return emptyIterator(str).next();
	}

	public static void main(String[] args) {
		float[] gammas = new float[]{0.0001f, 0.001f, 0.01f, 0.1f, 0.5f, 0.75f, 0.99f};
		ArrayList<Integer> iters = new ArrayList<>();
		ValueIterationAgent agent = new ValueIterationAgent();
		for (float g : gammas) {
			GAMMA = g;
			iters.add(agent.train());
			evaluate(agent, false);
			System.out.println(iters.get(iters.size() - 1));
		}
		System.out.println(iters);
		int trials = 2;
		float[] times = new float[trials];
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			agent = new ValueIterationAgent();
			agent.train();
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

}
