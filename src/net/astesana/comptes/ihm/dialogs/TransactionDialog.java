package net.astesana.comptes.ihm.dialogs;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.*;

import net.astesana.comptes.data.*;
import net.astesana.comptes.date.helpers.DateStepper;
import net.astesana.comptes.ihm.IconManager;
import net.astesana.comptes.ihm.LocalizationData;
import net.astesana.comptes.ihm.MainFrame;
import net.astesana.comptes.ihm.widget.AmountWidget;
import net.astesana.comptes.ihm.widget.AutoSelectFocusListener;
import net.astesana.comptes.ihm.widget.CoolJComboBox;
import net.astesana.comptes.ihm.widget.DateWidget;

/** This dialog allows to create or edit a transaction */
public class TransactionDialog extends AbstractDialog {
	//FIXME Bug when a long mode name is added, the window is refreshed badly
	//FIXME Bug when you press return during editing the date, the valueDate is not refreshed
	private static final long serialVersionUID = 1L;
	private static final boolean DEBUG = false;
	
	private int selectedAccount;
	private JComboBox accounts;
	private JTextField description;
	private DateWidget date;
	private AmountWidget amount;
	private JCheckBox receipt;
	private int selectedMode;
	private CoolJComboBox modes;
	private JTextField transactionNumber;
	private CategoryPanel categories;
	private DateWidget defDate;
	private JTextField statement;
	private SubtransactionListPanel subtransactionsPanel;
	
	private GlobalData data;

	/** Display the creation dialog, if the creation is confirmed, add the transaction to the global data 
	 * @param data the global data
	 * @param frame the dialog's parent frame
	 * @param transaction the transaction we want to edit, or null if we want to create a new transaction
	 * @return the new transaction or the edited one
	 */
	public static Transaction open(GlobalData data, MainFrame frame, Transaction transaction) {
		if (data.getAccountsNumber()==0) {
			//Need to create an account first
			NewBankAccountDialog.open(data, frame, "Avant de pouvoir cr�er une op�ration, il est n�cessaire d'avoir cr�� au moins un compte");
			if (data.getAccountsNumber()==0) return null;
		}
		TransactionDialog dialog = new TransactionDialog(frame, data, transaction);
		dialog.setVisible(true);
		Transaction newTransaction = dialog.getTransaction();
		if (newTransaction!=null) {
			if (transaction!=null) data.removeTransaction(transaction);
			data.add(newTransaction);
		}
		return newTransaction;
	}
	
	private TransactionDialog(JFrame owner, GlobalData data, Transaction transaction) {
		super(owner, "Nouvelle op�ration", data); //LOCAL
		if (transaction!=null) setContent(transaction);
		this.data = data;
	}

	private void setContent(Transaction transaction) {
		Account account = transaction.getAccount();
		selectedAccount = data.indexOf(account);
		accounts.setSelectedIndex(selectedAccount);
		description.setText(transaction.getDescription());
		date.setDate(transaction.getDate());
		selectedMode = account.findMode(transaction.getMode(), transaction.getAmount()<=0);
		modes.setSelectedIndex(selectedMode);
		transactionNumber.setText(transaction.getNumber());
		categories.setCategory(transaction.getCategory());
		defDate.setDate(transaction.getValueDate());
		statement.setText(transaction.getStatement());
		subtransactionsPanel.fill(transaction);
		// Danger, subtransaction.fill throws Property Change events that may alter the amount field content.
		// So, set up the amountField after the subtransactions list.
		amount.setValue(Math.abs(transaction.getAmount()));
		receipt.setSelected(transaction.getAmount()>0);
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
		return new Transaction(date.getDate(), transactionNumber.getText(), description.getText().trim(), amount,
				this.data.getAccount(selectedAccount), getCurrentMode(), categories.getCategory(),
				defDate.getDate(), statementId, subTransactions);
	}
	
/**/	private JPanel combine (JComboBox box, JButton button) {
        JPanel pane = new JPanel(new GridBagLayout());
        Dimension dimension = box.getPreferredSize();
        button.setPreferredSize(new Dimension(dimension.height, dimension.height));
        GridBagConstraints c = new GridBagConstraints(); c.gridx = 1;
        pane.add(button, c);
        c.gridx = 0; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1;
        pane.add(box, c);
        return pane;
	}/**/ //TODO remove
	
	@Override
	protected JPanel createCenterPane(Object data) {
		this.data = (GlobalData) data;
		
        //Create the content pane.
        JPanel centerPane = new JPanel(new GridBagLayout());
        FocusListener focusListener = new AutoSelectFocusListener();
        KeyListener listener = new AutoUpdateOkButtonKeyListener(this);

        Insets insets = new Insets(5,5,5,5);
        JLabel titleCompte = new JLabel(LocalizationData.get("NewBankAccountDialog.account")); //$NON-NLS-1$
        GridBagConstraints c = new GridBagConstraints();
		c.insets = insets; c.gridx=0; c.gridy=0; c.anchor=GridBagConstraints.WEST;
        centerPane.add(titleCompte,c);
        accounts = new JComboBox(getAccounts());
        selectedAccount = 0; //TODO let select the last selected account
        accounts.setSelectedIndex(selectedAccount);
        AccountsListener accountListener = new AccountsListener();
		accounts.addActionListener(accountListener);
        accounts.setToolTipText("S�lectionnez dans ce menu le compte sur lequel s'effectue l'op�ration");
        JButton newAccount = new JButton(IconManager.NEW_ACCOUNT);
        newAccount.setFocusable(false);
        newAccount.addActionListener(accountListener);
        newAccount.setToolTipText("Cliquez ici pour cr�er un nouveau compte");
        c.gridx=1; c.gridwidth =5; c.fill = GridBagConstraints.HORIZONTAL; c.weightx=1;
        centerPane.add(combine(accounts, newAccount), c);
        
     	JLabel titleLibelle = new JLabel("Libell� :");
        c = new GridBagConstraints();
        c.insets = insets; c.gridx=0; c.gridy=1; c.anchor = GridBagConstraints.WEST;
		centerPane.add(titleLibelle, c);
        description = new JTextField(40);
        description.addFocusListener(focusListener);
        c.gridx=1; c.gridwidth=5; c.fill = GridBagConstraints.HORIZONTAL;
    	centerPane.add(description,c);
       
    	JLabel titleDate = new JLabel("Date de l'op�ration :");
        c = new GridBagConstraints();
        c.insets = insets; c.gridx=0; c.gridy=2; c.anchor = GridBagConstraints.WEST;
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
        c.gridx=1; c.weightx=0; c.fill = GridBagConstraints.HORIZONTAL;
		centerPane.add(date,c);
        c.gridx=2; c.fill=GridBagConstraints.NONE; c.anchor = GridBagConstraints.WEST; c.weightx = 0;
        centerPane.add(new JLabel("Montant :"),c);
        amount = new AmountWidget();
        amount.addFocusListener(focusListener);
        amount.addKeyListener(listener);
        amount.setValue(new Double(0));
        amount.setToolTipText("Entrez le montant de l'op�ration");
        c.gridx=3; c.weightx=0; c.fill = GridBagConstraints.HORIZONTAL;
        centerPane.add(amount,c);
        receipt = new JCheckBox("Recette");
        receipt.addItemListener(new ReceiptListener());
        c.gridx=4; c.weightx=0; c.anchor = GridBagConstraints.WEST;
        centerPane.add(receipt, c);
        
        c = new GridBagConstraints();
        c.insets = insets; c.gridx=0; c.gridy=3; c.anchor = GridBagConstraints.WEST;
		centerPane.add(new JLabel("Mode de paiement :"), c);
        modes = new CoolJComboBox();
        buildModes();
        selectedMode = 0;
        ModesListener modeListener = new ModesListener();
		modes.addActionListener(modeListener);
        modes.setToolTipText("S�lectionnez dans ce menu le mode de paiement");
        c.gridx=1; c.weightx=0;
        JButton newMode = new JButton(IconManager.NEW_MODE);
        newMode.setFocusable(false);
        newMode.addActionListener(modeListener);
        newMode.setToolTipText("Cliquez ici pour cr�er un nouveau mode de paiement");
        centerPane.add(combine(modes, newMode), c);
        c.gridx=2;
        centerPane.add(new JLabel("Num�ro :"), c);
        transactionNumber = new JTextField(15);
        transactionNumber.addFocusListener(focusListener);
        c.gridx=3;
        centerPane.add(transactionNumber, c);
        c.gridx=4;
        centerPane.add(new JLabel("Cat�gorie :"),c);
        categories = new CategoryPanel(this.data);
        c.gridx=5; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL;
        centerPane.add(categories, c);
        
        c = new GridBagConstraints();
		c.insets = insets; c.gridx=0; c.gridy=5; c.anchor = GridBagConstraints.WEST;
		centerPane.add(new JLabel("Date de valeur :"), c);
		defDate = new DateWidget();
		defDate.addFocusListener(focusListener);
		defDate.addKeyListener(listener);
        c.gridx=1; c.weightx=0; c.fill = GridBagConstraints.HORIZONTAL;
		centerPane.add(defDate,c);
        c.gridx=2; c.fill=GridBagConstraints.NONE; c.weightx = 0;
        centerPane.add(new JLabel("Relev� :"), c);
        statement = new JTextField(15);
        statement.addFocusListener(focusListener);
        c.gridx=3;
        centerPane.add(statement, c);

        c = new GridBagConstraints();
		c.insets = insets; c.gridx=0; c.gridy=6; c.gridwidth = 6; c.fill=GridBagConstraints.HORIZONTAL;
		centerPane.add(new JSeparator(JSeparator.HORIZONTAL),c);

		c = new GridBagConstraints();
		c.insets = insets; c.gridx=0; c.gridy=7; c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 3; c.fill=GridBagConstraints.HORIZONTAL;
		subtransactionsPanel = new SubtransactionListPanel(this.data);
		subtransactionsPanel.addPropertyChangeListener(SubtransactionListPanel.SUM_PROPERTY, new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				if ((amount.getValue()!=null) && subtransactionsPanel.isAddToTransactionSelected()) {
					double diff = (Double)evt.getNewValue()-(Double)evt.getOldValue();
					if (!receipt.isSelected()) diff = -diff;
					double newValue = amount.getValue()+diff;
					if (newValue<0) {
						newValue = -newValue;
						receipt.setSelected(!receipt.isSelected());
					}
					amount.setValue(newValue);
				}
			}
		});
		centerPane.add(subtransactionsPanel,c);

		return centerPane;
	}

	private void buildModes() {
		modes.setActionEnabled(false);
		modes.removeAllItems();
		Account currentAccount = data.getAccount(selectedAccount);
		boolean expense = !receipt.isSelected();
		int nb = currentAccount.getModesNumber(expense);
		for (int i = 0; i < nb; i++) {
			modes.addItem(currentAccount.getMode(i, expense).getName());
		}
		modes.setActionEnabled(true);
	}

	private String[] getAccounts() {
		String[] result = new String[data.getAccountsNumber()];
		for (int i = 0; i < data.getAccountsNumber(); i++) {
			result[i] = data.getAccount(i).getName();
		}
		return result;
	}

	public Transaction getTransaction() {
		return (Transaction) super.getResult();
	}

	private Mode displayNewModeDialog() {
		Account ac = data.getAccount(selectedAccount);
		Mode mode = NewModeDialog.open(ac, this);
		if (mode==null) return null;
		boolean expense = !receipt.isSelected();
		DateStepper vdc = expense ? mode.getExpenseVdc() : mode.getReceiptVdc();
		return (vdc != null)? mode : null;
	}
	
	private Mode getCurrentMode() {
		Account account = TransactionDialog.this.data.getAccount(selectedAccount);
		return account.getMode(selectedMode, !receipt.isSelected());
	}
	
	class AccountsListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource() == accounts) {
				int index = accounts.getSelectedIndex();
				if (index!=selectedAccount) {
					selectedAccount = index;
					if (DEBUG) System.out.println ("Account "+selectedAccount+" is selected");
					buildModes();
					modes.setSelectedItem(0);
				}
			} else {
				Account ac = NewBankAccountDialog.open(data, TransactionDialog.this, null);
				if (ac!=null) {
					accounts.addItem(ac.getName());
					accounts.setSelectedIndex(accounts.getItemCount()-1);
				}
			}
		}
	}

	class ReceiptListener implements ItemListener{
		public void itemStateChanged(ItemEvent e) {
			boolean receipt = (e.getStateChange()==ItemEvent.SELECTED);
			// We invert the receipt/expense value in order to have the mode currently selected and not the same mode
			// index with another expense/receipt value ... ro an exception
			Mode current = data.getAccount(selectedAccount).getMode(selectedMode, receipt);
			// needClearMode = the currentMode does'nt support the new expense/receipt state
			boolean needClearMode = (current!=null);
			DateStepper vdc = null;
			if (needClearMode) {
				vdc = receipt?current.getReceiptVdc():current.getExpenseVdc();
				needClearMode = (vdc==null);
			}
			buildModes();
			int modeIndex = 0;
			if (!needClearMode) {
				modeIndex = data.getAccount(selectedAccount).findMode(current, !receipt);
			}
			modes.setSelectedIndex(modeIndex);
		}
	}

	class ModesListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (e.getSource()==modes) {
				int index = modes.getSelectedIndex();
				if (index!=selectedMode) {
					selectedMode = index;
					if (DEBUG) System.out.println("Mode "+selectedMode+" is selected");
					boolean expense = !receipt.isSelected();
					Mode mode = getCurrentMode();
					//TODO transaction number may depend on the new selected mode
					transactionNumber.setText("");
					DateStepper vdc = expense?mode.getExpenseVdc():mode.getReceiptVdc();
					defDate.setDate(vdc.getNextStep(date.getDate()));
				}
			} else {
				// New mode required
				Mode m = displayNewModeDialog();
				if (m!=null) {
					modes.addItem(m.getName());
					modes.setSelectedIndex(modes.getItemCount()-1);
				}
			}
		}
    }

	@Override
	protected String getOkDisabledCause() {
		if (this.amount.getValue()==null) return "Ce bouton est inactif car le montant de l'op�ration est incorrect";
		if (this.date.getDate()==null) return "Ce bouton est inactif car la date de l'op�ration est incorrecte";
		if (this.defDate.getDate()==null) return "Ce bouton est inactif car la date de valeur de l'op�ration est incorrecte";
		return null;
	}
}
