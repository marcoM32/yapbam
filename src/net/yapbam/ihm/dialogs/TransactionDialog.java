package net.yapbam.ihm.dialogs;

import java.awt.GridBagConstraints;
import java.awt.Window;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.*;

import net.yapbam.data.*;
import net.yapbam.date.helpers.DateStepper;
import net.yapbam.ihm.LocalizationData;
import net.yapbam.ihm.widget.DateWidget;

/** This dialog allows to create or edit a transaction */
public class TransactionDialog extends AbstractTransactionDialog {
	//FIXME Bug when you press return during editing the date, the valueDate is not refreshed
	private static final long serialVersionUID = 1L;
	
	private DateWidget date;
	private JTextField transactionNumber;
	private DateWidget defDate;
	private JTextField statement;
	
	/** Display the creation dialog, if the creation is confirmed, add the transaction to the global data 
	 * @param data the global data
	 * @param frame the dialog's parent frame
	 * @param transaction the transaction we want to edit, or null if we want to create a new transaction
	 * @return the new transaction or the edited one
	 */
	public static Transaction open(GlobalData data, Window frame, Transaction transaction) {
		if (data.getAccountsNumber()==0) {
			//Need to create an account first
			AccountDialog.open(data, frame, LocalizationData.get("TransactionDialog.needAccount")); //$NON-NLS-1$
			if (data.getAccountsNumber()==0) return null;
		}
		TransactionDialog dialog = new TransactionDialog(frame, data, transaction);
		dialog.setVisible(true);
		Transaction newTransaction = dialog.getTransaction();
		if (newTransaction!=null) {
			if (transaction!=null) data.remove(transaction);
			data.add(newTransaction);
		}
		return newTransaction;
	}
	
	private TransactionDialog(Window owner, GlobalData data, Transaction transaction) {
		super(owner, (transaction==null?LocalizationData.get("TransactionDialog.title.new"):LocalizationData.get("TransactionDialog.title.edit")), data, transaction);
	}

	protected void setContent(AbstractTransaction transaction) {
		super.setContent(transaction);
		Transaction t = (Transaction) transaction;
		date.setDate(t.getDate());
		transactionNumber.setText(t.getNumber());
		defDate.setDate(t.getValueDate());
		statement.setText(t.getStatement());
	}

	@Override
	protected Object buildResult() {
		double amount = Math.abs(((Number)this.amount.getValue()).doubleValue());
		if (!this.receipt.isSelected()) amount = -amount;
		String statementId = statement.getText().trim();
		if (statementId.length()==0) statementId = null;
		String number = transactionNumber.getText().trim();
		if (number.length()==0) number = null;
		ArrayList<SubTransaction> subTransactions = new ArrayList<SubTransaction>();
		for (int i = 0; i < subtransactionsPanel.getSubtransactionsCount(); i++) {
			subTransactions.add(subtransactionsPanel.getSubtransaction(i));
		}
		return new Transaction(date.getDate(), number, description.getText().trim(), amount,
				this.data.getAccount(selectedAccount), getCurrentMode(), categories.getCategory(),
				defDate.getDate(), statementId, subTransactions);
	}
	
	protected void buildStatementFields(JPanel centerPane, FocusListener focusListener, KeyListener listener, GridBagConstraints c) {
		centerPane.add(new JLabel(LocalizationData.get("TransactionDialog.valueDate")), c); //$NON-NLS-1$
		defDate = new DateWidget();
		defDate.addFocusListener(focusListener);
		defDate.addKeyListener(listener);
        c.gridx=1; c.weightx=0; c.fill = GridBagConstraints.HORIZONTAL;
		centerPane.add(defDate,c);
        c.gridx=2; c.fill=GridBagConstraints.NONE; c.weightx = 0;
        centerPane.add(new JLabel(LocalizationData.get("TransactionDialog.statement")), c); //$NON-NLS-1$
        statement = new JTextField(15);
        statement.addFocusListener(focusListener);
        c.gridx=3;
        centerPane.add(statement, c);
	}

	protected void buildNumberField(JPanel centerPane, FocusListener focusListener, GridBagConstraints c) {
		centerPane.add(new JLabel(LocalizationData.get("TransactionDialog.number")), c); //$NON-NLS-1$
        transactionNumber = new JTextField(15);
        transactionNumber.addFocusListener(focusListener);
        c.gridx++;
        centerPane.add(transactionNumber, c);
        c.gridx++;
	}

	protected void buildDateField(JPanel centerPane, FocusListener focusListener, KeyListener listener, GridBagConstraints c) {
		JLabel titleDate = new JLabel(LocalizationData.get("TransactionDialog.date")); //$NON-NLS-1$
		centerPane.add(titleDate, c);
		date = new DateWidget();
		date.addFocusListener(focusListener);
		date.addFocusListener(new FocusListener() {
			@Override
			public void focusLost(FocusEvent e) {
				Mode m = getCurrentMode();
				DateStepper vdc = receipt.isSelected()?m.getReceiptVdc():m.getExpenseVdc();
				defDate.setDate(vdc.getNextStep(date.getDate()));
			}
			@Override
			public void focusGained(FocusEvent e) {}
		});
		date.addKeyListener(listener);
        c.gridx++; c.weightx=0; c.fill = GridBagConstraints.HORIZONTAL;
		centerPane.add(date,c);
        c.gridx++;
	}

	public Transaction getTransaction() {
		return (Transaction) super.getResult();
	}

	protected void optionnalUpdatesOnModeChange() {
		boolean expense = !receipt.isSelected();
		Mode mode = getCurrentMode();
		//TODO transaction number may depend on the new selected mode
		transactionNumber.setText(""); //$NON-NLS-1$
		DateStepper vdc = expense?mode.getExpenseVdc():mode.getReceiptVdc();
		defDate.setDate(vdc.getNextStep(date.getDate()));
	}


	@Override
	protected String getOkDisabledCause() {
		String disabledCause = super.getOkDisabledCause(); 
		if (disabledCause!=null) return disabledCause;
		if (this.date.getDate()==null) return LocalizationData.get("TransactionDialog.bad.date"); //$NON-NLS-1$
		if (this.defDate.getDate()==null) return LocalizationData.get("TransactionDialog.bad.valueDate"); //$NON-NLS-1$
		return null;
	}
}
