package net.yapbam.gui.administration;

import java.awt.GridBagLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;

import java.awt.GridBagConstraints;

import net.yapbam.data.GlobalData;
import net.yapbam.gui.LocalizationData;
import net.yapbam.gui.dialogs.ModeListPanel;
import javax.swing.BorderFactory;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.awt.Font;
import java.awt.Color;
import net.yapbam.gui.dialogs.ChequeBookListPanel;

public class AccountAdministrationPanel extends JPanel implements AbstractAdministrationPanel {
	private static final long serialVersionUID = 1L;
	private AccountListPanel accountListPanel = null;
	private ModeListPanel modeListPanel = null;
	private ChequeBookListPanel chequeBookListPanel = null;
	private GlobalData data;
	
	public AccountAdministrationPanel(GlobalData data) {
		super();
		this.data = data;
		initialize();
		final JTable jTable = this.getAccountListPanel().getJTable();
		jTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					int row = jTable.getSelectedRow();
					getModeListPanel().setContent(row>=0?AccountAdministrationPanel.this.data.getAccount(row):null);
				}
			}
		});
	}
	
	/**
	 * This is the default constructor
	 */
	public AccountAdministrationPanel() {
		this(new GlobalData());
	}

	/**
	 * This method initializes this
	 * 
	 * @return void
	 */
	private void initialize() {
		GridBagConstraints gridBagConstraints1 = new GridBagConstraints();
		gridBagConstraints1.gridx = 0;
		gridBagConstraints1.fill = GridBagConstraints.BOTH;
		gridBagConstraints1.weightx = 0.0D;
		gridBagConstraints1.weighty = 0.75D;
		gridBagConstraints1.gridy = 1;
		GridBagConstraints gridBagConstraints2 = new GridBagConstraints();
		gridBagConstraints2.gridx = 0;
		gridBagConstraints2.weighty = 1.0D;
		gridBagConstraints2.fill = GridBagConstraints.BOTH;
		gridBagConstraints2.gridy = 1;
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.fill = GridBagConstraints.BOTH;
		gridBagConstraints.weighty = 1.0D;
		gridBagConstraints.weightx = 1.0D;
		gridBagConstraints2.weightx = 1.0D;
		gridBagConstraints.gridy = 0;
		GridBagConstraints gridBagConstraints3 = new GridBagConstraints();
		gridBagConstraints3.gridx = 0;
		gridBagConstraints3.fill = GridBagConstraints.BOTH;
		gridBagConstraints3.weighty = 1.0D;
		gridBagConstraints3.weightx = 1.0D;
		gridBagConstraints3.gridy = 2;
		this.setSize(300, 200);
		this.setLayout(new GridBagLayout());
		this.add(getAccountListPanel(), gridBagConstraints);
		this.add(getModeListPanel(), gridBagConstraints1);
		this.add(getChequeBookListPanel(), gridBagConstraints3);
	}

	/**
	 * This method initializes accountListPanel	
	 * 	
	 * @return net.yapbam.gui.administration.AccountListPanel	
	 */
	private AccountListPanel getAccountListPanel() {
		if (accountListPanel == null) {
			accountListPanel = new AccountListPanel(data);
		}
		return accountListPanel;
	}

	/**
	 * This method initializes modeListPanel	
	 * 	
	 * @return net.yapbam.gui.dialogs.ModeListPanel	
	 */
	private ModeListPanel getModeListPanel() {
		if (modeListPanel == null) {
			modeListPanel = new AdministrationModeListPanel(data);
			modeListPanel.setBorder(BorderFactory.createTitledBorder(null, LocalizationData.get("AccountDialog.modes.border.title"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
		}
		return modeListPanel;
	}

	@Override
	public JComponent getPanel() {
		return this;
	}

	@Override
	public void restoreState() {
	}

	@Override
	public void saveState() {
	}
	
	@Override
	public String getPanelTitle() {
		return LocalizationData.get("AccountManager.title"); //$NON-NLS-1$
	}

	public String getPanelToolTip() {
		return LocalizationData.get("AccountManager.toolTip"); //$NON-NLS-1$
	}

	/**
	 * This method initializes chequeBookListPanel	
	 * 	
	 * @return net.yapbam.gui.dialogs.ChequeBookListPanel	
	 */
	private ChequeBookListPanel getChequeBookListPanel() {
		if (chequeBookListPanel == null) {
			chequeBookListPanel = new ChequeBookListPanel();
			chequeBookListPanel.setBorder(BorderFactory.createTitledBorder(null, LocalizationData.get("ChequeBookDialog.border.title"), TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, new Font("Dialog", Font.BOLD, 12), new Color(51, 51, 51)));
		}
		return chequeBookListPanel;
	}
}
