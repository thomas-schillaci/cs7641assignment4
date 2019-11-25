package assignment4;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import static assignment4.TicTacToe.emptyIterator;
import static assignment4.TicTacToe.evaluate;


public class QAgent extends Agent {

	private int t;
	private HashMap<Integer, float[]> q;
	private boolean training;
	private int lastState;
	private int action;
	private static Random random = new Random();
	private static float EPSILON = 0.03f;
	private static float EPSILON_DECAY = 25.0f;
	private final static float ALPHA = 0.1f;
	private static float GAMMA = 0.99f;
	private ExplorationMethod explorationMethod;

	public static void main(String[] args) {
		tryExplorationMethod(ExplorationMethod.OPTIMISTIC);
		for (float epsilon = 0.01f; epsilon < 0.1f; epsilon += 0.01f) {
			EPSILON = epsilon;
			System.out.println("Epsilon: " + epsilon);
			tryExplorationMethod(ExplorationMethod.EGREEDY);
		}
		for (float decay = 0.01f; decay < 0.32f; decay += 0.05f) {
			System.out.println("Decay: " + decay);
			EPSILON_DECAY = decay;
			tryExplorationMethod(ExplorationMethod.EGREEDYD);
		}
		float[] gammas = new float[]{0.0001f, 0.001f, 0.01f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.99f};
		for (float g : gammas) {
			GAMMA = g;
			int trials = 10;
			float[] scores = new float[trials];
			for (int i = 0; i < trials; i++) {
				scores[i] = (train(ExplorationMethod.OPTIMISTIC, 10000));
			}
			System.out.println("GAMMA: " + g);
			System.out.println(mean(scores));
			System.out.println(std(scores));
		}
		int[] games = new int[]{10000, 50000, 250000, 500000, 750000, 1000000};
		float[] means = new float[games.length];
		float[] sstd = new float[games.length];
		int trials = 3;
		for (int i = 0; i < games.length; i++) {
			float[] scores = new float[trials];
			int game = games[i];
			for (int j = 0; j < trials; j++) {
				scores[j] = train(ExplorationMethod.OPTIMISTIC, game);
				System.out.println(scores[j]);
			}
			means[i] = mean(scores);
			sstd[i] = std(scores);
		}
		System.out.println(Arrays.toString(means));
		System.out.println(Arrays.toString(sstd));
		trials = 5;
		float[] times = new float[trials];
		for (int i = 0; i < trials; i++) {
			long start = System.currentTimeMillis();
			train(ExplorationMethod.EGREEDY, 800000);
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

	private static void tryExplorationMethod(ExplorationMethod explorationMethod) {
		int trials = 10;
		float[] scores = new float[trials];
		for (int k = 0; k < trials; k++) {
			scores[k] = train(explorationMethod, 10000);
		}

		System.out.println(explorationMethod);
		System.out.println("Average score: " + mean(scores));
		System.out.println("Associated deviation: " + std(scores));
	}

	private static float train(ExplorationMethod explorationMethod, int games) {
		QAgent agentX = new QAgent(explorationMethod);
		for (int i = 0; i < games; i++) {
			Result result = TicTacToe.play(agentX, new NotSoRandomAgent());
			int reward;
			switch (result) {
				case DRAW:
					reward = 5;
					break;
				case X:
					reward = 10;
					break;
				case O:
					reward = -10;
					break;
				default:
					throw new Error("Unexpected result: " + result);
			}
			agentX.updateQValue(reward);
			agentX.resetTime();
		}
		agentX.setTraining(false);
		return evaluate(agentX, false);
	}

	public static float mean(float[] values) {
		float sum = 0;
		for (float value : values) {
			sum += value;
		}
		return sum / values.length;
	}

	public static float std(float[] values) {
		float std = 0;
		float mean = mean(values);
		for (float value : values) {
			std += (float) Math.pow(mean - value, 2);
		}
		return std / values.length;
	}

	public QAgent(ExplorationMethod explorationMethod) {
		this.t = 0;
		this.q = new HashMap<>();
		this.training = true;
		this.explorationMethod = explorationMethod;
	}

	public void resetTime() {
		this.t = 0;
	}

	private int countEmpty(int state) {
		int empty = 0;
		for (int i = 0; i < 16; i++) {
			if ((state / (int) Math.pow(3, i)) % 3 == 0) {
				empty++;
			}
		}
		return empty;
	}

	private int getState(String grid) {
		int res = 0;
		for (int i = 0; i < 16; i++) {
			if (grid.charAt(i) == 'x') {
				res += Math.pow(3, i);
			}
			if (grid.charAt(i) == 'o') {
				res += 2 * Math.pow(3, i);
			}
		}
		return res;
	}

	private float[] getQValues(int state) {
		if (!this.q.containsKey(state)) {
			int empty = countEmpty(state);
			float[] values = new float[empty];
			Arrays.fill(values, 10000.0f);
			this.q.put(state, values);
		}
		return this.q.get(state);
	}

	public void setTraining(boolean training) {
		this.training = training;
	}

	public void updateQValue(int reward) {
		updateQValue(reward, 0);
	}

	public void updateQValue(int reward, int state) {
		float[] values = getQValues(this.lastState);
		float max = 0;
		if (state != 0) {
			for (Float value : getQValues(state)) {
				float copy = value; // SORRY
				if (copy == 10000.0f) {
					copy = 0.0f;
				}
				if (copy > max) {
					max = copy;
				}
			}
		}
		values[this.action] = (1.0f - ALPHA) * values[this.action] + GAMMA * ALPHA * (reward + max);
		this.q.put(this.lastState, values);
	}

	private float epsilon() {
		return (float) Math.max(0.01f, Math.min(1.0f, 1.0f - Math.log10((this.t + 1) / EPSILON_DECAY)));
	}

	@Override
	public int play(String grid) {
		int state = getState(grid);
		this.t++;

		if (this.lastState != 0) {
			updateQValue(0, state);
		}

		this.lastState = state;
		int empty = countEmpty(state);

		boolean explore = false;

		switch (this.explorationMethod) {
			case EGREEDY:
				if (Math.random() < EPSILON && this.training) {
					explore = true;
					this.action = random.nextInt(empty);
				}
				break;
			case EGREEDYD:
				if (Math.random() < epsilon() && this.training) {
					explore = true;
					this.action = random.nextInt(empty);
				}
				break;
			case OPTIMISTIC:
				explore = true;
				float[] floats = getQValues(state);
				for (int i = 0; i < floats.length; i++) {
					float value = floats[i];
					if (value == 10000.0f) {
						floats[i] = 0.0f;
						this.q.put(state, floats);
						this.action = i;
						break;
					}
					if (i == floats.length - 1) {
						explore = false;
					}
				}
				break;
			default:
				throw new Error("Unexpected exploration method: " + explorationMethod);
		}

		if (!explore) {
			this.action = 0;
			for (int i = 0; i < empty; i++) {
				if (getQValues(state)[i] > getQValues(state)[this.action]) {
					this.action = i;
				}
			}
		}

		int count = 0;
		for (Iterator<Integer> it = emptyIterator(grid); it.hasNext(); ) {
			int index = it.next();
			if (count++ == this.action) {
				return index;
			}
		}

		return -1;
	}

}
