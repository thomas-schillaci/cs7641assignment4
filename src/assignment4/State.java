package assignment4;

public class State {

	private String str;
	private int[] actions;
	private int reward;
	private char player;
	private State[] nextStates;

	public State(String str) {
		this.str = str;
	}

	public State(String str, int[] actions, int reward, char player) {
		this.str = str;
		this.actions = actions;
		this.reward = reward;
		this.player = player;
	}

	public void setNextStates(State[] nextStates) {
		this.nextStates = nextStates;
	}

	public char getPlayer() {
		return player;
	}

	public String getStr() {
		return str;
	}

	public int[] getActions() {
		return actions;
	}

	public int getReward() {
		return reward;
	}

	public State[] getNextStates() {
		return nextStates;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		State state = (State) o;
		return str.equals(state.getStr());
	}

	@Override
	public int hashCode() {
		return str.hashCode();
	}
}
