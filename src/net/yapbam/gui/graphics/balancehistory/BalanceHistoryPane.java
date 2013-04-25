package net.yapbam.gui.graphics.balancehistory;

import javax.swing.JCheckBox;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.astesana.ajlib.utilities.NullUtils;
import net.yapbam.data.Account;
import net.yapbam.data.BalanceHistory;
import net.yapbam.data.FilteredData;
import net.yapbam.data.event.AccountAddedEvent;
import net.yapbam.data.event.AccountPropertyChangedEvent;
import net.yapbam.data.event.AccountRemovedEvent;
import net.yapbam.data.event.DataEvent;
import net.yapbam.data.event.DataListener;
import net.yapbam.data.event.EverythingChangedEvent;
import net.yapbam.data.event.TransactionsAddedEvent;
import net.yapbam.data.event.TransactionsRemovedEvent;
import net.yapbam.gui.IconManager;
import net.yapbam.gui.LocalizationData;
import net.yapbam.gui.YapbamState;
import net.yapbam.gui.IconManager.Name;
import net.yapbam.gui.actions.CompoundTransactionSelector;
import net.yapbam.gui.actions.TransactionSelector;
import net.yapbam.gui.widget.TabbedPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.print.Printable;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BalanceHistoryPane extends JPanel {
	private static final long serialVersionUID = 1L;
	static final String FIRST_ALERT = "FIRST_ALERT"; //$NON-NLS-1$
	static final String SELECTED_PANEL = "SELECTED_PANEL"; //$NON-NLS-1$
	private JCheckBox ignoreEnd;
	private BalanceHistoryGraphPane graph;
	private Date firstAlert;
	private TabbedPane tabbedPane;
	private FilteredData data;
	private BalanceHistoryTablePane tablePane;
	private int selectedPanel;
	private CompoundTransactionSelector transactionSelector;

	/**
	 * Create the panel.
	 */
	public BalanceHistoryPane(FilteredData data) {
		this.data = data;
		this.graph = new BalanceHistoryGraphPane(data);
		setLayout(new BorderLayout(0, 0));
		tabbedPane = new TabbedPane();
		final JLayeredPane layered = new JLayeredPane();
		add(layered, BorderLayout.CENTER);
		layered.add(tabbedPane);
		layered.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e){
				tabbedPane.setBounds(layered.getBounds());
			}
		});
		ignoreEnd = new JCheckBox(LocalizationData.get("BalanceHistory.ignoreEnd")); //$NON-NLS-1$
		ignoreEnd.setToolTipText(LocalizationData.get("BalanceHistory.ignoreEnd.toolTip")); //$NON-NLS-1$
		ignoreEnd.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent arg0) {
				graph.refresh(ignoreEnd.isSelected());
			}
		});
		layered.add(ignoreEnd, JLayeredPane.MODAL_LAYER);
		tabbedPane.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e){
				Rectangle bounds = tabbedPane.getBounds();
				Dimension preferredSize = ignoreEnd.getPreferredSize();
				bounds.x = bounds.width - preferredSize.width - 2;
				bounds.y = 2;
				bounds.width = preferredSize.width;
				bounds.height = preferredSize.height;
				ignoreEnd.setBounds(bounds);
			}
		});
		
		tabbedPane.addTab(LocalizationData.get("BalanceHistory.graph.title"), null, this.graph, LocalizationData.get("BalanceHistory.graph.toolTip")); //$NON-NLS-1$ //$NON-NLS-2$
		data.addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if ((event instanceof EverythingChangedEvent)) {
					ignoreEnd.setVisible(BalanceHistoryPane.this.data.getFilter().getValueDateTo()!=null);
					graph.refresh(ignoreEnd.isSelected());
				}
			}
		});
		data.getGlobalData().addListener(new DataListener() {
			@Override
			public void processEvent(DataEvent event) {
				if ((event instanceof EverythingChangedEvent)
					|| (event instanceof AccountAddedEvent) || (event instanceof AccountRemovedEvent) || (event instanceof AccountPropertyChangedEvent)
					|| (event instanceof TransactionsAddedEvent) || (event instanceof TransactionsRemovedEvent)) {
					graph.refresh(ignoreEnd.isSelected());
					testAlert();
				}
			}
		});
		testAlert();
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				changeDisplayed();
			}
		});
		tablePane = new BalanceHistoryTablePane(data);
		tabbedPane.addTab(LocalizationData.get("BalanceHistory.transaction.title"), null, tablePane, LocalizationData.get("BalanceHistory.transaction.tooltip")); //$NON-NLS-1$ //$NON-NLS-2$
		transactionSelector = new CompoundTransactionSelector();
	}
	
	void changeDisplayed () {
		int selected = tabbedPane.getId(tabbedPane.getSelectedIndex());
		if (selected==0) {
			graph.scrollToSelectedDate();
		}
		if (selected != this.selectedPanel) {
			int old = this.selectedPanel;
			this.selectedPanel = selected;
			firePropertyChange(SELECTED_PANEL, old, this.selectedPanel);
		}
		transactionSelector.setInternalSelector(selected==0?null:tablePane.table);
	}

	private void testAlert() {
		Date today = new Date();
		List<Alert> alerts = new ArrayList<Alert>();
		for (int i=0;i<data.getGlobalData().getAccountsNumber();i++) {
			Account account = data.getGlobalData().getAccount(i);
			BalanceHistory balanceHistory = account.getBalanceData().getBalanceHistory();
			long firstAlertDate = balanceHistory.getFirstAlertDate(today, null, account.getAlertThreshold());
			if (firstAlertDate>=0) {
				Date date = new Date();
				if (firstAlertDate>0) date.setTime(firstAlertDate);
				alerts.add(new Alert(date, account, balanceHistory.getBalance(date)));
			}
		}
		graph.setAlerts(alerts.toArray(new Alert[alerts.size()]));
		// Compute when occurs the first alert.
		Date first = null;
		for (int i = 0; i < alerts.size(); i++) {
			if ((first==null) || (alerts.get(i).getDate().compareTo(first)<0)) {
				first = alerts.get(i).getDate();
			}
		}
		setFirstAlert(first);
	}
	
	private void setFirstAlert (Date first) {
		if (!NullUtils.areEquals(first, firstAlert)) {
			Date old = firstAlert;
			firstAlert = first;
			int index = tabbedPane.getIndexOf(0);
			tabbedPane.setIconAt(index, first!=null?IconManager.get(Name.ALERT):null);
			String tooltip;
			tooltip = LocalizationData.get("BalanceHistory.graph.toolTip"); //$NON-NLS-1$
			if (first!=null) {
				String dateStr = DateFormat.getDateInstance(DateFormat.SHORT, LocalizationData.getLocale()).format(first);
				tooltip = tooltip.replace("'", "''"); // single quotes in message pattern are escape characters. So, we have to replace them with "double simple quote" //$NON-NLS-1$ //$NON-NLS-2$
				String pattern = "<html>"+tooltip+"<br>"+LocalizationData.get("BalanceHistory.alertTooltipAdd")+"</html>"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				tooltip = MessageFormat.format(pattern, "<b>"+dateStr+"</b>"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			tabbedPane.setToolTipTextAt(index, tooltip);
			this.firePropertyChange(FIRST_ALERT, old, firstAlert);
		}
	}

	public Date getFirstAlert() {
		return this.firstAlert;
	}

	public void saveState() {
		YapbamState.INSTANCE.saveState(tabbedPane, this.getClass().getCanonicalName());
		this.tablePane.saveState();
	}

	public void restoreState() {
		this.tablePane.restoreState();
		YapbamState.INSTANCE.restoreState(tabbedPane, this.getClass().getCanonicalName());
	}

	public Printable getPrintable() {
		return tablePane.getPrintable();
	}
	
	TransactionSelector getTransactionSelector() {
		return transactionSelector;
	}
}
