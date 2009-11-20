package net.yapbam.gui.transactiontable;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import net.yapbam.data.AbstractTransaction;
import net.yapbam.data.SubTransaction;
import net.yapbam.gui.LocalizationData;

public class SubTransactionsTableModel extends AbstractTableModel implements ColoredModel {
	private static final long serialVersionUID = 1L;

	private List<SubTransaction> subTransactions;
	
	public SubTransactionsTableModel() {
		this.subTransactions = new ArrayList<SubTransaction>();
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex==1) return double[].class;
		return String.class;
	}

	public int getColumnCount() {
		return 3;
	}

	@Override
	public String getColumnName(int columnIndex) {
		if (columnIndex==0) return LocalizationData.get("Transaction.description"); //$NON-NLS-1$
		if (columnIndex==1) return LocalizationData.get("Transaction.amount"); //$NON-NLS-1$
		if (columnIndex==2) return LocalizationData.get("Transaction.category"); //$NON-NLS-1$
		return "?"; //$NON-NLS-1$
	}

	public int getRowCount() {
		return this.subTransactions.size();
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		SubTransaction st = this.subTransactions.get(rowIndex);
		if (columnIndex==0) return st.getDescription();
		if (columnIndex==1) return new double[]{st.getAmount()};
		if (columnIndex==2) return st.getCategory().getName();
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	public void fill(AbstractTransaction transaction) {
		this.subTransactions.clear();
		for (int i = 0; i < transaction.getSubTransactionSize(); i++) {
			this.subTransactions.add(transaction.getSubTransaction(i));
		}
	}
	
	public SubTransaction get(int index) {
		return this.subTransactions.get(index);
	}
	
	@Override
	public void setRowLook(Component renderer, JTable table, int row, boolean isSelected, boolean hasFocus) {
		if (isSelected) {
	        renderer.setBackground(table.getSelectionBackground());
	        renderer.setForeground(table.getSelectionForeground());
	    } else {
	        boolean expense = this.subTransactions.get(row).getAmount()<0;  	
	        renderer.setForeground(table.getForeground());
	        renderer.setBackground(expense?GenericTransactionTableModel.CASHOUT:GenericTransactionTableModel.CASHIN);
	    }
	}

	@Override
	public int getAlignment(int column) {
    	return SwingConstants.LEFT;
	}

	public void add(SubTransaction sub) {
		this.subTransactions.add(sub);
		this.fireTableRowsInserted(getRowCount()-1, getRowCount()-1);
	}

	public void remove(int index) {
		this.subTransactions.remove(index);
		this.fireTableRowsDeleted(index, index);
	}

	public void replace(int index, SubTransaction sub) {
		this.subTransactions.set(index, sub);
		this.fireTableRowsUpdated(index, index);
	}
}
