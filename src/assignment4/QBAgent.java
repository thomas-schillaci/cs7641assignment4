package assignment4;

import java.util.Arrays;

import static assignment4.BlackJack.evaluate;
import static assignment4.BlackJack.getState;
import static assignment4.QAgent.mean;
import static assignment4.QAgent.std;

public class QBAgent extends BAgent {

	private int t;
	private float[][] q;
	private boolean training;
	private int lastState;
	private int action;
	private ExplorationMethod explorationMethod;

	private static float EPSILON = 0.9f;
	private static float EPSILON_DECAY = 0.1f;
	private final static float ALPHA = 0.1f;
	private static float GAMMA = 0.4f;
	private final static int STATES = 162;

	public static void main(String[] args) {
		tryExplorationMethod(ExplorationMethod.OPTIMISTIC);
		for (float epsilon : new float[]{0.5f, 0.95f}) {
			EPSILON = epsilon;
			System.out.println("Epsilon: " + epsilon);
			tryExplorationMethod(ExplorationMethod.EGREEDY);
		}
		for (float decay = 11; decay < 15.01f; decay += 2) {
			System.out.println("Decay: " + decay);
			EPSILON_DECAY = decay;
			tryExplorationMethod(ExplorationMethod.EGREEDYD);
		}
		float[] gammas = new float[]{0.0001f, 0.001f, 0.01f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.99f};
		for (float g : gammas) {
			GAMMA = g;
			System.out.println("GAMMA: " + g);
			System.out.println(train(ExplorationMethod.EGREEDY, 1000));
		}
		int[] games = new int[]{10, 100, 1000, 5000, 10000};
		float[] means = new float[games.length];
		float[] sstd = new float[games.length];
		int trials = 20;
		for (int i = 0; i < games.length; i++) {
			float[] scores = new float[trials];
			for (int j = 0; j < trials; j++) {
				int game = games[i];
				scores[j] = train(ExplorationMethod.EGREEDY, game);
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
			train(ExplorationMethod.EGREEDY, 6000);
			times[i] = (float) (System.currentTimeMillis() - start) / 1000;
		}
		System.out.println(mean(times));
		System.out.println(std(times));
	}

	private static void tryExplorationMethod(ExplorationMethod explorationMethod) {
		int trials = 100;
		float[] scores = new float[trials];
		for (int k = 0; k < trials; k++) {
			scores[k] = train(ExplorationMethod.OPTIMISTIC, 1000);
		}
		float sum = 0;
		for (float score : scores) {
			sum += score;
		}
		float mean = sum / trials;
		float std = 0;
		for (float score : scores) {
			std += (float) Math.pow(mean - score, 2) / trials;
		}
		System.out.println(explorationMethod);
		System.out.println("Average score: " + mean);
		System.out.println("Associated deviation: " + std);
	}

	private static float train(ExplorationMethod explorationMethod, int games) {
		QBAgent agentX = new QBAgent(explorationMethod);

		for (int i = 0; i < games; i++) {
			BResult result = BlackJack.play(agentX);
			int reward;
			switch (result) {
				case DRAW:
					reward = 5;
					break;
				case PLAYER:
					reward = 10;
					break;
				case DEALER:
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

	public QBAgent(ExplorationMethod explorationMethod) {
		this.q = new float[STATES][];
		for (int state = 0; state < STATES; state++) {
			if (explorationMethod == ExplorationMethod.OPTIMISTIC) {
				this.q[state] = new float[]{10000.0f, 10000.0f};
			} else {
				this.q[state] = new float[]{0.0f, 0.0f};
			}
		}
		this.training = true;
		this.explorationMethod = explorationMethod;
		this.t = 0;
	}

	public void resetTime() {
		this.t = 0;
	}

	private void setTraining(boolean training) {
		this.training = training;
	}

	private void updateQValue(int reward) {
		updateQValue(reward, -1);
	}

	private void updateQValue(int reward, int state) {
		float[] values = this.q[this.lastState];
		float max = 0;
		if (state != -1) {
			float[] newValues = this.q[state];
			float copy1 = newValues[0]; // SORRY AGAIN
			if (copy1 == 10000.0f) {
				copy1 = 0.0f;
			}
			float copy2 = newValues[1];
			if (copy2 == 10000.0f) {
				copy2 = 0.0f;
			}
			max = Math.max(copy1, copy2);
		}
		values[this.action] = (1.0f - ALPHA) * values[this.action] + GAMMA * ALPHA * (reward + max);
		this.q[this.lastState] = values;
	}

	private float epsilon() {
		return (float) Math.max(0.01f, Math.min(1.0f, 1.0f - Math.log10((this.t + 1) / EPSILON_DECAY)));
	}

	@Override
	public boolean play(int[] hands) {
		int state = getState(hands);
		updateQValue(0, state);
		this.lastState = state;
		this.t++;

		switch (explorationMethod) {
			case EGREEDY:
				if (Math.random() < EPSILON && this.training) {
					boolean decision = Math.random() < 0.5;
					this.action = decision ? 0 : 1;
					return decision;
				}
				break;
			case EGREEDYD:
				if (Math.random() < epsilon() && this.training) {
					boolean decision = Math.random() < 0.5;
					this.action = decision ? 0 : 1;
					return decision;
				}
				break;
			case OPTIMISTIC:
				float[] floats = this.q[state];
				for (int i = 0; i < floats.length; i++) {
					float value = floats[i];
					if (value == 10000.0f) {
						this.q[state][i] = 0.0f;
						this.action = i;
						return i == 0;
					}
				}
				break;
			default:
				throw new Error("Unexpected exploration method: " + explorationMethod);
		}


		float[] values = this.q[state];
		boolean decision = values[0] > values[1];
		this.action = (decision ? 0 : 1);
		return decision;
	}
}

enum ExplorationMethod {
	OPTIMISTIC,
	EGREEDY,
	EGREEDYD
}