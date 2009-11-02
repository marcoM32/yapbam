package net.yapbam.gui.transactiontable;

public interface ColoredModel {
	public abstract boolean isExpense(int row);

	public abstract boolean isChecked(int row);
	
	public abstract int getAlignment(int column);
}
